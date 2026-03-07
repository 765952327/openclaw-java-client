package cn.welsione.client.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class HooksConfig {

    @JsonProperty("enabled")
    private boolean enabled = false;

    @JsonProperty("token")
    private String token;

    @JsonProperty("path")
    private String path = "/hooks";

    @JsonProperty("presets")
    private List<String> presets;

    @JsonProperty("mappings")
    private Map<String, HookMapping> mappings;

    @JsonProperty("transforms-dir")
    private String transformsDir;

    @JsonIgnore
    private boolean initialized = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getPresets() {
        return presets;
    }

    public void setPresets(List<String> presets) {
        this.presets = presets;
    }

    public Map<String, HookMapping> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, HookMapping> mappings) {
        this.mappings = mappings;
    }

    public String getTransformsDir() {
        return transformsDir;
    }

    public void setTransformsDir(String transformsDir) {
        this.transformsDir = transformsDir;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isValid() {
        if (!enabled) {
            return true;
        }
        return token != null && !token.isEmpty();
    }

    public static class HookMapping {

        @JsonProperty("action")
        private String action;

        @JsonProperty("match")
        private MatchConfig match;

        @JsonProperty("template")
        private Map<String, Object> template;

        @JsonProperty("transform")
        private TransformConfig transform;

        @JsonProperty("deliver")
        private Boolean deliver;

        @JsonProperty("channel")
        private String channel;

        @JsonProperty("to")
        private String to;

        @JsonProperty("name")
        private String name;

        @JsonProperty("message")
        private String message;

        @JsonProperty("sessionKey")
        private String sessionKey;

        @JsonProperty("wakeMode")
        private String wakeMode;

        @JsonProperty("model")
        private String model;

        @JsonProperty("thinking")
        private String thinking;

        @JsonProperty("timeoutSeconds")
        private Integer timeoutSeconds;

        @JsonProperty("allowUnsafeExternalContent")
        private Boolean allowUnsafeExternalContent;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public MatchConfig getMatch() {
            return match;
        }

        public void setMatch(MatchConfig match) {
            this.match = match;
        }

        public Map<String, Object> getTemplate() {
            return template;
        }

        public void setTemplate(Map<String, Object> template) {
            this.template = template;
        }

        public TransformConfig getTransform() {
            return transform;
        }

        public void setTransform(TransformConfig transform) {
            this.transform = transform;
        }

        public Boolean getDeliver() {
            return deliver;
        }

        public void setDeliver(Boolean deliver) {
            this.deliver = deliver;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSessionKey() {
            return sessionKey;
        }

        public void setSessionKey(String sessionKey) {
            this.sessionKey = sessionKey;
        }

        public String getWakeMode() {
            return wakeMode;
        }

        public void setWakeMode(String wakeMode) {
            this.wakeMode = wakeMode;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getThinking() {
            return thinking;
        }

        public void setThinking(String thinking) {
            this.thinking = thinking;
        }

        public Integer getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public Boolean getAllowUnsafeExternalContent() {
            return allowUnsafeExternalContent;
        }

        public void setAllowUnsafeExternalContent(Boolean allowUnsafeExternalContent) {
            this.allowUnsafeExternalContent = allowUnsafeExternalContent;
        }
    }

    public static class MatchConfig {

        @JsonProperty("source")
        private String source;

        @JsonProperty("type")
        private String type;

        @JsonProperty("pattern")
        private String pattern;

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
    }

    public static class TransformConfig {

        @JsonProperty("module")
        private String module;

        @JsonProperty("function")
        private String function;

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public String getFunction() {
            return function;
        }

        public void setFunction(String function) {
            this.function = function;
        }
    }
}
