package org.hwbot.bench.service;

import javax.swing.JLabel;

public class ProcessorFrequencyMonitor implements Runnable {

    private final JLabel frequency;
    private final HardwareService hardwareService;

    public ProcessorFrequencyMonitor(HardwareService hardwareService, JLabel frequency) {
        this.hardwareService = hardwareService;
        this.frequency = frequency;
    }
    
    public void run() {
        Float processorSpeed = hardwareService.getEstimatedProcessorSpeed();
        if (processorSpeed != null) {
            frequency.setText(BenchService.getProcessorFrequency(processorSpeed));
        }
    }

}
