package org.hwbot.bench.prime;

public interface ProgressBar {

    void setValue(int i);

    void setMaxValue(int max);

    int getProgress();

}
