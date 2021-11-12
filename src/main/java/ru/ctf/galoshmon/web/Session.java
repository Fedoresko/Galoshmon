package ru.ctf.galoshmon.web;

import ru.ctf.galoshmon.web.filters.Filter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class Session {
    private final long expires;
    private final Map<String, Object> data;

    Session() {
        expires = System.currentTimeMillis() + 600_000;
        data = new ConcurrentHashMap<>();
    }

    public static Session getDefault() {
        Session session = new Session();
        List<Filter> filters = new CopyOnWriteArrayList<>();
        filters.add(new Filter("GET", "^GET", 100));
        filters.add(new Filter("POST", "^POST", 230));
        session.put(Server.FILTERS_KEY, filters);
        return session;
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
