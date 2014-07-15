package org.hwbot.prime;

import java.util.concurrent.TimeUnit;

import org.hwbot.bench.BenchmarkConfiguration;
import org.hwbot.bench.PrimeBenchmark;
import org.hwbot.bench.prime.PrimeEncryptionModule;
import org.hwbot.prime.service.AndroidHardwareService;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.DataServiceXml;
import org.hwbot.prime.service.SecurityService;

import android.util.Log;

public class PrimeBenchService extends BenchService {

    protected static PrimeBenchService benchService;
    public String version = "1.0.2";
    public static String HWBOT_PRIME_APP_NAME = "HWBOT Prime";

    private PrimeBenchService() {
        hardwareService = AndroidHardwareService.getInstance();
        SecurityService.setEncryptionModule(new PrimeEncryptionModule());
        securityService = SecurityService.getInstance();
        dataServiceXml = DataServiceXml.getInstance();
        if (version == null) {
            version = HWBOT_APP_CLIENT_DEV_VERSION;
        }
    }

    public static PrimeBenchService getInstance() {
        if (benchService == null) {
            benchService = new PrimeBenchService();
        }
        return benchService;
    }

    public PrimeBenchmark instantiateBenchmark() {
        BenchmarkConfiguration configuration = new BenchmarkConfiguration();
        configuration.setValue(PrimeBenchmark.TIME_SPAN, TimeUnit.SECONDS.toMillis(10));
        configuration.setValue(PrimeBenchmark.SILENT, false);
        // return new PrimeBenchmark(this, super.benchUI, super.availableProcessors, super.progressBar, super.compareButton);
        return new PrimeBenchmark(configuration, Runtime.getRuntime().availableProcessors(), this.progressBar);
    }

    public String getVersion() {
        Log.i("version", version);
        return version;
    }

}
