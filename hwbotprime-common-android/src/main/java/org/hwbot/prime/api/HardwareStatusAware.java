package org.hwbot.prime.api;

import org.hwbot.prime.model.DeviceInfo;

public interface HardwareStatusAware {

    public enum Status {
        no_network, service_down
    }

    public void notifyDeviceInfo(final DeviceInfo deviceInfo);

    public void notifyDeviceInfoFailed(Status status);

}
