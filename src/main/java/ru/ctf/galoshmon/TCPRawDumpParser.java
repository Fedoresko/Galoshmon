package ru.ctf.galoshmon;

import io.pkts.Pcap;
import io.pkts.buffer.Buffer;
import io.pkts.buffer.ByteBuffer;
import io.pkts.packet.TCPPacket;
import io.pkts.protocol.Protocol;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static java.time.temporal.ChronoUnit.SECONDS;

// for -w - tcpdump option
public class TCPRawDumpParser {
    public static final int CONV_SIZE_LIMIT = 30_000;
    private final Pcap pcap;
    private final Map<String, Conversation> lastTalks = new HashMap<>();
    private final Logger log = Logger.getLogger(TCPRawDumpParser.class);
    private long convIndex = 0;
    private String localAddress = InetAddress.getLocalHost().getHostAddress();

    private DateFormat formatter = new SimpleDateFormat("HH:mm:ss");

    public TCPRawDumpParser(InputStream tcpDumpOutput) throws IOException {
        pcap = Pcap.openStream(tcpDumpOutput);
        formatter.setTimeZone(Calendar.getInstance().getTimeZone());
        localAddress = Main.config.getProperty("tcpdump.localIp", localAddress);
    }

    public Thread start() {
        Thread pcapThread = new Thread(() -> {
            try {
                pcap.loop(packet -> {
                    try {
                        for (String host : Collections.unmodifiableCollection(lastTalks.keySet())) {
                            if (SECONDS.between(LocalTime.now(), lastTalks.get(host).time) > 1) {
                                storeConversation(host);
                            }
                        }

                        packet = packet.getPacket(Protocol.TCP);

                        if (packet != null) {
                            TCPPacket p = (TCPPacket) packet;
                            Buffer payload = p.getPayload();
                            String time = formatter.format(new Date(p.getArrivalTime() / 1000));
                            String from = p.getSourceIP();
                            String to = p.getDestinationIP();

                            int length = payload == null ? 0 : payload.getReadableBytes();

                            boolean incoming = to.equals(localAddress);
                            String host = incoming ? from : to;
                            int port = incoming ? p.getDestinationPort() : p.getSourcePort();


                            if (incoming && p.isSYN()) {
                                storeConversation(from);
                                lastTalks.put(from, new Conversation(LocalTime.parse(time), host, port));
                                log.debug("Start new conv");

                            } else if (p.isFIN() || p.isRST()) {
                                storeConversation(from);

                                log.debug("End conv");
                            } else if (length > 0) {
                                Conversation conversation = lastTalks.computeIfAbsent(incoming ? from : to, ff -> new Conversation(LocalTime.parse(time), host, port));


                                ByteBuffer buffer = (ByteBuffer) payload.readBytes(length);
                                String data = new String(buffer.getArray());

                                if (!conversation.messages.isEmpty() && conversation.messages.peekLast().incoming == incoming) {
                                    Message last = conversation.messages.pollLast();
                                    data = last.data + data;
                                }

                                conversation.messages.addLast(new Message(LocalTime.parse(time), host, port, data, incoming));

                                log.debug("New port " + port + " host " + (incoming ? from : to) + " incoming: " + incoming + " data: '" + data + "'");
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error while processing packet", e);
                    }
                    return !Thread.currentThread().isInterrupted();
                });
            } catch (Exception e) {
                log.error("Raw TCP Dump Collect failed", e);
            }
        }, "Raw Dump Collector Thread");
        pcapThread.start();
        return pcapThread;
    }

    private void storeConversation(String from) {
        Conversation conversation = lastTalks.get(from);
        lastTalks.remove(from);
        if (conversation != null && !conversation.messages.isEmpty()) {
            ConversationImmutable conversationFinal = conversation.getFinal(convIndex++);
            ConcurrentNavigableMap<Long, ConversationImmutable> convs = Main.conversations.computeIfAbsent(conversation.port, p -> new ConcurrentSkipListMap<>());
            convs.put(conversationFinal.uuid, conversationFinal);
            if (CONV_SIZE_LIMIT > 0 && convs.size() > CONV_SIZE_LIMIT) {
                convs.pollFirstEntry();
            }
        }
    }

    public void stop() {
        pcap.close();
    }
}
