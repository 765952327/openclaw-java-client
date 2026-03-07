package cn.welsione.client.exception;

public class OpenClawException extends RuntimeException {

    private final OpenClawErrorCode errorCode;
    private final String details;

    public OpenClawException(OpenClawErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public OpenClawException(OpenClawErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = message;
    }

    public OpenClawException(OpenClawErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    public OpenClawException(OpenClawErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = message;
    }

    public OpenClawException(String message) {
        super(message);
        this.errorCode = OpenClawErrorCode.UNKNOWN;
        this.details = message;
    }

    public OpenClawException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = OpenClawErrorCode.UNKNOWN;
        this.details = message;
    }

    public OpenClawErrorCode getErrorCode() {
        return errorCode;
    }

    public String getCode() {
        return errorCode.getCode();
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        if (details != null) {
            return String.format("[%s] %s: %s", errorCode.getCode(), errorCode.getMessage(), details);
        }
        return String.format("[%s] %s", errorCode.getCode(), errorCode.getMessage());
    }
}
