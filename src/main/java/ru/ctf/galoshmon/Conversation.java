package ru.ctf.galoshmon;

import java.time.LocalTime;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class Conversation {
    public LocalTime time;
    public String host;
    public int port;
    public final LinkedList<Message> messages = new LinkedList<>();
    public int ttl;

    Conversation(LocalTime time, String host, int port, int ttl) {
        this.time = time;
        this.host = host;
        this.port = port;
        this.ttl = ttl;
    }

    ConversationImmutable getFinal(long uuid) {
        return new ConversationImmutable(time, host, messages, uuid, ttl);
    }
}
