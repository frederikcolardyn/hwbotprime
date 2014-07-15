package org.hwbot.opengl;

import org.hwbot.bench.Benchmark;
import org.hwbot.bench.BenchmarkConfiguration;
import org.hwbot.bench.gpu.managers.EndState;
import org.hwbot.bench.prime.ProgressBar;

public class OpenGLBenchmark extends Benchmark {

    public OpenGLBenchmark(BenchmarkConfiguration config, int threads, ProgressBar progressBar) {
        super(config, threads, progressBar);
    }

    @Override
    public Number benchmark(BenchmarkConfiguration configuration) {
        while (OpenGLActivity.hwbotOpenGL.gsm.state != EndState.STATE) {
            // wait
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return OpenGLActivity.hwbotOpenGL.gsm.score;
    }

    @Override
    public String getClient() {
        return "HWBOT OpenGL";
    }

}
