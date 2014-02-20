package org.hwbot.bench.model;

import javax.xml.bind.annotation.XmlElement;

public class Screenshot {
    String screenshot;

    @XmlElement
    public String getScreenshot() {
        return screenshot;
    }

    public void setScreenshot(String screenshot) {
        this.screenshot = screenshot;
    }

}