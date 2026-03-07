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
}
