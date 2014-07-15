package org.hwbot.prime.model;

import java.io.Serializable;

/**
 * A benchmark result which holds all the data to be submitted to HWBOT. Used for temporary storage on local device.
 * 
 * @author frederik
 * 
 */

public class BenchmarkResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private byte[] encryptedXml;
    private float score;
    private long date;
    private Integer maxCpuFrequency;
    private boolean submitted;

    @Override
    public String toString() {
        return "BenchmarkResult [score=" + score + ", date=" + date + ", " + (maxCpuFrequency != null ? "maxCpuFrequency=" + maxCpuFrequency + ", " : "")
                + "submitted=" + submitted + "]";
    }

    public byte[] getEncryptedXml() {
        return encryptedXml;
    }

    public void setEncryptedXml(byte[] encryptedXml) {
        this.encryptedXml = encryptedXml;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public Integer getMaxCpuFrequency() {
        return maxCpuFrequency;
    }

    public void setMaxCpuFrequency(Integer maxCpuFrequency) {
        this.maxCpuFrequency = maxCpuFrequency;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

}
