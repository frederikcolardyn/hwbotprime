package org.hwbot.prime.model;

import java.util.ArrayList;
import java.util.List;

import org.hwbot.api.generic.dto.SubmissionDTO;

public class SubmissionRanking {

    List<SubmissionDTO> list = new ArrayList<SubmissionDTO>();
    String applicationName;
    int applicationId;

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public List<SubmissionDTO> getList() {
        return list;
    }

    public void setList(List<SubmissionDTO> submissions) {
        this.list = submissions;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public int getApplicationId() {
        return applicationId;
    }

    @Override
    public String toString() {
        return "SubmissionRanking [" + (list != null ? "submissions=" + list + ", " : "")
                + (applicationName != null ? "applicationName=" + applicationName + ", " : "") + "applicationId=" + applicationId + "]";
    }

}