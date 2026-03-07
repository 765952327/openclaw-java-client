package cn.welsione.client.exception;

public class OpenClawTimeoutException extends OpenClawException {

    private final long timeoutMs;

    public OpenClawTimeoutException(OpenClawErrorCode errorCode) {
        super(errorCode);
        this.timeoutMs = 0;
    }

    public OpenClawTimeoutException(OpenClawErrorCode errorCode, long timeoutMs) {
        super(errorCode, errorCode.getMessage() + " (timeout: " + timeoutMs + "ms)");
        this.timeoutMs = timeoutMs;
    }

    public OpenClawTimeoutException(OpenClawErrorCode errorCode, String message, long timeoutMs) {
        super(errorCode, message);
        this.timeoutMs = timeoutMs;
    }

    public OpenClawTimeoutException(String message, long timeoutMs) {
        super(OpenClawErrorCode.REQUEST_TIMEOUT, message);
        this.timeoutMs = timeoutMs;
    }

    public OpenClawTimeoutException(String message, long timeoutMs, Throwable cause) {
        super(OpenClawErrorCode.REQUEST_TIMEOUT, message, cause);
        this.timeoutMs = timeoutMs;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }
}
