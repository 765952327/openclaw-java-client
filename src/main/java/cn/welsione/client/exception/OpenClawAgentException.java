package cn.welsione.client.exception;

public class OpenClawAgentException extends OpenClawException {

    private final String runId;
    private final String agentId;

    public OpenClawAgentException(OpenClawErrorCode errorCode) {
        super(errorCode);
        this.runId = null;
        this.agentId = null;
    }

    public OpenClawAgentException(OpenClawErrorCode errorCode, String message) {
        super(errorCode, message);
        this.runId = null;
        this.agentId = null;
    }

    public OpenClawAgentException(OpenClawErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
        this.runId = null;
        this.agentId = null;
    }

    public OpenClawAgentException(OpenClawErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.runId = null;
        this.agentId = null;
    }

    public OpenClawAgentException(OpenClawErrorCode errorCode, String runId, String agentId, String message) {
        super(errorCode, message);
        this.runId = runId;
        this.agentId = agentId;
    }

    public OpenClawAgentException(OpenClawErrorCode errorCode, String runId, String agentId, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.runId = runId;
        this.agentId = agentId;
    }

    public OpenClawAgentException(String message) {
        super(OpenClawErrorCode.AGENT_EXECUTION_FAILED, message);
        this.runId = null;
        this.agentId = null;
    }

    public OpenClawAgentException(String message, Throwable cause) {
        super(OpenClawErrorCode.AGENT_EXECUTION_FAILED, message, cause);
        this.runId = null;
        this.agentId = null;
    }

    public String getRunId() {
        return runId;
    }

    public String getAgentId() {
        return agentId;
    }
}
