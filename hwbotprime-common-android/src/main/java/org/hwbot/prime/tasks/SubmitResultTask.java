package org.hwbot.prime.tasks;

import java.io.Reader;
import java.io.StringReader;
import java.net.UnknownHostException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.hwbot.prime.api.NetworkStatusAware;
import org.hwbot.prime.api.SubmissionStatusAware;
import org.hwbot.prime.service.BasicResponseStatusHandler;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SubmitResponse;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

public class SubmitResultTask extends AsyncTask<Void, Void, SubmitResponse> {

    private final byte[] data;
    private SubmissionStatusAware submissionStatusAware;
    private NetworkStatusAware networkStatusAware;

    public SubmitResultTask(NetworkStatusAware networkStatusAware, SubmissionStatusAware submissionStatusAware, byte[] data) {
        this.submissionStatusAware = submissionStatusAware;
        this.data = data;
    }

    @Override
    public SubmitResponse doInBackground(Void... params) {
        HttpParams httpParameters = new BasicHttpParams();
        int timeoutConnection = 20;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection * 1000);
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutConnection * 1000);
        HttpClient httpclient = new DefaultHttpClient(httpParameters);
        try {
            // Create a response handler
            HttpPost req = new HttpPost(BenchService.SERVER + "/submit/api?client=hwbot_prime&clientVersion=0.8.3&mode=remote");
            MultipartEntity mpEntity = new MultipartEntity();
            mpEntity.addPart("data", new ByteArrayBody(data, "data"));
            req.setEntity(mpEntity);

            BasicResponseStatusHandler responseHandler = new BasicResponseStatusHandler();
            String response = httpclient.execute(req, responseHandler);

            Reader in = new StringReader(response);
            JsonReader reader = null;
            try {
                reader = new JsonReader(in);
                Log.i(this.getClass().getSimpleName(), "Reading response: " + response);
                return readSubmitResponse(reader);
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), "Error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                reader.close();
                in.close();
            }
        } catch (UnknownHostException e) {
            networkStatusAware.showNetworkPopupOnce();
        } catch (HttpHostConnectException e) {
            Log.i(this.getClass().getName(), "Failed to connect to HWBOT server! Are you connected to the internet?");
            e.printStackTrace();
        } catch (Exception e) {
            Log.i(this.getClass().getName(),
                    "Error communicating with online service. If this issue persists, please contact HWBOT crew. Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
        return null;
    }

    @Override
    protected void onPostExecute(SubmitResponse result) {
        if (result != null) {
            submissionStatusAware.notifySubmissionDone(result);
        }
        super.onPostExecute(result);
    }

    private SubmitResponse readSubmitResponse(JsonReader reader) {
        SubmitResponse response = new SubmitResponse();
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("url")) {
                    response.setUrl(reader.nextString());
                } else if (name.equals("message")) {
                    response.setMessage(reader.nextString());
                } else if (name.equals("status")) {
                    response.setStatus(reader.nextString());
                } else if (name.equals("technicalMessage")) {
                    JsonToken peek = reader.peek();
                    if (JsonToken.NULL.equals(peek)) {
                        reader.nextNull();
                    } else {
                        response.setTechnicalMessage(reader.nextString());
                    }
                } else {
                    Log.i(this.getClass().getSimpleName(), "unkown tag: " + name);
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (Exception e) {
            Log.e(this.getClass().getName(), "error parsing response: " + e.getMessage());
            e.printStackTrace();
            response.setMessage("Technical difficulties... sorry!");
            response.setStatus("error");
        }
        return response;
    }

}
