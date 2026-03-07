package cn.welsione.client.exception;

public class OpenClawRequestException extends OpenClawException {

    private final String requestId;

    public OpenClawRequestException(OpenClawErrorCode errorCode) {
        super(errorCode);
        this.requestId = null;
    }

    public OpenClawRequestException(OpenClawErrorCode errorCode, String message) {
        super(errorCode, message);
        this.requestId = null;
    }

    public OpenClawRequestException(OpenClawErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.requestId = null;
    }

    public OpenClawRequestException(OpenClawErrorCode errorCode, String requestId, String message) {
        super(errorCode, message);
        this.requestId = requestId;
    }

    public OpenClawRequestException(OpenClawErrorCode errorCode, String requestId, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.requestId = requestId;
    }

    public OpenClawRequestException(String message) {
        super(OpenClawErrorCode.REQUEST_FAILED, message);
        this.requestId = null;
    }

    public OpenClawRequestException(String message, Throwable cause) {
        super(OpenClawErrorCode.REQUEST_FAILED, message, cause);
        this.requestId = null;
    }

    public String getRequestId() {
        return requestId;
    }
}
