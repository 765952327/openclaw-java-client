package ai.openclaw.client.ws;

import java.util.Map;

public interface AgentStreamCallback {

    void onStart(String runId, Map<String, Object> metadata);

    void onOutput(String runId, String text, Map<String, Object> data);

    void onError(String runId, String error);

    void onComplete(String runId, String summary, Map<String, Object> result);

    default void onToken(String runId, String token) {
    }

    default void onThinking(String runId, String thought) {
    }
}
