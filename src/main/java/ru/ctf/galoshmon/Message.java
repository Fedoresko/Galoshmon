package ru.ctf.galoshmon;

import java.time.LocalTime;

public class Message {
    public LocalTime time;
    public String host;
    public int port;
    public String data;
    public boolean incoming;

    Message(LocalTime time, String host, int port, String data, boolean incoming) {
        this.time = time;
        this.host = host;
        this.port = port;
        this.data = data;
        this.incoming = incoming;
    }
}
