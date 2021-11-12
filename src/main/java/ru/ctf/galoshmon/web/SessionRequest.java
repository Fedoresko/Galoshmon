package ru.ctf.galoshmon.web;

import one.nio.http.Request;

public class SessionRequest extends Request {
    private Session session;

    public SessionRequest(Request prototype, Session session) {
        super(prototype);
        this.session = session;
    }

    public Session getSession() {
        return session;
    }
}
