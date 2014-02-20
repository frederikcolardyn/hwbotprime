package org.hwbot.bench;

import java.util.HashMap;
import java.util.Map;

public class BenchmarkConfiguration {

    Map<String, Object> config = new HashMap<String, Object>();

    public Object getValue(String key) {
        return config.get(key);
    }

    public void setValue(String key, Object value) {
        config.put(key, value);
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return "BenchmarkConfiguration [config=" + config + "]";
    }

}
