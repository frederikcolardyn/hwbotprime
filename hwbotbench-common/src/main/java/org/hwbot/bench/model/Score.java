package org.hwbot.bench.model;

import javax.xml.bind.annotation.XmlElement;

public class Score {
    String points;

    @XmlElement
    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }
}