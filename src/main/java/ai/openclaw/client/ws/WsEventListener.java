package ai.openclaw.client.ws;

import java.util.Map;

public interface WsEventListener {

    void onEvent(String event, Map<String, Object> payload);

    default void onError(Throwable t) {
    }

    default void onAgentEvent(String runId, String status, Map<String, Object> payload) {
    }

    default void onChatEvent(String runId, String sessionKey, String state, Map<String, Object> message) {
    }

    default void onPresenceUpdate(Map<String, Object> presence) {
    }

    default void onTick() {
    }

    default void onShutdown(String reason, Integer restartExpectedMs) {
    }
}
