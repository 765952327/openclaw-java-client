package ai.openclaw.client.service;

import ai.openclaw.client.config.HooksConfig;
import ai.openclaw.client.config.OpenClawProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);

    private final OpenClawProperties properties;
    private final ObjectMapper objectMapper;

    public WebhookService(OpenClawProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    public boolean isEnabled() {
        return properties.isHooksEnabled();
    }

    public boolean validateToken(String token) {
        String expectedToken = properties.getHooksToken();
        if (expectedToken == null || expectedToken.isEmpty()) {
            return properties.getToken() != null && properties.getToken().equals(token);
        }
        return expectedToken.equals(token);
    }

    public Map<String, Object> getMappingConfig(String hookName) {
        HooksConfig hooksConfig = properties.getHooks();
        if (hooksConfig == null || hooksConfig.getMappings() == null) {
            return null;
        }
        
        HooksConfig.HookMapping mapping = hooksConfig.getMappings().get(hookName);
        if (mapping == null) {
            return null;
        }
        
        Map<String, Object> config = new HashMap<>();
        config.put("action", mapping.getAction());
        config.put("deliver", mapping.getDeliver());
        config.put("channel", mapping.getChannel());
        config.put("to", mapping.getTo());
        config.put("name", mapping.getName());
        config.put("message", mapping.getMessage());
        config.put("sessionKey", mapping.getSessionKey());
        config.put("wakeMode", mapping.getWakeMode());
        config.put("model", mapping.getModel());
        config.put("thinking", mapping.getThinking());
        config.put("timeoutSeconds", mapping.getTimeoutSeconds());
        config.put("allowUnsafeExternalContent", mapping.getAllowUnsafeExternalContent());
        
        if (mapping.getTemplate() != null) {
            config.put("template", mapping.getTemplate());
        }
        
        if (mapping.getMatch() != null) {
            Map<String, Object> matchConfig = new HashMap<>();
            matchConfig.put("source", mapping.getMatch().getSource());
            matchConfig.put("type", mapping.getMatch().getType());
            matchConfig.put("pattern", mapping.getMatch().getPattern());
            config.put("match", matchConfig);
        }
        
        if (mapping.getTransform() != null) {
            Map<String, Object> transformConfig = new HashMap<>();
            transformConfig.put("module", mapping.getTransform().getModule());
            transformConfig.put("function", mapping.getTransform().getFunction());
            config.put("transform", transformConfig);
        }
        
        return config;
    }

    public boolean hasMapping(String hookName) {
        HooksConfig hooksConfig = properties.getHooks();
        if (hooksConfig == null || hooksConfig.getMappings() == null) {
            return false;
        }
        return hooksConfig.getMappings().containsKey(hookName);
    }

    public String getMappingAction(String hookName) {
        HooksConfig.HookMapping mapping = getMapping(hookName);
        return mapping != null ? mapping.getAction() : null;
    }

    public HooksConfig.HookMapping getMapping(String hookName) {
        HooksConfig hooksConfig = properties.getHooks();
        if (hooksConfig == null || hooksConfig.getMappings() == null) {
            return null;
        }
        return hooksConfig.getMappings().get(hookName);
    }

    public boolean isPresetEnabled(String preset) {
        HooksConfig hooksConfig = properties.getHooks();
        if (hooksConfig == null || hooksConfig.getPresets() == null) {
            return false;
        }
        return hooksConfig.getPresets().contains(preset);
    }

    public String getTransformsDir() {
        HooksConfig hooksConfig = properties.getHooks();
        return hooksConfig != null ? hooksConfig.getTransformsDir() : null;
    }

    public Map<String, Object> buildAgentRequestFromMapping(String hookName, Map<String, Object> requestBody) {
        HooksConfig.HookMapping mapping = getMapping(hookName);
        if (mapping == null) {
            return null;
        }
        
        Map<String, Object> agentRequest = new HashMap<>();
        
        if (requestBody.containsKey("message")) {
            agentRequest.put("message", requestBody.get("message"));
        } else if (mapping.getMessage() != null) {
            agentRequest.put("message", mapping.getMessage());
        }
        
        if (requestBody.containsKey("name")) {
            agentRequest.put("name", requestBody.get("name"));
        } else if (mapping.getName() != null) {
            agentRequest.put("name", mapping.getName());
        }
        
        if (requestBody.containsKey("sessionKey")) {
            agentRequest.put("sessionKey", requestBody.get("sessionKey"));
        } else if (mapping.getSessionKey() != null) {
            agentRequest.put("sessionKey", mapping.getSessionKey());
        }
        
        if (requestBody.containsKey("wakeMode")) {
            agentRequest.put("wakeMode", requestBody.get("wakeMode"));
        } else if (mapping.getWakeMode() != null) {
            agentRequest.put("wakeMode", mapping.getWakeMode());
        } else {
            agentRequest.put("wakeMode", "now");
        }
        
        if (requestBody.containsKey("deliver")) {
            agentRequest.put("deliver", requestBody.get("deliver"));
        } else if (mapping.getDeliver() != null) {
            agentRequest.put("deliver", mapping.getDeliver());
        } else {
            agentRequest.put("deliver", true);
        }
        
        if (requestBody.containsKey("channel")) {
            agentRequest.put("channel", requestBody.get("channel"));
        } else if (mapping.getChannel() != null) {
            agentRequest.put("channel", mapping.getChannel());
        }
        
        if (requestBody.containsKey("to")) {
            agentRequest.put("to", requestBody.get("to"));
        } else if (mapping.getTo() != null) {
            agentRequest.put("to", mapping.getTo());
        }
        
        if (requestBody.containsKey("model")) {
            agentRequest.put("model", requestBody.get("model"));
        } else if (mapping.getModel() != null) {
            agentRequest.put("model", mapping.getModel());
        }
        
        if (requestBody.containsKey("thinking")) {
            agentRequest.put("thinking", requestBody.get("thinking"));
        } else if (mapping.getThinking() != null) {
            agentRequest.put("thinking", mapping.getThinking());
        }
        
        if (requestBody.containsKey("timeoutSeconds")) {
            agentRequest.put("timeoutSeconds", requestBody.get("timeoutSeconds"));
        } else if (mapping.getTimeoutSeconds() != null) {
            agentRequest.put("timeoutSeconds", mapping.getTimeoutSeconds());
        }
        
        return agentRequest;
    }

    public Map<String, Object> buildWakeRequestFromMapping(String hookName, Map<String, Object> requestBody) {
        HooksConfig.HookMapping mapping = getMapping(hookName);
        if (mapping == null) {
            return null;
        }
        
        Map<String, Object> wakeRequest = new HashMap<>();
        
        if (requestBody.containsKey("text")) {
            wakeRequest.put("text", requestBody.get("text"));
        }
        
        if (requestBody.containsKey("mode")) {
            wakeRequest.put("mode", requestBody.get("mode"));
        } else if (mapping.getWakeMode() != null) {
            wakeRequest.put("mode", mapping.getWakeMode());
        } else {
            wakeRequest.put("mode", "now");
        }
        
        return wakeRequest;
    }
}
