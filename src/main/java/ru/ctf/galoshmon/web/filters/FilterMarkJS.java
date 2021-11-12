package ru.ctf.galoshmon.web.filters;

import java.io.Serializable;

public class FilterMarkJS implements Serializable {
    public final int nHits;
    public final int hue;

    public FilterMarkJS(int nHits, int hue) {
        this.nHits = nHits;
        this.hue = hue;
    }
}
