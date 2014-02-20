package org.hwbot.bench.service;

import org.hwbot.bench.model.Hardware;
import org.hwbot.bench.prime.HardwareService;

public class HardwareServiceUnknown implements HardwareService {

    @Override
    public Float getProcessorTemperature() {
        return null;
    }

    @Override
    public Hardware gatherHardwareInfo() {
        return null;
    }

    @Override
    public Integer getAvailableProcessors() {
        return 1;
    }

}
