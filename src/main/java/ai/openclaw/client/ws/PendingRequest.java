package ai.openclaw.client.ws;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PendingRequest {
    
    private final String id;
    private final String message;
    private final String agentId;
    private final Boolean deliver;
    private final String sessionKey;
    private final long requestTimeoutMs;
    private final long resultTimeoutMs;
    private final CompletableFuture<AgentResult> future;
    
    private volatile String runId;
    private volatile long createdAt;
    
    public PendingRequest(
            String message,
            String agentId,
            Boolean deliver,
            String sessionKey,
            long requestTimeoutMs,
            long resultTimeoutMs,
            CompletableFuture<AgentResult> future) {
        this.id = java.util.UUID.randomUUID().toString();
        this.message = message;
        this.agentId = agentId;
        this.deliver = deliver;
        this.sessionKey = sessionKey;
        this.requestTimeoutMs = requestTimeoutMs;
        this.resultTimeoutMs = resultTimeoutMs;
        this.future = future;
        this.createdAt = System.currentTimeMillis();
    }
    
    public String getId() {
        return id;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getAgentId() {
        return agentId;
    }
    
    public Boolean getDeliver() {
        return deliver;
    }
    
    public String getSessionKey() {
        return sessionKey;
    }
    
    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }
    
    public long getResultTimeoutMs() {
        return resultTimeoutMs;
    }
    
    public CompletableFuture<AgentResult> getFuture() {
        return future;
    }
    
    public String getRunId() {
        return runId;
    }
    
    public void setRunId(String runId) {
        this.runId = runId;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public long getWaitTimeMs() {
        return System.currentTimeMillis() - createdAt;
    }
    
    public boolean isRequestTimeout() {
        return getWaitTimeMs() > requestTimeoutMs;
    }
    
    public boolean isResultTimeout() {
        return requestTimeoutMs > 0 && getWaitTimeMs() > requestTimeoutMs + resultTimeoutMs;
    }
    
    @Override
    public String toString() {
        return "PendingRequest{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                ", agentId='" + agentId + '\'' +
                ", runId='" + runId + '\'' +
                ", requestTimeoutMs=" + requestTimeoutMs +
                ", resultTimeoutMs=" + resultTimeoutMs +
                ", createdAt=" + createdAt +
                '}';
    }
}
