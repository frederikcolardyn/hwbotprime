package org.hwbot.opengl;

import org.hwbot.bench.BenchmarkConfiguration;
import org.hwbot.prime.service.AndroidHardwareService;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.DataServiceXml;
import org.hwbot.prime.service.SecurityService;

import android.util.Log;

public class OpenGLBenchService extends BenchService {

    protected static OpenGLBenchService benchService;
    public String version = "0.0.1";
    public static String HWBOT_PRIME_APP_NAME = "HWBOT 3D";

    private OpenGLBenchService() {
        hardwareService = AndroidHardwareService.getInstance();
        securityService = SecurityService.getInstance();
        dataServiceXml = DataServiceXml.getInstance();
        if (version == null) {
            version = HWBOT_APP_CLIENT_DEV_VERSION;
        }
    }

    public static OpenGLBenchService getInstance() {
        if (benchService == null) {
            benchService = new OpenGLBenchService();
        }
        return benchService;
    }

    public OpenGLBenchmark instantiateBenchmark() {
        BenchmarkConfiguration configuration = new BenchmarkConfiguration();
        // return new PrimeBenchmark(this, super.benchUI, super.availableProcessors, super.progressBar, super.compareButton);
        return new OpenGLBenchmark(configuration, Runtime.getRuntime().availableProcessors(), super.progressBar);
    }

    public String getVersion() {
        Log.i("version", version);
        return version;
    }

}
