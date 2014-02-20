package org.hwbot.bench.model;

import javax.xml.bind.annotation.XmlElement;

public class Application {
    String name;
    String version;

    @XmlElement
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}