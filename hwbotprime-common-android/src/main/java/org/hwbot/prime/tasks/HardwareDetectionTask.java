package org.hwbot.prime.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;

import org.hwbot.prime.api.HardwareStatusAware;
import org.hwbot.prime.api.NetworkStatusAware;
import org.hwbot.prime.model.DeviceInfo;
import org.hwbot.prime.service.BenchService;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

public class HardwareDetectionTask extends AsyncTask<Void, Void, DeviceInfo> {

    private HardwareStatusAware observer;
    private String deviceName;
    private final NetworkStatusAware networkStatusAware;

    public HardwareDetectionTask(NetworkStatusAware networkStatusAware, HardwareStatusAware observer, String deviceName) {
        this.networkStatusAware = networkStatusAware;
        this.observer = observer;
        this.deviceName = deviceName;
    }

    @Override
    public DeviceInfo doInBackground(Void... params) {
        JsonReader reader = null;
        try {
            URL url = new URL(BenchService.SERVER + "/api/hardware/device/" + deviceName);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            reader = new JsonReader(in);
            DeviceInfo deviceInfo = readDeviceInfo(reader);
            observer.notifyDeviceInfo(deviceInfo);
        } catch (UnknownHostException e) {
            Log.w(this.getClass().getSimpleName(), "No network access: " + e.getMessage());
            networkStatusAware.showNetworkPopupOnce();
            observer.notifyDeviceInfoFailed(org.hwbot.prime.api.HardwareStatusAware.Status.no_network);
        } catch (Exception e) {
            Log.i(this.getClass().getSimpleName(), "Error: " + e.getMessage());
            e.printStackTrace();
            observer.notifyDeviceInfo(DeviceInfo.dummy);
            // observer.notifyDeviceInfoFailed(org.hwbot.prime.api.HardwareStatusAware.Status.no_network);
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

    private DeviceInfo readDeviceInfo(JsonReader reader) {

        Integer id = null;
        Integer soCid = null;
        Integer processorId = null;
        Integer processorCoreId = null;
        Integer processorSubFamilyId = null;
        Integer processorFamilyId = null;
        Integer videocardId = null;
        String processor = null;
        String processorCore = null;
        String processorSubFamily = null;
        String processorFamily = null;
        Integer processorClock = null;
        String videocard = null;
        String deviceName = null;
        Integer ram = null;

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("id")) {
                    id = reader.nextInt();
                } else if (name.equals("soCid")) {
                    soCid = reader.nextInt();
                } else if (name.equals("videocardId")) {
                    videocardId = reader.nextInt();
                } else if (name.equals("processor")) {
                    processor = reader.nextString();
                } else if (name.equals("processorId")) {
                    processorId = reader.nextInt();
                } else if (name.equals("processorCore")) {
                    processorCore = reader.nextString();
                } else if (name.equals("processorCoreId")) {
                    processorCoreId = reader.nextInt();
                } else if (name.equals("processorSubFamily")) {
                    processorSubFamily = reader.nextString();
                } else if (name.equals("processorSubFamilyId")) {
                    processorSubFamilyId = reader.nextInt();
                } else if (name.equals("processorFamily")) {
                    processorFamily = reader.nextString();
                } else if (name.equals("processorFamilyId")) {
                    processorFamilyId = reader.nextInt();
                } else if (name.equals("videocard")) {
                    videocard = reader.nextString();
                } else if (name.equals("ram")) {
                    ram = reader.nextInt();
                } else if (name.equals("name")) {
                    deviceName = reader.nextString();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }

        DeviceInfo deviceInfo = new DeviceInfo(id, deviceName, soCid, processorId, videocardId, processor, processorClock, videocard, ram);
        deviceInfo.setProcessorCore(processorCore);
        deviceInfo.setProcessorSubFamily(processorSubFamily);
        deviceInfo.setProcessorFamily(processorFamily);
        deviceInfo.setProcessorCoreId(processorCoreId);
        deviceInfo.setProcessorSubFamilyId(processorSubFamilyId);
        deviceInfo.setProcessorFamilyId(processorFamilyId);
        return deviceInfo;
    }
}