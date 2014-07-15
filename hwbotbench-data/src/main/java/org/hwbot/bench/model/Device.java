package org.hwbot.bench.model;

import javax.xml.bind.annotation.XmlElement;

public class Device {
    String deviceName;
    String socName;
    String deviceVendor;

    @XmlElement
    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String name) {
        this.deviceName = name;
    }

    @XmlElement
    public String getSocName() {
        return socName;
    }

    public void setSocName(String socName) {
        this.socName = socName;
    }

    @XmlElement
    public String getDeviceVendor() {
        return deviceVendor;
    }

    public void setDeviceVendor(String deviceVendor) {
        this.deviceVendor = deviceVendor;
    }

}