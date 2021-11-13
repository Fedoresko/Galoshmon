package ru.ctf.galoshmon;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.time.LocalTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

// For -XX tcpdump option
public class TCPDumpCollectorThread extends Thread {
    private final InputStream tcpDumpOutput;
    private final Map<String, Conversation> lastTalks = new HashMap<>();
    private final Logger log = Logger.getLogger(TCPDumpCollectorThread.class);
    private long convIndex = 0;

    public TCPDumpCollectorThread(InputStream tcpDumpOutput) {
        super("Collector Thread");
        this.tcpDumpOutput = tcpDumpOutput;
    }

    @Override
    public void run() {
        try {
            String localAddress = detectIpAddrLocal();

            BufferedReader reader = new BufferedReader(new InputStreamReader(tcpDumpOutput));
            String line = null;

            while (!Thread.currentThread().isInterrupted()) {
                while (line == null && !Thread.currentThread().isInterrupted()) {
                    if (reader.ready()) {
                        line = reader.readLine();
                    }
                }
                if (line == null) {
                    break;
                }

                String packet = line;
                log.info(packet);

                StringBuilder payload = new StringBuilder();
                boolean isPacketFinished = false;
                while (!isPacketFinished) {
                    line = null;
                    while (!reader.ready() && !Thread.currentThread().isInterrupted()) {
                        Thread.yield(); //spin
                    }
                    if (reader.ready()) {
                        line = reader.readLine();
                    }
                    isPacketFinished = (line == null) || line.matches("^\\d\\d:\\d\\d:\\d\\d\\.\\d+.*");
                    if (!isPacketFinished) {
                        payload.append(line);
                        payload.append("\n");
                    }
                }

                int delPos = packet.indexOf(": ", 8);
                if (delPos == -1) {
                    log.warn("Not format");
                    line = null;
                    continue;
                }
                String[] header = packet.substring(0, delPos).split(" ");
                String time = header[0];
                String from = header[2];
                String to = header[4];

                String[] info = packet.substring(delPos + 1).trim().split(", ");

                int length = 0;
                String flags = "";
                for (String field : info) {
                    if (field.startsWith("Flags")) {
                        flags = info[0].substring(7, info[0].length() - 1);
                    } else if (field.startsWith("length")) {
                        int pos = field.indexOf(':');
                        if (pos != -1) {
                            length = Integer.parseInt(field.substring(7, pos));
                            payload.insert(0, field.substring(pos + 1)+"\n");
                        }

                    }
                }

                boolean incoming;
                String host;
                int port;
                if (from.startsWith(localAddress)) {
                    incoming = false;
                    port = Integer.parseInt(from.substring(from.lastIndexOf('.') + 1));
                    host = to.substring(0, to.lastIndexOf('.'));
                } else {
                    incoming = true;
                    port = Integer.parseInt(to.substring(to.lastIndexOf('.') + 1));
                    host = from.substring(0, from.lastIndexOf('.'));
                }

                if (info[0].startsWith("UDP")) {
                    log.info("UDP");
                    //TODO
                } else if (incoming && flags.contains("S")) {
                    storeConversation(from, port);
                    lastTalks.put(from, new Conversation(LocalTime.parse(time), host, port, 0));
                    log.info("Start new conv");

                } else if (incoming && flags.contains("F")) {
                    storeConversation(from, port);

                    log.info("End conv");
                } else if (length > 0) {
                    Conversation conversation = lastTalks.computeIfAbsent(incoming ? from : to, (ff) -> new Conversation(LocalTime.parse(time), host, port, 0));
                    conversation.messages.addLast(new Message(LocalTime.parse(time), host, port, new String(payload), incoming));

                    log.info("New port " + port + " host " + (incoming ? from : to) + " incoming: " + incoming + " data: '" + new String(payload) + "'");
                }
            }

            for (Map.Entry<String, Conversation> entry : lastTalks.entrySet()) {
                ConversationImmutable conversation = entry.getValue().getFinal(convIndex++);
                Main.conversations.computeIfAbsent(entry.getValue().messages.peekFirst().port, (tt) -> new ConcurrentSkipListMap<>()).put(conversation.uuid, conversation);
            }
        } catch (Exception e) {
            log.error("TCP collect failed", e);
        }
    }

    private String detectIpAddrLocal() throws UnknownHostException, SocketException {
        int index = Integer.parseInt(Main.config.getProperty("tcpdump.interface", "1"));
        NetworkInterface networkInterface = NetworkInterface.getByIndex(index);
        Enumeration<InetAddress> inetAddress = networkInterface.getInetAddresses();
        while(inetAddress.hasMoreElements())
        {
            InetAddress currentAddress = inetAddress.nextElement();
            if(currentAddress instanceof Inet4Address && !currentAddress.isLoopbackAddress())
            {
                return currentAddress.getHostAddress();
            }
        }
        return InetAddress.getLocalHost().getHostAddress();
    }

    private void storeConversation(String from, int port) {
        Conversation conversation = lastTalks.get(from);
        lastTalks.remove(from);
        if (conversation != null) {
            ConversationImmutable conversationFinal = conversation.getFinal(convIndex++);
            Main.conversations.computeIfAbsent(port, p -> new ConcurrentSkipListMap<>()).put(conversationFinal.uuid, conversationFinal);
        }
    }
}
