package org.hwbot.prime.model;

import java.util.ArrayList;
import java.util.List;

public class SubmissionRanking {

    List<Result> submissions = new ArrayList<Result>();
    String applicationName;
    int applicationId;

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public List<Result> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(List<Result> submissions) {
        this.submissions = submissions;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public int getApplicationId() {
        return applicationId;
    }

    @Override
    public String toString() {
        return "SubmissionRanking [" + (submissions != null ? "submissions=" + submissions + ", " : "")
                + (applicationName != null ? "applicationName=" + applicationName + ", " : "") + "applicationId=" + applicationId + "]";
    }

}