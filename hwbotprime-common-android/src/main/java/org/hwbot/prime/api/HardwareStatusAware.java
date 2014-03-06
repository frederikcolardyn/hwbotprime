package org.hwbot.prime.api;

import org.hwbot.prime.model.DeviceInfo;

public interface HardwareStatusAware {

	public void notifyDeviceInfo(final DeviceInfo deviceInfo);

}
