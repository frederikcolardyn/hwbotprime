package org.hwbot.opengl;

import org.hwbot.bench.opengl.core.BenchStateListener;
import org.hwbot.bench.opengl.core.HWBOTOpenGL;

import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.surfaceview.FixedResolutionStrategy;

public class BenchActivity extends AndroidApplication implements BenchStateListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        // FSAA
        // config.r = 8;
        // config.g = 8;
        // config.b = 8;
        // config.a = 2;
        config.numSamples = 2;
        /** number of bits for depth and stencil buffer **/
        // config.depth = 16;
        // config.stencil = 4;
        // config.numSamples = 0;
        // config.resolutionStrategy = new FillResolutionStrategy();
        config.resolutionStrategy = new FixedResolutionStrategy(800, 480);
        HWBOTOpenGL.listeners.add(this);
        // 800, 480
        initialize(new HWBOTOpenGL(), config);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // @Override
    public void notifySubTestFinished(String subtest) {
    }

    // @Override
    public void notifyTestFinished(int score) {
        Log.i(this.getClass().getSimpleName(), "Stopping activity, score " + score);
        finish();
    }
}