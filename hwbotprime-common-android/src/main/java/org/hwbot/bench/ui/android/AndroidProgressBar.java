package org.hwbot.bench.ui.android;

import org.hwbot.bench.prime.ProgressBar;

public class AndroidProgressBar implements ProgressBar {

    private final android.widget.ProgressBar progressbar;

    public AndroidProgressBar(android.widget.ProgressBar progressbar, int i) {
        this.progressbar = progressbar;
        this.progressbar.setMax(i);
    }

    public void setValue(int i) {
        progressbar.setProgress(i);
    }

    public void setMaxValue(int max) {
        progressbar.setMax(max);
    }

    public int getProgress() {
        return progressbar.getProgress();
    }

}
