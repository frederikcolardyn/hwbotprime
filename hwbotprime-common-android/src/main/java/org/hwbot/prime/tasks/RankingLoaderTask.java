package org.hwbot.prime.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.hwbot.prime.api.NetworkStatusAware;
import org.hwbot.prime.api.SubmissionRankingAware;
import org.hwbot.prime.model.Result;
import org.hwbot.prime.model.SubmissionRanking;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

public class RankingLoaderTask extends AsyncTask<Void, Void, SubmissionRanking> {

    private SubmissionRankingAware observer;
    private String url;
    private NetworkStatusAware networkStatusAware;

    public RankingLoaderTask(NetworkStatusAware networkStatusAware, SubmissionRankingAware observer, String url) {
        this.observer = observer;
        this.url = url;
    }

    @Override
    protected SubmissionRanking doInBackground(Void... params) {
        JsonReader reader = null;
        try {
            // TODO artificial delay
            Thread.sleep(400);
            URL hwbotRanking = new URL(url);
            BufferedReader in = new BufferedReader(new InputStreamReader(hwbotRanking.openStream()));
            reader = new JsonReader(in);
            SubmissionRanking ranking = readSubmissionRanking(reader);
            observer.notifySubmissionRanking(ranking);
            return ranking;
        } catch (UnknownHostException e) {
            networkStatusAware.showNetworkPopupOnce();
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private SubmissionRanking readSubmissionRanking(JsonReader reader) {
        SubmissionRanking ranking = new SubmissionRanking();
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("application")) {
                    readApplicationName(reader, ranking);
                } else if (name.equals("list")) {
                    ranking.setSubmissions(readSubmissionArray(reader));
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException e) {
            Log.e(this.getClass().getName(), "error loading rankings: " + e.getMessage());
            e.printStackTrace();
        }

        return ranking;
    }

    private void readApplicationName(JsonReader reader, SubmissionRanking ranking) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("name")) {
                ranking.setApplicationName(reader.nextString());
            } else if (name.equals("id")) {
                ranking.setApplicationId(reader.nextInt());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private List<Result> readSubmissionArray(JsonReader reader) throws IOException {
        List<Result> results = new ArrayList<Result>();
        reader.beginArray();
        while (reader.hasNext()) {
            results.add(readResult(reader));
        }
        reader.endArray();
        return results;
    }

    private Result readResult(JsonReader reader) throws IOException {
        Result result = new Result();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("user")) {
                result.user = reader.nextString();
            } else if (name.equals("team")) {
                result.team = reader.nextString();
            } else if (name.equals("hardware")) {
                result.hardware = reader.nextString();
            } else if (name.equals("score")) {
                result.score = reader.nextString();
            } else if (name.equals("points")) {
                result.points = reader.nextString();
            } else if (name.equals("country")) {
                result.country = reader.nextString();
            } else if (name.equals("id")) {
                result.id = reader.nextInt();
            } else if (name.equals("image")) {
                result.image = reader.nextString();
            } else if (name.equals("description")) {
                result.description = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return result;
    }

}