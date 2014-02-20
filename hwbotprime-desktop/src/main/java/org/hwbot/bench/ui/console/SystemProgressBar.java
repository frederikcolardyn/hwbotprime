package org.hwbot.bench.ui.console;

import org.hwbot.bench.prime.ProgressBar;

public class SystemProgressBar implements ProgressBar {

    private int iterations;
    private int max;
    private int value;

    public SystemProgressBar(int iterations) {
        this.iterations = iterations;
    }

    public void setValue(int i) {
        this.value = i;
        System.out.print(".");
        // System.out.print((iterations--) + "/" + max + "...");
    }

    public void setMaxValue(int max) {
        this.max = max;
    }

    @Override
    public int getProgress() {
        return this.value;
    }

}
