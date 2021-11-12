package ru.ctf.galoshmon;

import java.time.LocalTime;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class Conversation {
    public final LocalTime time;
    public final String host;
    public final int port;
    public final Deque<Message> messages = new LinkedList<>();

    Conversation(LocalTime time, String host, int port) {
        this.time = time;
        this.host = host;
        this.port = port;
    }

    ConversationImmutable getFinal(long uuid) {
        return new ConversationImmutable(time, host, (List<Message>) messages, uuid);
    }
}
