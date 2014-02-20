package org.hwbot.bench.model;

import javax.xml.bind.annotation.XmlAttribute;

public class MetaData {
    private String info = System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version");
    private String name = "java";

    @XmlAttribute(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

}