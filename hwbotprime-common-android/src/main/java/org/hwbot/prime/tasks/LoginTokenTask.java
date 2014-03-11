package org.hwbot.prime.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;

import org.hwbot.prime.api.NetworkStatusAware;
import org.hwbot.prime.api.PersistentLoginAware;
import org.hwbot.prime.model.PersistentLogin;
import org.hwbot.prime.service.BenchService;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

public class LoginTokenTask extends AsyncTask<Void, Void, PersistentLogin> {

    private String token;
    private PersistentLoginAware observer;
    private NetworkStatusAware networkStatusAware;

    public LoginTokenTask(NetworkStatusAware networkStatusAware, PersistentLoginAware observer, String token) {
        this.observer = observer;
        this.token = token;
    }

    @Override
    protected PersistentLogin doInBackground(Void... params) {
        JsonReader reader = null;
        try {
            URL hwbotRanking = new URL(BenchService.SERVER + "/api/authenticate?token=" + token);
            BufferedReader in = new BufferedReader(new InputStreamReader(hwbotRanking.openStream()));
            reader = new JsonReader(in);
            PersistentLogin loginToken = readLoginToken(reader);
            Log.i(this.getClass().getSimpleName(), "Token: " + loginToken);
            observer.notifyPersistentLoginOk(loginToken);
            return loginToken;
        } catch (UnknownHostException e) {
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

    public static PersistentLogin readLoginToken(JsonReader reader) {
        PersistentLogin login = new PersistentLogin();
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("userName")) {
                    login.setUserName(reader.nextString());
                } else if (name.equals("teamName")) {
                    login.setTeamName(reader.nextString());
                } else if (name.equals("countryName")) {
                    login.setCountryName(reader.nextString());
                } else if (name.equals("token")) {
                    login.setToken(reader.nextString());
                } else if (name.equals("dateUntil")) {
                    login.setDateUntil(reader.nextLong());
                } else if (name.equals("userId")) {
                    login.setUserId(reader.nextInt());
                } else if (name.equals("teamId")) {
                    login.setTeamId(reader.nextInt());
                } else if (name.equals("countryId")) {
                    login.setCountryId(reader.nextInt());
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException e) {
            Log.e(LoginTokenTask.class.getName(), "error loading rankings: " + e.getMessage());
            e.printStackTrace();
        }

        return login;
    }

}