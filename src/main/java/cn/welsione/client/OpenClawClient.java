package cn.welsione.client;

import cn.welsione.client.config.OpenClawProperties;
import cn.welsione.client.http.OpenClawHttpClient;
import cn.welsione.client.model.AgentRequest;
import cn.welsione.client.model.OpenClawResponse;
import cn.welsione.client.model.WakeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class OpenClawClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenClawClient.class);

    private final OpenClawHttpClient httpClient;
    private final OpenClawProperties properties;

    public OpenClawClient(OpenClawProperties properties) {
        this.properties = properties;
        this.httpClient = new OpenClawHttpClient(properties);
        logger.info("OpenClawClient initialized with base URL: {}", properties.getBaseUrl());
    }

    public OpenClawClient(String baseUrl, String token) {
        this.properties = new OpenClawProperties();
        this.properties.setBaseUrl(baseUrl);
        this.properties.setToken(token);
        this.httpClient = new OpenClawHttpClient(properties);
        logger.info("OpenClawClient initialized with base URL: {}", baseUrl);
    }

    public OpenClawResponse wake(String text) {
        return wake(text, "now");
    }

    public OpenClawResponse wake(String text, String mode) {
        WakeRequest request = new WakeRequest(text, mode);
        return httpClient.post("/wake", request);
    }

    public OpenClawResponse sendMessage(String message) {
        return sendMessage(message, null);
    }

    public OpenClawResponse sendMessage(String message, String agentName) {
        AgentRequest request = AgentRequest.builder()
                .message(message)
                .name(agentName)
                .build();
        return httpClient.post("/agent", request);
    }

    public OpenClawResponse runAgent(String message) {
        return runAgent(message, null);
    }

    public OpenClawResponse runAgent(String message, String agentName) {
        return runAgent(message, agentName, null);
    }

    public OpenClawResponse runAgent(String message, String agentName, String sessionKey) {
        AgentRequest.Builder builder = AgentRequest.builder()
                .message(message)
                .name(agentName);

        if (sessionKey != null) {
            builder.sessionKey(sessionKey);
        }

        return httpClient.post("/agent", builder.build());
    }

    public OpenClawResponse runAgent(AgentRequest request) {
        return httpClient.post("/agent", request);
    }

    public OpenClawResponse runAgent(String message, String agentName, String sessionKey, 
                                     String model, String thinking, Integer timeoutSeconds,
                                     String channel, String to) {
        AgentRequest.Builder builder = AgentRequest.builder()
                .message(message)
                .name(agentName)
                .sessionKey(sessionKey)
                .model(model)
                .thinking(thinking)
                .timeoutSeconds(timeoutSeconds)
                .channel(channel)
                .to(to);

        return httpClient.post("/agent", builder.build());
    }

    public OpenClawResponse sendToCustomHook(String hookName, Map<String, Object> body) {
        return httpClient.post("/" + hookName, body);
    }

    public OpenClawResponse sendToCustomHook(String hookName, Object body) {
        return httpClient.post("/" + hookName, body);
    }

    public OpenClawResponse gmail(String source, List<Map<String, Object>> messages) {
        Map<String, Object> body = Map.of(
                "source", source,
                "messages", messages
        );
        return httpClient.post("/gmail", body);
    }

    public void close() {
        httpClient.close();
    }

    public OpenClawProperties getProperties() {
        return properties;
    }
}
