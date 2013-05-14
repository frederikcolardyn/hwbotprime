package org.hwbot.bench;

import junit.framework.Assert;

import org.hwbot.bench.service.HardwareService;
import org.junit.Before;
import org.junit.Test;

public class HardwareServiceTest {

    HardwareService hardwareService;

    @Before
    public void setup() {
        hardwareService = new HardwareService();
    }

    @Test
    public void testDefaultSpeed() {
        Float cpuFrequencyInHz = hardwareService.getDefaultProcessorSpeed();
        Assert.assertTrue(cpuFrequencyInHz > 0);
        System.out.println("Default: " + cpuFrequencyInHz);
    }

    @Test
    public void testMeasureSpeed() {
        Float cpuFrequencyInHz = hardwareService.measureCpuSpeed();
        Assert.assertNotNull(cpuFrequencyInHz);
        System.out.println("Measured: " + cpuFrequencyInHz);
    }

}
