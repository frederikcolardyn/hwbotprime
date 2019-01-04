package org.hwbot.bench.model;

import javax.xml.bind.annotation.XmlElement;

public class Memory extends HardwareItem {
    private int totalSize;

    @XmlElement
    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

}