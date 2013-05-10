package org.hwbot.bench.ui.console;

import org.hwbot.bench.ui.ProgressBar;

public class SystemProgressBar implements ProgressBar {

    private int iterations;
    private int max;

    public SystemProgressBar(int iterations) {
        this.iterations = iterations;
    }

    public void setValue(int i) {
        System.out.print(".");
        // System.out.print((iterations--) + "/" + max + "...");
    }

    public void setMaxValue(int max) {
        this.max = max;
    }

}
