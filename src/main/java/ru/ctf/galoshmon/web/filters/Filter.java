package ru.ctf.galoshmon.web.filters;

import java.io.Serializable;
import java.util.regex.Pattern;

public class Filter implements Serializable {
    public Filter(String name, String regexp, int hue) {
        this.name = name;
        this.regexp = regexp;
        this.hue = hue;
        this.pattern = Pattern.compile(regexp);
    }
    public final String name;
    public final String regexp;
    public final int hue;
    public boolean isActive;
    public final transient Pattern pattern;
}
