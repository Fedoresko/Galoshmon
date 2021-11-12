package ru.ctf.galoshmon.web;

import ru.ctf.galoshmon.ConversationImmutable;
import ru.ctf.galoshmon.Message;
import ru.ctf.galoshmon.web.filters.Filter;
import ru.ctf.galoshmon.web.filters.FilterMarkJS;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

class ConversationsJS implements Serializable {
    final String last;
    final ConversationJS[] convs;

    ConversationsJS(List<ConversationImmutable> conversations, List<Filter> filters) {


        convs = conversations.stream().map(c -> {
            int incoming = 0;
            int outgoing = 0;
            List<FilterMarkJS>  marks = new ArrayList<>();
            for (Message message : c.messages) {
                if (message.incoming) {
                    incoming++;
                } else {
                    outgoing++;
                }
            }
            for (Filter filter : filters) {
                int sum = 0;
                for (Message message : c.messages) {
                    sum += filter.pattern.matcher(message.data).results().count();
                }
                if (sum > 0) {
                    marks.add(new FilterMarkJS(sum, filter.hue));
                }
            }

            return new ConversationJS(c.time.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                  c.host, Long.toString(c.uuid), incoming, outgoing, marks.toArray(FilterMarkJS[]::new));
        } ).toArray(ConversationJS[]::new);
        last = convs[convs.length - 1].uuid;
    }
}
