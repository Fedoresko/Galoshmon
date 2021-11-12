package ru.ctf.galoshmon.web;

import one.nio.http.Request;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Util {
    private Util() {}

    public static String escapeHTML(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') {
                out.append("<br>");
            } else if (c > 127 || c == '"' || c == '\'' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    static Map<String, String> getFormEncodedParams(Request request) throws UnsupportedEncodingException {
        String body = new String(request.getBody());
        Map<String, String> params = new HashMap<>();
        for (String p : body.split("&")) {
            String[] fields = p.trim().split("=");
            if (fields.length == 2) {
                params.put(fields[0], java.net.URLDecoder.decode(fields[1], StandardCharsets.UTF_8.name()));
            }
        }
        return params;
    }
}
