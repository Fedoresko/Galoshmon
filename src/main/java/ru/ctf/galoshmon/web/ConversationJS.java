package ru.ctf.galoshmon.web;

import ru.ctf.galoshmon.web.filters.FilterMarkJS;

import java.io.Serializable;

class ConversationJS implements Serializable {
    final String time;
    final String host;
    final String uuid;
    final int incoming;
    final int outgoing;
    final FilterMarkJS[] marks;
    final int ttl;

    ConversationJS(String time, String host, String uuid, int incoming, int outgoing, FilterMarkJS[] marks, int ttl) {
        this.time = time;
        this.host = host;
        this.uuid = uuid;
        this.incoming = incoming;
        this.outgoing = outgoing;
        this.marks = marks;
        this.ttl = ttl;
    }
}
