package ai.openclaw.client;

import ai.openclaw.client.model.AgentRequest;
import ai.openclaw.client.model.OpenClawResponse;
import ai.openclaw.client.model.WakeRequest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class OpenClawClientTest {

    private OpenClawClient client;

    @Before
    public void setUp() {
        client = new OpenClawClient(
                "http://127.0.0.1:18789",
                "test-token"
        );
    }

    @Test
    public void testWakeRequest() {
        WakeRequest request = new WakeRequest("Test event");
        assertEquals("Test event", request.getText());
        assertEquals("now", request.getMode());

        WakeRequest requestWithMode = new WakeRequest("Test event", "next-heartbeat");
        assertEquals("next-heartbeat", requestWithMode.getMode());
    }

    @Test
    public void testAgentRequestBuilder() {
        AgentRequest request = AgentRequest.builder()
                .message("Test message")
                .name("TestAgent")
                .sessionKey("test-session")
                .model("openai/gpt-5.2-mini")
                .thinking("low")
                .timeoutSeconds(60)
                .build();

        assertEquals("Test message", request.getMessage());
        assertEquals("TestAgent", request.getName());
        assertEquals("test-session", request.getSessionKey());
        assertEquals("openai/gpt-5.2-mini", request.getModel());
        assertEquals("low", request.getThinking());
        assertEquals(Integer.valueOf(60), request.getTimeoutSeconds());
    }

    @Test
    public void testClientConfiguration() {
        assertEquals("http://127.0.0.1:18789", client.getProperties().getBaseUrl());
        assertEquals("test-token", client.getProperties().getToken());
    }

    @Test
    public void testOpenClawResponse() {
        OpenClawResponse response = new OpenClawResponse(true, 200, "Success body");
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertEquals("Success body", response.getBody());

        OpenClawResponse errorResponse = new OpenClawResponse(false, 401, "Error", "Unauthorized");
        assertFalse(errorResponse.isSuccess());
        assertEquals(401, errorResponse.getStatusCode());
        assertEquals("Error", errorResponse.getMessage());
    }
}
