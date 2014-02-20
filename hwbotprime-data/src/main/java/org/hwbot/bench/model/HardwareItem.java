package org.hwbot.bench.model;

import javax.xml.bind.annotation.XmlElement;

public class HardwareItem {

    protected String name;
    protected String vendor;
    protected Integer amount;
    protected Float idleTemp;
    protected Float loadTemp;
    protected String cooling;

    @XmlElement
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement
    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    @XmlElement
    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    @XmlElement
    public Float getIdleTemp() {
        return idleTemp;
    }

    public void setIdleTemp(Float idleTemp) {
        this.idleTemp = idleTemp;
    }

    @XmlElement
    public Float getLoadTemp() {
        return loadTemp;
    }

    public void setLoadTemp(Float loadTemp) {
        this.loadTemp = loadTemp;
    }

    @XmlElement
    public String getCooling() {
        return cooling;
    }

    public void setCooling(String cooling) {
        this.cooling = cooling;
    }

}
