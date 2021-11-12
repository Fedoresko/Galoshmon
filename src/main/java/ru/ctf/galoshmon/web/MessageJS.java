package ru.ctf.galoshmon.web;

import java.io.Serializable;

class MessageJS implements Serializable {
    final boolean incoming;
    final String data;

    MessageJS(boolean incoming, String data) {
        this.incoming = incoming;
        this.data = data;
    }
}
