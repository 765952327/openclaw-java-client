package ai.openclaw.client.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AsyncAgentService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncAgentService.class);

    private final OpenClawWsClient wsClient;
    private final Map<String, AgentCallback> pendingCallbacks = new ConcurrentHashMap<>();

    public AsyncAgentService(OpenClawWsClient wsClient) {
        this.wsClient = wsClient;
        
        wsClient.addEventListener(new WsEventListener() {
            @Override
            public void onEvent(String event, Map<String, Object> payload) {
                if ("agent".equals(event)) {
                    handleAgentEvent(payload);
                } else if ("chat".equals(event)) {
                    handleChatEvent(payload);
                } else if ("presence".equals(event)) {
                    handlePresenceEvent(payload);
                } else if ("tick".equals(event)) {
                    handleTick();
                } else if ("shutdown".equals(event)) {
                    handleShutdown(payload);
                }
            }
        });
    }

    private void handleAgentEvent(Map<String, Object> payload) {
        String runId = (String) payload.get("runId");
        String status = (String) payload.get("status");
        
        if (runId == null) {
            return;
        }
        
        AgentCallback callback = pendingCallbacks.get(runId);
        if (callback == null) {
            return;
        }
        
        if ("accepted".equals(status)) {
            callback.onAccepted(runId);
        } else if ("ok".equals(status)) {
            AgentResult result = new AgentResult();
            result.setRunId(runId);
            result.setStatus(status);
            result.setSummary((String) payload.get("summary"));
            result.setError((String) payload.get("error"));
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> messages = (java.util.List<Map<String, Object>>) payload.get("messages");
            result.setMessages(messages);
            callback.onCompleted(result);
            pendingCallbacks.remove(runId);
        } else if ("error".equals(status)) {
            AgentResult result = new AgentResult();
            result.setRunId(runId);
            result.setStatus(status);
            result.setError((String) payload.get("error"));
            callback.onError(new IOException((String) payload.get("error")));
            pendingCallbacks.remove(runId);
        } else {
            callback.onProgress(payload);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleChatEvent(Map<String, Object> payload) {
        String runId = (String) payload.get("runId");
        String state = (String) payload.get("state");
        
        if (runId == null || !"final".equals(state)) {
            return;
        }
        
        AgentCallback callback = pendingCallbacks.get(runId);
        if (callback == null) {
            return;
        }
        
        Map<String, Object> message = (Map<String, Object>) payload.get("message");
        if (message != null) {
            String summary = extractTextFromMessage(message);
            if (summary != null) {
                AgentResult result = new AgentResult();
                result.setRunId(runId);
                result.setStatus("ok");
                result.setSummary(summary);
                
                logger.info("Agent completed via chat event, summary: {}", summary);
                callback.onCompleted(result);
                pendingCallbacks.remove(runId);
            }
        }
    }

    private String extractTextFromMessage(Map<String, Object> message) {
        if (message == null) {
            return null;
        }
        
        Object contentObj = message.get("content");
        if (contentObj instanceof java.util.List) {
            java.util.List<?> contentList = (java.util.List<?>) contentObj;
            for (Object item : contentList) {
                if (item instanceof Map) {
                    Map<?, ?> contentItem = (Map<?, ?>) item;
                    if ("text".equals(contentItem.get("type"))) {
                        return (String) contentItem.get("text");
                    }
                }
            }
        }
        
        String directText = (String) message.get("content");
        if (directText != null) {
            return directText;
        }
        
        return null;
    }

    private void handlePresenceEvent(Map<String, Object> payload) {
        logger.debug("Presence update: {}", payload);
    }

    private void handleTick() {
        logger.debug("Tick received");
    }

    private void handleShutdown(Map<String, Object> payload) {
        String reason = (String) payload.get("reason");
        Integer restartExpectedMs = payload.get("restartExpectedMs") != null 
            ? (Integer) payload.get("restartExpectedMs") 
            : null;
        logger.warn("Gateway shutdown: reason={}, restartExpectedMs={}", reason, restartExpectedMs);
    }

    public CompletableFuture<AgentResult> runAgentAsync(String message) {
        return runAgentAsync(message, null, null);
    }

    public CompletableFuture<AgentResult> runAgentAsync(String message, String agentId) {
        return runAgentAsync(message, agentId, null);
    }

    public CompletableFuture<AgentResult> runAgentAsync(String message, String agentId, Boolean deliver) {
        CompletableFuture<AgentResult> future = new CompletableFuture<>();
        
        AgentCallback callback = new AgentCallback() {
            @Override
            public void onAccepted(String runId) {
                logger.info("Agent accepted, runId: {}", runId);
            }

            @Override
            public void onProgress(Map<String, Object> payload) {
                logger.debug("Agent progress: {}", payload);
            }

            @Override
            public void onCompleted(AgentResult result) {
                logger.info("Agent completed: {}", result);
                future.complete(result);
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Agent error: {}", t.getMessage());
                future.completeExceptionally(t);
            }
        };
        
        try {
            WsResponse response = wsClient.sendAndWait("agent", buildParams(message, agentId, deliver), 30000);
            
            if (!Boolean.TRUE.equals(response.getOk())) {
                future.completeExceptionally(new IOException(response.getErrorMessage()));
                return future;
            }
            
            String runId = (String) response.getPayloadValue("runId");
            String status = (String) response.getPayloadValue("status");
            
            if (runId != null) {
                pendingCallbacks.put(runId, callback);
            }
            
            if ("accepted".equals(status)) {
                logger.info("Agent accepted, waiting for result...");
            } else if ("ok".equals(status) || "error".equals(status)) {
                AgentResult result = new AgentResult();
                result.setRunId(runId);
                result.setStatus(status);
                result.setSummary((String) response.getPayloadValue("summary"));
                result.setError((String) response.getPayloadValue("error"));
                future.complete(result);
            }
            
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }

    private Map<String, Object> buildParams(String message, String agentId, Boolean deliver) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("message", message);
        if (agentId != null) {
            params.put("agentId", agentId);
        }
        if (deliver != null) {
            params.put("deliver", deliver);
        }
        return params;
    }

    public void registerCallback(String runId, AgentCallback callback) {
        pendingCallbacks.put(runId, callback);
    }

    public void unregisterCallback(String runId) {
        pendingCallbacks.remove(runId);
    }

    public interface AgentCallback {
        void onAccepted(String runId);
        
        void onProgress(Map<String, Object> payload);
        
        void onCompleted(AgentResult result);
        
        void onError(Throwable t);
    }
}
