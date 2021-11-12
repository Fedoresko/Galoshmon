package ru.ctf.galoshmon.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class Session {
    private final long expires;
    private final Map<String, Object> data;

    Session() {
        expires = System.currentTimeMillis() + 600_000;
        data = new ConcurrentHashMap<>();
    }

    boolean isExpired() {
        return System.currentTimeMillis() > expires;
    }

    public Object get(String key) {
        return data.get(key);
    }

    public void put(String key, Object val) {
        data.put(key, val);
    }
}
