package org.xdty.callerinfo.model;

import org.xdty.callerinfo.R;

public class TextColorPair {

    public String text = "";
    public int color = R.color.blue_light;

    public TextColorPair() {
    }

    public TextColorPair(String text, int color) {
        this.text = text;
        this.color = color;
    }
}
