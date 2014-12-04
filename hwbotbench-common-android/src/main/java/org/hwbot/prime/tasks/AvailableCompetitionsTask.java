package org.hwbot.prime.tasks;

import android.os.AsyncTask;
import android.util.Log;
import com.google.gson.*;
import org.hwbot.api.bench.dto.DeviceRecordsDTO;
import org.hwbot.api.esports.CompetitionStageDTO;
import org.hwbot.prime.api.CompetitionsStatusAware;
import org.hwbot.prime.api.HardwareRecordsStatusAware;
import org.hwbot.prime.api.NetworkStatusAware;
import org.hwbot.prime.service.BenchService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class AvailableCompetitionsTask extends AsyncTask<Integer, Void, List<CompetitionStageDTO>> {

    private CompetitionsStatusAware observer;
    private final NetworkStatusAware networkStatusAware;

    public AvailableCompetitionsTask(NetworkStatusAware networkStatusAware, CompetitionsStatusAware observer) {
        this.networkStatusAware = networkStatusAware;
        this.observer = observer;
    }

    @Override
    public List<CompetitionStageDTO> doInBackground(Integer... param) {
        Integer deviceId = (param == null || param.length == 0) ? null : param[0];

        if (deviceId == null){
            // need to know device for competition
            return Collections.emptyList();
        }

        if (observer == null) {
            Log.w(this.getClass().getSimpleName(), "No device or observer loaded, can not load records.");
            return null;
        }
        Reader reader = null;
        try {
            // Creates the json object which will manage the information received
            GsonBuilder builder = new GsonBuilder();
            // Register an adapter to manage the date types as long values
            builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    return new Date(json.getAsJsonPrimitive().getAsLong());
                }
            });
            Gson gson = builder.create();

            URL url = new URL(BenchService.SERVER + "/api/round/stages/open?applicationId=57" + (deviceId == null ? "" : "&deviceId="+deviceId));

            // Log.i(this.getClass().getSimpleName(), "Loading available competitions from: " + url);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            CompetitionStageDTO[] list = gson.fromJson(reader, CompetitionStageDTO[].class);

            // Log.i(this.getClass().getSimpleName(), "Loaded: " + (list == null ? "no competitions" : list.length + " competitions"));
            if (list != null && list.length > 0){
                observer.notifyAvailableCompetitions(Arrays.asList(list));
            }
        } catch (UnknownHostException e) {
            Log.w(this.getClass().getSimpleName(), "No network access: " + e.getMessage());
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
}