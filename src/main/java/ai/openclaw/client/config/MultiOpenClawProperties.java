package ai.openclaw.client.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MultiOpenClawProperties {

    @JsonProperty("instances")
    private Map<String, InstanceConfig> instances;

    @JsonProperty("default-instance")
    private String defaultInstance;

    @JsonIgnore
    private final Map<String, OpenClawProperties> propertiesCache = new ConcurrentHashMap<>();

    public Map<String, InstanceConfig> getInstances() {
        return instances;
    }

    public void setInstances(Map<String, InstanceConfig> instances) {
        this.instances = instances;
    }

    public String getDefaultInstance() {
        return defaultInstance;
    }

    public void setDefaultInstance(String defaultInstance) {
        this.defaultInstance = defaultInstance;
    }

    public OpenClawProperties getProperties(String instanceName) {
        if (instanceName == null) {
            instanceName = defaultInstance;
        }
        
        if (instanceName == null && instances != null && !instances.isEmpty()) {
            instanceName = instances.keySet().iterator().next();
        }
        
        return propertiesCache.computeIfAbsent(instanceName, name -> {
            InstanceConfig config = instances != null ? instances.get(name) : null;
            if (config == null) {
                return null;
            }
            
            OpenClawProperties props = new OpenClawProperties();
            props.setBaseUrl(config.getBaseUrl());
            props.setToken(config.getToken());
            props.setHooksPath(config.getHooksPath() != null ? config.getHooksPath() : "/hooks");
            props.setDefaultTimeoutSeconds(config.getDefaultTimeoutSeconds() != null ? config.getDefaultTimeoutSeconds() : 120);
            props.setDefaultWakeMode(config.getDefaultWakeMode() != null ? config.getDefaultWakeMode() : "now");
            props.setConnectTimeoutMs(config.getConnectTimeoutMs() != null ? config.getConnectTimeoutMs() : 10000);
            props.setReadTimeoutMs(config.getReadTimeoutMs() != null ? config.getReadTimeoutMs() : 30000);
            
            if (config.getHooks() != null) {
                props.setHooks(config.getHooks());
            }
            
            return props;
        });
    }

    public OpenClawProperties getDefaultProperties() {
        return getProperties(defaultInstance);
    }

    public boolean hasInstance(String name) {
        return instances != null && instances.containsKey(name);
    }

    public static class InstanceConfig {

        @JsonProperty("base-url")
        private String baseUrl;

        @JsonProperty("token")
        private String token;

        @JsonProperty("hooks-path")
        private String hooksPath = "/hooks";

        @JsonProperty("default-timeout-seconds")
        private Integer defaultTimeoutSeconds;

        @JsonProperty("default-wake-mode")
        private String defaultWakeMode;

        @JsonProperty("connect-timeout-ms")
        private Integer connectTimeoutMs;

        @JsonProperty("read-timeout-ms")
        private Integer readTimeoutMs;

        @JsonProperty("enabled")
        private boolean enabled = true;

        private HooksConfig hooks;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getHooksPath() {
            return hooksPath;
        }

        public void setHooksPath(String hooksPath) {
            this.hooksPath = hooksPath;
        }

        public Integer getDefaultTimeoutSeconds() {
            return defaultTimeoutSeconds;
        }

        public void setDefaultTimeoutSeconds(Integer defaultTimeoutSeconds) {
            this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        }

        public String getDefaultWakeMode() {
            return defaultWakeMode;
        }

        public void setDefaultWakeMode(String defaultWakeMode) {
            this.defaultWakeMode = defaultWakeMode;
        }

        public Integer getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(Integer connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public Integer getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(Integer readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public HooksConfig getHooks() {
            return hooks;
        }

        public void setHooks(HooksConfig hooks) {
            this.hooks = hooks;
        }
    }
}
