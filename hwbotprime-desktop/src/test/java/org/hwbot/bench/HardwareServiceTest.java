package org.hwbot.bench;

import junit.framework.Assert;

import org.hwbot.bench.prime.HardwareService;
import org.hwbot.bench.service.HardwareServiceFactory;
import org.hwbot.cpuid.CpuId;
import org.junit.Before;
import org.junit.Test;

public class HardwareServiceTest {

    HardwareService hardwareService;

    @Before
    public void setup() {
        hardwareService = HardwareServiceFactory.getInstance();
    }

    @Test
    public void testLoadLibrary() {
        Assert.assertTrue(CpuId.sampleFrequency() > 0f);
        String model = CpuId.model();
        Assert.assertNotNull(model);
    }

    @Test
    public void testDefaultSpeed() {
        Float cpuFrequencyInHz = hardwareService.gatherHardwareInfo().getProcessor().getCoreClock();
        Assert.assertTrue(cpuFrequencyInHz > 0);
        System.out.println("Default: " + cpuFrequencyInHz);
    }

    @Test
    public void testReadTemperature() {
        Float temperature = hardwareService.getProcessorTemperature();
        Assert.assertNotNull(temperature);
    }

}
