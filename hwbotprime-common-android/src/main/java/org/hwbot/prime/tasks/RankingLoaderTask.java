package org.hwbot.prime.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;

import org.hwbot.prime.api.NetworkStatusAware;
import org.hwbot.prime.api.SubmissionRankingAware;
import org.hwbot.prime.model.SubmissionRanking;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

public class RankingLoaderTask extends AsyncTask<Void, Void, SubmissionRanking> {

    private SubmissionRankingAware observer;
    private String url;
    private NetworkStatusAware networkStatusAware;

    public RankingLoaderTask(NetworkStatusAware networkStatusAware, SubmissionRankingAware observer, String url) {
        this.networkStatusAware = networkStatusAware;
        this.observer = observer;
        this.url = url;
    }

    @Override
    protected SubmissionRanking doInBackground(Void... params) {
        BufferedReader in = null;
        try {
            URL hwbotRanking = new URL(url);
            in = new BufferedReader(new InputStreamReader(hwbotRanking.openStream()));
            SubmissionRanking ranking = new Gson().fromJson(in, SubmissionRanking.class);

            observer.notifySubmissionRanking(ranking);
            return ranking;
        } catch (UnknownHostException e) {
            Log.w(this.getClass().getSimpleName(), "No network access: " + e.getMessage());
            networkStatusAware.showNetworkPopupOnce();
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}