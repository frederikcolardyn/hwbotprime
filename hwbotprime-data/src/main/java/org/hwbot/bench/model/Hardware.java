package org.hwbot.bench.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


@XmlType(propOrder = { "device", "processor", "memory" })
public class Hardware {

    private Device device;

    private Processor processor;

    private Memory memory;

    @XmlElement
    public Processor getProcessor() {
        return processor;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    @XmlElement
    public Memory getMemory() {
        return memory;
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    @XmlElement
    public Device getDevice() {
        return device;
    }

}