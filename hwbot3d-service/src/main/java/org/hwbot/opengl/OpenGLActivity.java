package org.hwbot.opengl;

import org.hwbot.bench.opengl.core.HWBOTOpenGL;

import android.util.Log;
import android.view.WindowManager;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy;

public class OpenGLActivity extends AndroidApplication {

    public static HWBOTOpenGL hwbotOpenGL;

    public OpenGLActivity() {
        try {

            AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
            // FSAA
            // config.r = 8;
            // config.g = 8;
            // config.b = 8;
            // config.a = 2;

            /** number of bits for depth and stencil buffer **/
            // config.depth = 16;
            // config.stencil = 4;
            config.numSamples = 0;
            config.resolutionStrategy = new FillResolutionStrategy();
            // config.resolutionStrategy = new FixedResolutionStrategy(800, 480);
            // 800, 480
            hwbotOpenGL = new HWBOTOpenGL();
            initialize(hwbotOpenGL, config);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "error");
            e.printStackTrace();
        }
    }
}