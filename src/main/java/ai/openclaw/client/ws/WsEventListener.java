package ai.openclaw.client.ws;

import java.util.Map;

public interface WsEventListener {

    default void onEvent(String event, Map<String, Object> payload) {
    }

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

    default void onReconnected() {
    }

    default void onReconnectFailed(Throwable t) {
    }

    /**
     * 健康检查结果回调
     * 
     * @param healthy 是否健康
     * @param error   错误信息（如果检查失败）
     */
    default void onHealthCheck(boolean healthy, Throwable error) {
    }

    default void onAgentStart(String runId, Map<String, Object> metadata) {
    }

    default void onAgentOutput(String runId, String text, Map<String, Object> data) {
    }

    default void onAgentThinking(String runId, String thought) {
    }

    default void onAgentToken(String runId, String token) {
    }

    default void onAgentComplete(String runId, String summary, Map<String, Object> result) {
    }

    default void onAgentError(String runId, String error) {
    }
}
