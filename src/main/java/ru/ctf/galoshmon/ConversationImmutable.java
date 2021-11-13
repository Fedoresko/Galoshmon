package ru.ctf.galoshmon;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ConversationImmutable implements Iterable<Message> {
    public final List<Message> messages;
    public final LocalTime time;
    public final String host;
    public final long uuid;
    public final int ttl;

    ConversationImmutable(LocalTime time, String host, List<Message> messages, long uuid, int ttl) {
        this.messages = Collections.unmodifiableList(messages);
        this.time = time;
        this.host = host;
        this.uuid = uuid;
        this.ttl = ttl;
    }

    public Iterator<Message> iterator() {
        return messages.iterator();
    }
}
