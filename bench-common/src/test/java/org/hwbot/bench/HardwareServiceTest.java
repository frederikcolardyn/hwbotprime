package org.hwbot.bench;

import junit.framework.Assert;

import org.hwbot.bench.service.HardwareService;
import org.hwbot.cpuid.CpuId;
import org.junit.Before;
import org.junit.Test;

public class HardwareServiceTest {

    HardwareService hardwareService;

    @Before
    public void setup() {
        hardwareService = new HardwareService();
    }

    @Test
    public void testLoadLibrary() {
        Assert.assertTrue(CpuId.sampleFrequency() > 0f);
        String model = CpuId.model();
        Assert.assertNotNull(model);
    }

    @Test
    public void testDefaultSpeed() {
        Float cpuFrequencyInHz = hardwareService.getEstimatedProcessorSpeed();
        Assert.assertTrue(cpuFrequencyInHz > 0);
        System.out.println("Default: " + cpuFrequencyInHz);
    }

    @Test
    public void testReadTemperature() {
        Float temperature = hardwareService.getProcessorTemperature();
        Assert.assertNotNull(temperature);
    }

}
