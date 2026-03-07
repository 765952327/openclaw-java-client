package ai.openclaw.client.exception;

import org.junit.Test;

import static org.junit.Assert.*;

public class OpenClawExceptionTest {

    @Test
    public void testOpenClawException() {
        OpenClawException ex = new OpenClawException("Test error");
        
        assertEquals(OpenClawErrorCode.UNKNOWN, ex.getErrorCode());
        assertEquals("Test error", ex.getMessage());
    }

    @Test
    public void testOpenClawExceptionWithCode() {
        OpenClawException ex = new OpenClawException(OpenClawErrorCode.CONNECTION_FAILED, "Connection failed");
        
        assertEquals(OpenClawErrorCode.CONNECTION_FAILED, ex.getErrorCode());
        assertEquals("C001", ex.getCode());
    }

    @Test
    public void testConnectionException() {
        OpenClawConnectionException ex = new OpenClawConnectionException("Connection error");
        
        assertEquals(OpenClawErrorCode.CONNECTION_FAILED, ex.getErrorCode());
    }

    @Test
    public void testRequestException() {
        OpenClawRequestException ex = new OpenClawRequestException("Request failed");
        
        assertEquals(OpenClawErrorCode.REQUEST_FAILED, ex.getErrorCode());
    }

    @Test
    public void testTimeoutException() {
        OpenClawTimeoutException ex = new OpenClawTimeoutException("Timeout", 5000);
        
        assertEquals(OpenClawErrorCode.REQUEST_TIMEOUT, ex.getErrorCode());
        assertEquals(5000, ex.getTimeoutMs());
    }

    @Test
    public void testAgentException() {
        OpenClawAgentException ex = new OpenClawAgentException(OpenClawErrorCode.AGENT_EXECUTION_FAILED, "run-123", "agent-1", "Agent error");
        
        assertEquals(OpenClawErrorCode.AGENT_EXECUTION_FAILED, ex.getErrorCode());
        assertEquals("run-123", ex.getRunId());
        assertEquals("agent-1", ex.getAgentId());
    }

    @Test
    public void testErrorCodeFromCode() {
        OpenClawErrorCode code = OpenClawErrorCode.fromCode("C001");
        assertEquals(OpenClawErrorCode.CONNECTION_FAILED, code);
    }

    @Test
    public void testErrorCodeUnknown() {
        OpenClawErrorCode code = OpenClawErrorCode.fromCode("UNKNOWN");
        assertEquals(OpenClawErrorCode.UNKNOWN, code);
    }
}
