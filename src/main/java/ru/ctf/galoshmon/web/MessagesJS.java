package ru.ctf.galoshmon.web;

import ru.ctf.galoshmon.ConversationImmutable;
import ru.ctf.galoshmon.web.filters.Filter;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;

class MessagesJS implements Serializable {
    MessageJS[] messages;

    MessagesJS(ConversationImmutable conv, List<Filter> filters) {
        messages = conv.messages.stream().map(m -> {
            String text = Util.escapeHTML(m.data);
            for (Filter filter : filters) {
                int pos = 0;
                StringBuilder res = new StringBuilder();
                Matcher matcher = filter.pattern.matcher(text);
                while (matcher.find()) {
                    res.append(text, pos, matcher.start());
                    res.append("<span style=\"background-color: hsl("+filter.hue+", 75%, 25%); color: white;\">");
                    res.append(text, matcher.start(), matcher.end());
                    res.append("</span>");
                    pos = matcher.end();
                }
                res.append(text.substring(pos));
                text = res.toString();
            }
            text = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
            return new MessageJS(m.incoming, text);
        }).toArray(MessageJS[]::new);
    }
}
