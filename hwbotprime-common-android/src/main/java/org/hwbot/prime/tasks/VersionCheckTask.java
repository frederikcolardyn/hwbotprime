package org.hwbot.prime.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;

import org.apache.commons.lang.StringUtils;
import org.hwbot.api.submit.dto.VersionApiResponse;
import org.hwbot.prime.api.NetworkStatusAware;
import org.hwbot.prime.api.VersionStatusAware;
import org.hwbot.prime.service.BenchService;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import com.google.gson.Gson;

public class VersionCheckTask extends AsyncTask<Void, Void, Void> {

    private final NetworkStatusAware networkStatusAware;
    private final VersionStatusAware versionStatusAware;
    
    private static boolean checked = false;

    public VersionCheckTask(NetworkStatusAware networkStatusAware, VersionStatusAware versionStatusAware) {
        this.networkStatusAware = networkStatusAware;
        this.versionStatusAware = versionStatusAware;
    }

    @Override
    public Void doInBackground(Void... params) {
        if (checked){
            Log.i(this.getClass().getSimpleName(), "Version check skipped.");
            return null;
        }
        JsonReader reader = null;
        try {
            URL url = new URL(BenchService.SERVER + "/version/api?client=" + URLEncoder.encode(BenchService.HWBOT_PRIME_APP_NAME, "utf8") + "&os=android");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

            VersionApiResponse apiResponse = new Gson().fromJson(in, VersionApiResponse.class);
            Log.i(this.getClass().getSimpleName(), "Version check: " + apiResponse);
            checked = true;

            if (StringUtils.isNotEmpty(apiResponse.getError())) {
                Log.w(this.getClass().getSimpleName(), apiResponse.getError());
            } else if (apiResponse.getVersion() != null && apiResponse.getVersion().equals(BenchService.getInstance().version)) {
                // all ok!
                Log.i(this.getClass().getSimpleName(), "Latest version, all ok!");
            } else if (apiResponse.getSupportedVersions() != null && apiResponse.getSupportedVersions().contains(BenchService.getInstance().version)) {
                Log.w(this.getClass().getSimpleName(), "Not the latest version, but still supported.");
                versionStatusAware.showNewVersionPopup(apiResponse.getVersion(), apiResponse.getUrl(), false);
            } else {
                // not supported!
                Log.w(this.getClass().getSimpleName(), "Version no longer supported.");
                versionStatusAware.showNewVersionPopup(apiResponse.getVersion(), apiResponse.getUrl(), true);
            }
        } catch (UnknownHostException e) {
            Log.w(this.getClass().getSimpleName(), "No network access: " + e.getMessage());
            networkStatusAware.showNetworkPopupOnce();
        } catch (Exception e) {
            Log.w(this.getClass().getSimpleName(), "Error: " + e.getMessage());
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