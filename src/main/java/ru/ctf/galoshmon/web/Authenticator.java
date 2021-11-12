package ru.ctf.galoshmon.web;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import ru.ctf.galoshmon.Main;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class Authenticator {
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private SecureRandom secureRandom = new SecureRandom();

    public Authenticator() {
        secureRandom.setSeed(System.nanoTime());
    }

    public Session check(Request request, HttpSession session) throws IOException {
        if ("/auth".equals(request.getPath())) {
            return new Session();
        }

        String cookies = request.getHeader("Cookie:");
        if (cookies != null) {
            for (String cookie : cookies.split(";")) {
                String name = cookie.trim().split("=")[0];
                String val = cookie.trim().split("=")[1];
                if (Server.SESSION_ID_COOKIE.equals(name)) {
                    Session sessionLocal = sessions.get(val);
                    if (sessionLocal != null) {
                        if (sessionLocal.isExpired()) {
                            sessions.remove(val);
                        } else {
                            return sessionLocal;
                        }
                    }
                }
            }
        }

        if (request.getPath().equals("/ports") || request.getPath().equals("/conversations")) {
            Response res = new Response(Response.UNAUTHORIZED);
            res.addHeader("Auth-Request: true");
            res.addHeader("Location: /auth");
            session.sendResponse(res);
            session.scheduleClose();
        } else {
            session.sendResponse(Response.redirect("/auth"));
        }

        return null;
    }

    public Response authenticate(Request request) throws UnsupportedEncodingException {
        Map<String, String> params = Util.getFormEncodedParams(request);

        String name = params.get("name");
        String password = params.get("password");

        if (Main.config.getProperty("user", "a").equals(name) && Main.config.getProperty("password", "b").equals(password)) {
            Response response = Response.redirect("/");
            byte[] bb = new byte[256];
            secureRandom.nextBytes(bb);
            String id = new String(Base64.getEncoder().encode(bb)).replace("=", "");
            sessions.put(id, Session.getDefault());
            response.addHeader("Set-Cookie: " + Server.SESSION_ID_COOKIE + "=" + id);
            return response;
        }

        return Response.redirect("/auth");
    }
}
