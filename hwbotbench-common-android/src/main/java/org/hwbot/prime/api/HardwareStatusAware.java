package org.hwbot.prime.api;

import org.hwbot.api.bench.dto.DeviceInfoDTO;

public interface HardwareStatusAware {

    public enum Status {
        no_network, service_down, unknown_device
    }

    public void notifyDeviceInfo(final DeviceInfoDTO deviceInfo);

    public void notifyDeviceInfoFailed(Status status);

}
