package org.hwbot.bench.prime;

import org.hwbot.bench.model.Hardware;

public interface HardwareService {

    public abstract Float getProcessorTemperature();

    public abstract Hardware gatherHardwareInfo();

    public abstract Integer getAvailableProcessors();

}