package org.hwbot.bench.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "submission")
@XmlType(propOrder = { "application", "score", "screenshot", "applicationChecksum", "hardware", "javaMetaData" })
public class Request {

    private Application application;
    private Score score;
    private String screenshot;
    private Hardware hardware;
    private String applicationChecksum;
    private MetaData javaMetaData;

    public Request() {
    }

    public Request(String client, String version, String processorModel, Float processorSpeed, Integer memoryInMB, String points) {
        super();
        application = new Application();
        application.setName(client);
        application.setVersion(version);

        score = new Score();
        score.setPoints(points);

        hardware = new Hardware();
        Processor processor = new Processor();
        processor.setCoreClock(processorSpeed);
        processor.setName(processorModel);
        if (memoryInMB != null) {
            Memory memory = new Memory();
            memory.setTotalSize(memoryInMB.intValue());
            hardware.setMemory(memory);
        }
        hardware.setProcessor(processor);
        // hwbot production bug, disabled
        // javaMetaData = new MetaData();
    }

    @XmlElement
    public String getApplicationChecksum() {
        return applicationChecksum;
    }

    @XmlElement(name = "metadata")
    public MetaData getJavaMetaData() {
        return javaMetaData;
    }

    public void setJavaMetaData(MetaData javaMetaData) {
        this.javaMetaData = javaMetaData;
    }

    public void setApplicationChecksum(String applicationChecksum) {
        this.applicationChecksum = applicationChecksum;
    }

    @XmlElement
    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    @XmlElement
    public Score getScore() {
        return score;
    }

    public void setScore(Score score) {
        this.score = score;
    }

    @XmlElement
    public String getScreenshot() {
        return screenshot;
    }

    public void setScreenshot(String screenshot) {
        this.screenshot = screenshot;
    }

    @XmlElement
    public Hardware getHardware() {
        return hardware;
    }

    public void setHardware(Hardware hardware) {
        this.hardware = hardware;
    }

    public static class Application {
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

    public static class MetaData {
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

    public static class Score {
        String points;

        @XmlElement
        public String getPoints() {
            return points;
        }

        public void setPoints(String points) {
            this.points = points;
        }
    }

    public static class Screenshot {
        String screenshot;

        @XmlElement
        public String getScreenshot() {
            return screenshot;
        }

        public void setScreenshot(String screenshot) {
            this.screenshot = screenshot;
        }

    }

    @XmlType(propOrder = { "processor", "memory" })
    public static class Hardware {

        Processor processor;

        Memory memory;

        @XmlElement
        public Processor getProcessor() {
            return processor;
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

    }

    public static class Processor {
        String name;
        Float coreClock;

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

    }

    public static class Memory {
        int totalSize;

        @XmlElement
        public int getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(int totalSize) {
            this.totalSize = totalSize;
        }

    }

    public void addScreenshot(String base64) {
        screenshot = base64;
    }

}
