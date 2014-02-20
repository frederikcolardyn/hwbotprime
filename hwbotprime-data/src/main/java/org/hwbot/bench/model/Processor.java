package org.hwbot.bench.model;

import javax.xml.bind.annotation.XmlElement;

public class Processor extends HardwareItem {
    String name;
    Float coreClock;
    private int effectiveCores;

    @XmlElement
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement
    public Float getCoreClock() {
        return coreClock;
    }

    public void setCoreClock(Float coreClock) {
        this.coreClock = coreClock;
    }

    @XmlElement
    public int getEffectiveCores() {
        return effectiveCores;
    }

    public void setEffectiveCores(int cores) {
        this.effectiveCores = cores;
    }

}