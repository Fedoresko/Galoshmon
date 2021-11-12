package ru.ctf.galoshmon.web;

import one.nio.http.*;
import one.nio.serial.Json;
import one.nio.server.AcceptorConfig;
import ru.ctf.galoshmon.ConversationImmutable;
import ru.ctf.galoshmon.Main;
import ru.ctf.galoshmon.web.filters.Filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class Server extends HttpServer {

    public static final int POLLING_TIMEOUT = 2000;
    public static final String SESSION_ID_COOKIE = "SessionID";
    public static final String FILTERS_KEY = "filters";
    private SecureRandom secureRandom = new SecureRandom();

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    public Server() throws IOException {
        super(getConfig());
        secureRandom.setSeed(System.nanoTime());
    }

    private static HttpServerConfig getConfig() {
        HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[1];
        AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = Integer.parseInt(Main.config.getProperty("web.port", "80"));
        acceptorConfig.keepAlive = true;
        acceptorConfig.reusePort = true;
        config.acceptors[0] = acceptorConfig;
        return config;
    }

    @Override
    public void handleRequest(Request request, HttpSession session) throws IOException {
        if ("/auth".equals(request.getPath())) {
            super.handleRequest(request, session);
            return;
        }

        String cookies = request.getHeader("Cookie:");
        if (cookies != null) {
            for (String cookie : cookies.split(";")) {
                String name = cookie.trim().split("=")[0];
                String val = cookie.trim().split("=")[1];
                if (SESSION_ID_COOKIE.equals(name)) {
                    Session sessionLocal = sessions.get(val);
                    if (sessionLocal != null) {
                        if (sessionLocal.isExpired()) {
                            sessions.remove(val);
                        } else {
                            super.handleRequest(new SessionRequest(request, sessionLocal), session);
                            return;
                        }
                    }
                }
            }
        }

        session.sendResponse(Response.redirect("/auth"));
    }

    @Path("/")
    @RequestMethod(Request.METHOD_GET)
    public Response serveRoot(Request request) throws IOException {
        return getResponse(getClass().getResourceAsStream("/index.html").readAllBytes());
    }

    @Path("/logo")
    @RequestMethod(Request.METHOD_GET)
    public Response getLogo(Request request) throws IOException {
        return getResponse(getClass().getResourceAsStream("/logo.png").readAllBytes(), "image/png");
    }

    @Path("/auth")
    @RequestMethod(Request.METHOD_GET)
    public Response getAuth(Request request) throws IOException {
        return getResponse(getClass().getResourceAsStream("/auth.html").readAllBytes());
    }

    @Path("/auth")
    @RequestMethod(Request.METHOD_POST)
    public Response postAuth(Request request) throws UnsupportedEncodingException {
        Map<String, String> params = getFormEncodedParams(request);

        String name = params.get("name");
        String password = params.get("password");

        if (Main.config.getProperty("user", "a").equals(name) && Main.config.getProperty("password", "b").equals(password)) {
            Response response = Response.redirect("/");
            Session session = new Session();
            byte[] bb = new byte[256];
            secureRandom.nextBytes(bb);
            String id = new String(Base64.getEncoder().encode(bb)).replace("=", "");
            sessions.put(id, session);
            List<Filter> filters = new CopyOnWriteArrayList<>();
            filters.add(new Filter("GET", "^GET",100));
            filters.add(new Filter("POST", "^POST", 230));
            session.put(FILTERS_KEY, filters);
            response.addHeader("Set-Cookie: " + SESSION_ID_COOKIE + "=" + id);
            return response;
        }

        return Response.redirect("/auth");
    }

    private Map<String, String> getFormEncodedParams(Request request) throws UnsupportedEncodingException {
        String body = new String(request.getBody());
        Map<String, String> params = new HashMap<>();
        for (String p : body.split("&")) {
            String[] fields = p.trim().split("=");
            if (fields.length == 2) {
                params.put(fields[0], java.net.URLDecoder.decode(fields[1],StandardCharsets.UTF_8.name()));
            }
        }
        return params;
    }

    @Path("/ports")
    @RequestMethod(Request.METHOD_GET)
    public Response getPorts(Request request) throws IOException {
        String json = Json.toJson(Main.conversations.keySet());
        return getResponseJson(json);
    }

    @Path("/filters")
    @RequestMethod(Request.METHOD_GET)
    public Response getFilters(Request request) throws IOException {
        List<Filter> filters = (List<Filter>)((SessionRequest)request).getSession().get(FILTERS_KEY);
        String json = Json.toJson(filters);
        return getResponseJson(json);
    }

    @Path("/addfilter")
    @RequestMethod(Request.METHOD_POST)
    public Response newFilter(Request request) throws UnsupportedEncodingException {
        List<Filter> filters = (List<Filter>)((SessionRequest)request).getSession().get(FILTERS_KEY);
        Map<String, String> params = getFormEncodedParams(request);
        String name = params.get("name");
        String regexp = params.get("regexp");
        if (name != null && regexp != null) {
            filters.add(new Filter(name, regexp, secureRandom.nextInt(360)));
            return new Response(Response.CREATED);
        }
        return new Response(Response.BAD_REQUEST);
    }

    @Path("/removefilter")
    @RequestMethod(Request.METHOD_DELETE)
    public Response removeFilter(Request request) throws UnsupportedEncodingException {
        List<Filter> filters = (List<Filter>)((SessionRequest)request).getSession().get(FILTERS_KEY);
        Map<String, String> params = getFormEncodedParams(request);
        String name = params.get("name");
        if (name != null) {
            filters.removeIf(f -> f.name.equals(name));
            return new Response(Response.OK);
        }
        return new Response(Response.BAD_REQUEST);
    }

    @Path("/conversations")
    @RequestMethod(Request.METHOD_GET)
    public Response getConvs(Request request) throws IOException {
        String portStr = request.getParameter("port");
        if (portStr != null) {
            int port = Integer.parseInt(portStr.substring(1));
            String start = request.getParameter("start");

            ConcurrentNavigableMap<Long, ConversationImmutable> convs = Main.conversations.get(port);

            if (convs != null) {
                List<Filter> filters = (List<Filter>)((SessionRequest)request).getSession().get(FILTERS_KEY);
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < POLLING_TIMEOUT) {
                    if (start != null) {
                        convs = convs.tailMap(Long.parseLong(start.substring(1)), false);
                    }
                    if (convs.size() > 0) {
                        List<ConversationImmutable> filtered = convs.values().stream().limit(200).collect(Collectors.toList());
                        return getResponseJson(Json.toJson(new ConversationsJS(filtered, filters)));
                    }
                }
            }
        }

        return new Response(Response.NOT_FOUND);
    }

    @Path("/conversation")
    @RequestMethod(Request.METHOD_GET)
    public Response getConv(Request request) throws IOException {
        String portStr = request.getParameter("port");
        String uuid = request.getParameter("uuid");
        if (uuid != null && portStr != null) {
            int port = Integer.parseInt(portStr.substring(1));
            ConversationImmutable conversation = Main.conversations.get(port).get(Long.parseLong(uuid.substring(1)));
            if (conversation != null) {
                List<Filter> filters = (List<Filter>)((SessionRequest)request).getSession().get(FILTERS_KEY);
                return getResponseJson(Json.toJson(new MessagesJS(conversation, filters)));
            }
        }
        return new Response(Response.NOT_FOUND);
    }

    private Response getResponse(byte[] h) {
        return getResponse(h, "text/html; charset=UTF-8");
    }

    private Response getResponse(byte[] h, String mime) {
        Response response = new Response(Response.OK);
        response.setBody(h);
        response.addHeader("Date: " + LocalDateTime.now());
        response.addHeader("Content-Type: "+mime);
        response.addHeader("Content-Length: " + h.length);
        response.addHeader("Connection: close");
        return response;
    }

    private Response getResponseJson(String h) {
        Response response = new Response(Response.OK);
        response.setBody(h.getBytes(StandardCharsets.UTF_8));
        response.addHeader("Date: " + LocalDateTime.now());
        response.addHeader("Content-Type: application/json; charset=UTF-8");
        response.addHeader("Content-Length: " + h.length());
        response.addHeader("Connection: close");
        return response;
    }

}