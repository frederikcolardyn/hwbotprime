package org.hwbot.bench.model;

import javax.xml.bind.annotation.XmlElement;

public class Memory extends HardwareItem {
    int totalSize;

    @XmlElement
    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public int getTotalSizeMB() {
        return totalSize / 1024 / 1024;
    }

}