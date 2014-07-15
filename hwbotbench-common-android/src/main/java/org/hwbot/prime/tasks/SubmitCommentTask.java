package org.hwbot.prime.tasks;

import java.net.UnknownHostException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.hwbot.api.generic.dto.GenericApiResponse;
import org.hwbot.prime.api.CommentObserver;
import org.hwbot.prime.service.BasicResponseStatusHandler;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextSwitcher;

import com.google.gson.Gson;

public class SubmitCommentTask extends AsyncTask<Void, Void, Void> {

    private String comment;
    private String targetId;
    private String target;
    private final View icon;
    private final CommentObserver observer;
    private TextSwitcher count;

    public SubmitCommentTask(String comment, String targetId, String target, View icon, TextSwitcher count, CommentObserver observer) {
        super();
        this.comment = comment;
        this.targetId = targetId;
        this.target = target;
        this.icon = icon;
        this.observer = observer;
        this.count = count;
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
            String uri = BenchService.SERVER + "/api/comment/" + target + "/" + targetId + "?securityToken=" + token;
            Log.i(this.getClass().getSimpleName(), "Posting comment to " + uri + " with text: " + comment);
            HttpPost req = new HttpPost(uri);
            MultipartEntity mpEntity = new MultipartEntity();
            mpEntity.addPart("text", new StringBody(comment));
            req.setEntity(mpEntity);

            BasicResponseStatusHandler responseHandler = new BasicResponseStatusHandler();
            String response = httpclient.execute(req, responseHandler);
            Log.i(this.getClass().getSimpleName(), "Response: " + response);
            GenericApiResponse apiResponse = new Gson().fromJson(response, GenericApiResponse.class);
            Log.i(this.getClass().getSimpleName(), "Response: " + apiResponse);
            observer.notifyCommentSucceeded(icon, count);
        } catch (UnknownHostException e) {
            Log.w(this.getClass().getSimpleName(), "No network access: " + e.getMessage());
            observer.notifyCommentFailed(icon);
        } catch (HttpHostConnectException e) {
            Log.i(this.getClass().getName(), "Failed to connect to HWBOT server! Are you connected to the internet?");
            e.printStackTrace();
            observer.notifyCommentFailed(icon);
        } catch (Exception e) {
            Log.i(this.getClass().getName(),
                    "Error communicating with online service. If this issue persists, please contact HWBOT crew. Error: " + e.getMessage());
            e.printStackTrace();
            observer.notifyCommentFailed(icon);
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
        return null;
    }

}
