package org.hwbot.prime.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.UnknownHostException;

import org.hwbot.api.bench.dto.DeviceRecordsDTO;
import org.hwbot.prime.api.HardwareRecordsStatusAware;
import org.hwbot.prime.api.NetworkStatusAware;
import org.hwbot.prime.service.BenchService;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

public class HardwareRecordsTask extends AsyncTask<Void, Void, Void> {

    private HardwareRecordsStatusAware observer;
    private final NetworkStatusAware networkStatusAware;
    private final Integer deviceId;
    private final Integer userId;

    public HardwareRecordsTask(NetworkStatusAware networkStatusAware, HardwareRecordsStatusAware observer, Integer deviceId, Integer userId) {
        this.networkStatusAware = networkStatusAware;
        this.observer = observer;
        this.deviceId = deviceId;
        this.userId = userId;
    }

    @Override
    public Void doInBackground(Void... param) {
        if (deviceId == null || observer == null) {
            Log.w(this.getClass().getSimpleName(), "No device or observer loaded, can not load records.");
            return null;
        }
        Reader reader = null;
        try {
            URL url;
            if (userId != null) {
                url = new URL(BenchService.SERVER + "/api/hardware/device/" + deviceId + "/records?userId=" + userId);
            } else {
                url = new URL(BenchService.SERVER + "/api/hardware/device/" + deviceId + "/records");
            }
            Log.i(this.getClass().getSimpleName(), "Loading device records from: " + url);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            DeviceRecordsDTO deviceInfoWithRecordsDTO = new Gson().fromJson(reader, DeviceRecordsDTO.class);
            Log.i(this.getClass().getSimpleName(), "Loaded: " + deviceInfoWithRecordsDTO);

            if (deviceInfoWithRecordsDTO == null) {
                observer.notifyRecordsFailed(org.hwbot.prime.api.HardwareStatusAware.Status.unknown_device);
            } else if (userId != null) {
                observer.notifyDevicePersonalRecords(deviceInfoWithRecordsDTO);
            } else {
                observer.notifyDeviceRecords(deviceInfoWithRecordsDTO);
            }
        } catch (UnknownHostException e) {
            Log.w(this.getClass().getSimpleName(), "No network access: " + e.getMessage());
            networkStatusAware.showNetworkPopupOnce();
            observer.notifyRecordsFailed(org.hwbot.prime.api.HardwareStatusAware.Status.no_network);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Error: " + e.getMessage());
            e.printStackTrace();
            observer.notifyRecordsFailed(org.hwbot.prime.api.HardwareStatusAware.Status.service_down);
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