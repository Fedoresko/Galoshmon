package ru.ctf.galoshmon;

import org.apache.log4j.Logger;
import ru.ctf.galoshmon.web.Server;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.*;

public class Main {
    public static final ConcurrentMap<Integer, ConcurrentNavigableMap<Long, ConversationImmutable>> conversations = new ConcurrentHashMap<>();
    public static final Properties config = new Properties();
    private static final Logger log = Logger.getLogger(Main.class);
    private static TCPRawDumpParser parser;
    private static Thread dumpThread;
    private static Process tcpDumpProcess;

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        try (InputStream stream = new FileInputStream("conf.properties")) {
            config.load(stream);
        }

        log.info("Starting web server...");
        Server server = new Server();
        server.start();

        try {
            if (config.getProperty("tcpdump.onstart").equals("true")) {
                String cmd = "tcpdump -w - -nli " + config.getProperty("tcpdump.interface", "1") + " " + config.getProperty("tcpdump.filter");
                startDumpProcessing(cmd);
            }

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String command = console.readLine();
                if ("stop".equals(command)) {
                    break;
                }
                if (command.startsWith("tcpdump")) {
                    stopDumpProcessing();
                    startDumpProcessing("tcpdump -w - -nli " + command.substring(7).trim());
                }
            }

            stopDumpProcessing();
        } finally {
            server.stop();
            log.info("Web server stopped.");
        }
    }

    private static void startDumpProcessing(String cmd) throws IOException {
        log.info("Starting tcpdump (" + cmd + ")...");
        tcpDumpProcess = Runtime.getRuntime().exec(cmd);
        InputStream tcpDumpOutput = tcpDumpProcess.getInputStream();
        log.info("Starting collector stream...");
        parser = new TCPRawDumpParser(tcpDumpOutput);
        dumpThread = parser.start();
    }

    private static void stopDumpProcessing() throws InterruptedException, ExecutionException {
        log.info("Stopping...");
        if (parser != null) parser.stop();
        if (dumpThread != null) dumpThread.interrupt();
        if (tcpDumpProcess != null) {
            CompletableFuture<Process> completion = tcpDumpProcess.onExit();
            tcpDumpProcess.destroy();
            completion.get();
        }
        log.info("tcpdump stopped.");
        if (dumpThread != null) dumpThread.join();
        log.info("Collector joined.");
    }
}
