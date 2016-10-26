package org.hwbot.bench.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "submission")
@XmlType(propOrder = { "application", "score", "screenshot", "applicationChecksum", "hardware", "javaMetaData","applicationId"})
public class Request {

    private Application application;
    private Score score;
    private String screenshot;
    private Hardware hardware;
    private String applicationChecksum;
    private MetaData javaMetaData;


    public Request() {
    }

    public Request(String client, String version, String scorePoints, Hardware hardware) {
        super();
        this.application = new Application();
        this.application.setName(client);
        this.application.setVersion(version);



        this.score = new Score();
        this.score.setPoints(scorePoints);

        this.hardware = hardware;
        this.javaMetaData = new MetaData();
    }

    public Integer getApplicationId() {
        return this.application.getId();
    }

    public void setApplicationId(Integer applicationId) {
        this.application.setId(applicationId);
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

    public void addScreenshot(String base64) {
        screenshot = base64;
    }

}
