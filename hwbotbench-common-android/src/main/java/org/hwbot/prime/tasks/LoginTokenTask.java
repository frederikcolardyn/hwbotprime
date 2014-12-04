package org.hwbot.prime.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;

import org.hwbot.api.bench.dto.PersistentLoginDTO;
import org.hwbot.prime.api.NetworkStatusAware;
import org.hwbot.prime.api.PersistentLoginAware;
import org.hwbot.prime.service.BenchService;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

public class LoginTokenTask extends AsyncTask<Void, Void, PersistentLoginDTO> {

    private String token;
    private PersistentLoginAware observer;
    private NetworkStatusAware networkStatusAware;

    public LoginTokenTask(NetworkStatusAware networkStatusAware, PersistentLoginAware observer, String token) {
        this.networkStatusAware = networkStatusAware;
        this.observer = observer;
        this.token = token;
    }

    @Override
    protected PersistentLoginDTO doInBackground(Void... params) {
        BufferedReader reader = null;
        try {
            URL hwbotRanking = new URL(BenchService.SERVER + "/api/authenticate?token=" + token);
            reader = new BufferedReader(new InputStreamReader(hwbotRanking.openStream()));
            PersistentLoginDTO loginToken = new Gson().fromJson(reader, PersistentLoginDTO.class);
            // Log.i(this.getClass().getSimpleName(), "Token: " + loginToken);
            observer.notifyPersistentLoginOk(loginToken);
            return loginToken;
        } catch (UnknownHostException e) {
            Log.w(this.getClass().getSimpleName(), "No network access: " + e.getMessage());
            networkStatusAware.showNetworkPopupOnce();
        } catch (Exception e) {
            Log.i(this.getClass().getSimpleName(), "Error: " + e.getMessage());
            e.printStackTrace();
            observer.notifyPersistentLoginFailed("Login failed: " + e.getMessage());
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