package cn.welsione.client.exception;

public class OpenClawConnectionException extends OpenClawException {

    public OpenClawConnectionException(OpenClawErrorCode errorCode) {
        super(errorCode);
    }

    public OpenClawConnectionException(OpenClawErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public OpenClawConnectionException(OpenClawErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public OpenClawConnectionException(OpenClawErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public OpenClawConnectionException(String message) {
        super(OpenClawErrorCode.CONNECTION_FAILED, message);
    }

    public OpenClawConnectionException(String message, Throwable cause) {
        super(OpenClawErrorCode.CONNECTION_FAILED, message, cause);
    }
}
