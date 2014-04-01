package org.hwbot.prime.tasks;

import java.net.UnknownHostException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.hwbot.api.generic.dto.GenericApiResponse;
import org.hwbot.prime.api.VoteObserver;
import org.hwbot.prime.service.BasicResponseStatusHandler;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextSwitcher;

import com.google.gson.Gson;

public class SubmitVoteTask extends AsyncTask<Void, Void, Void> {

    private String targetId;
    private String target;
    private final View icon;
    private final VoteObserver observer;
    private final TextSwitcher count;

    public SubmitVoteTask(String targetId, String target, View icon, TextSwitcher count, VoteObserver observer) {
        super();
        this.targetId = targetId;
        this.target = target;
        this.icon = icon;
        this.count = count;
        this.observer = observer;
    }

    @Override
    public Void doInBackground(Void... params) {
        HttpParams httpParameters = new BasicHttpParams();
        int timeoutConnection = 20;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection * 1000);
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutConnection * 1000);
        HttpClient httpclient = new DefaultHttpClient(httpParameters);
        try {
            // Create a response handler
            String token;
            if (SecurityService.getInstance().isLoggedIn()) {
                token = SecurityService.getInstance().getCredentials().getToken();
            } else {
                token = "";
            }
            String uri = BenchService.SERVER + "/api/vote/" + target + "/" + targetId + "?securityToken=" + token;
            Log.i(this.getClass().getSimpleName(), "Voting to " + uri);
            HttpPost req = new HttpPost(uri);

            BasicResponseStatusHandler responseHandler = new BasicResponseStatusHandler();
            String response = httpclient.execute(req, responseHandler);
            Log.i(this.getClass().getSimpleName(), "Response: " + response);
            GenericApiResponse apiResponse = new Gson().fromJson(response, GenericApiResponse.class);
            Log.i(this.getClass().getSimpleName(), "Response: " + apiResponse);
            observer.notifyVoteSucceeded(icon, count);
        } catch (UnknownHostException e) {
            Log.w(this.getClass().getSimpleName(), "No network access: " + e.getMessage());
            observer.notifyVoteFailed(icon);
        } catch (HttpHostConnectException e) {
            Log.i(this.getClass().getName(), "Failed to connect to HWBOT server! Are you connected to the internet?");
            e.printStackTrace();
            observer.notifyVoteFailed(icon);
        } catch (Exception e) {
            Log.i(this.getClass().getName(),
                    "Error communicating with online service. If this issue persists, please contact HWBOT crew. Error: " + e.getMessage());
            e.printStackTrace();
            observer.notifyVoteFailed(icon);
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
        return null;
    }

}
