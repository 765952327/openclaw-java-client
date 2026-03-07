package cn.welsione.client.exception;

public enum OpenClawErrorCode {

    CONNECTION_FAILED("C001", "连接失败"),
    CONNECTION_CLOSED("C002", "连接已关闭"),
    CONNECTION_TIMEOUT("C003", "连接超时"),
    RECONNECT_FAILED("C004", "重连失败"),
    RECONNECT_EXHAUSTED("C005", "重连次数耗尽"),

    WEBSOCKET_ERROR("W001", "WebSocket 错误"),
    WEBSOCKET_NOT_CONNECTED("W002", "WebSocket 未连接"),
    WEBSOCKET_SEND_FAILED("W003", "发送消息失败"),

    REQUEST_TIMEOUT("R001", "请求超时"),
    REQUEST_QUEUE_FULL("R002", "请求队列已满"),
    REQUEST_CANCELLED("R003", "请求已取消"),
    REQUEST_FAILED("R004", "请求失败"),

    AGENT_NOT_FOUND("A001", "Agent 不存在"),
    AGENT_EXECUTION_FAILED("A002", "Agent 执行失败"),
    AGENT_TIMEOUT("A003", "Agent 执行超时"),
    AGENT_REJECTED("A004", "Agent 拒绝执行"),

    INVALID_RESPONSE("V001", "无效的响应"),
    PARSE_ERROR("V002", "解析响应失败"),

    UNKNOWN("U001", "未知错误");

    private final String code;
    private final String message;

    OpenClawErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static OpenClawErrorCode fromCode(String code) {
        for (OpenClawErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return UNKNOWN;
    }
}
