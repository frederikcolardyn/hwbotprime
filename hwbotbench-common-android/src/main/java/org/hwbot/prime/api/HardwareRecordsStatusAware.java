package org.hwbot.prime.api;

import org.hwbot.api.bench.dto.DeviceRecordsDTO;

public interface HardwareRecordsStatusAware {

    public void notifyDeviceRecords(final DeviceRecordsDTO records);

    public void notifyDevicePersonalRecords(final DeviceRecordsDTO records);

    public void notifyRecordsFailed(HardwareStatusAware.Status status);

}
