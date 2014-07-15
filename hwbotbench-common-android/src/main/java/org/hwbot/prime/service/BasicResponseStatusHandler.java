package org.hwbot.prime.service;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.util.EntityUtils;

public class BasicResponseStatusHandler extends BasicResponseHandler {

    private int status;

    @Override
    public String handleResponse(HttpResponse response) throws HttpResponseException, IOException {

        StatusLine statusLine = response.getStatusLine();
        status = statusLine.getStatusCode();
        HttpEntity entity = response.getEntity();
        return entity == null ? null : EntityUtils.toString(entity);
    }

    public int getStatus() {
        return status;
    }
    

}
