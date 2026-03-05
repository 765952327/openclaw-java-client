package ai.openclaw.client;

import ai.openclaw.client.config.HooksConfig;
import ai.openclaw.client.config.OpenClawProperties;
import ai.openclaw.client.model.AgentRequest;
import ai.openclaw.client.model.OpenClawResponse;
import ai.openclaw.client.service.WebhookService;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class WebhookIntegrationTest {

    private OpenClawClient client;
    private OpenClawProperties properties;
    private WebhookService webhookService;

    @Before
    public void setUp() {
        properties = new OpenClawProperties();
        properties.setBaseUrl("http://127.0.0.1:18789");
        properties.setToken("gmail_hook_2026");
        properties.setHooksPath("/hooks");

        HooksConfig hooksConfig = new HooksConfig();
        hooksConfig.setEnabled(true);
        hooksConfig.setToken("gmail_hook_2026");
        hooksConfig.setPath("/hooks");
        properties.setHooks(hooksConfig);

        client = new OpenClawClient(properties);
        webhookService = new WebhookService(properties);
    }

    @Test
    public void testWakeEndpoint() {
        System.out.println("\n=== Testing /hooks/wake ===");
        OpenClawResponse response = client.wake("Test event from Java client");
        
        System.out.println("Success: " + response.isSuccess());
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Body: " + response.getBody());
        
        assertNotNull(response);
    }

    @Test
    public void testWakeWithMode() {
        System.out.println("\n=== Testing /hooks/wake with mode ===");
        OpenClawResponse response = client.wake("Test event", "next-heartbeat");
        
        System.out.println("Success: " + response.isSuccess());
        System.out.println("Status: " + response.getStatusCode());
        
        assertNotNull(response);
    }

    @Test
    public void testAgentEndpoint() {
        System.out.println("\n=== Testing /hooks/agent ===");
        OpenClawResponse response = client.runAgent("Hello from Java client test", "TestAgent");
        
        System.out.println("Success: " + response.isSuccess());
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Body: " + response.getBody());
        
        assertNotNull(response);
    }

    @Test
    public void testAgentWithFullOptions() {
        System.out.println("\n=== Testing /hooks/agent with full options ===");
        AgentRequest request = AgentRequest.builder()
                .message("Summarize my day")
                .name("DailySummary")
                .wakeMode("now")
                .deliver(true)
                .model("openai/gpt-5.2-mini")
                .thinking("low")
                .timeoutSeconds(60)
                .build();

        OpenClawResponse response = client.runAgent(request);
        
        System.out.println("Success: " + response.isSuccess());
        System.out.println("Status: " + response.getStatusCode());
        
        assertNotNull(response);
    }

    @Test
    public void testWebhookService() {
        System.out.println("\n=== Testing WebhookService ===");
        
        assertTrue(webhookService.isEnabled());
        assertTrue(webhookService.validateToken("gmail_hook_2026"));
        assertFalse(webhookService.validateToken("wrong-token"));
        
        System.out.println("isEnabled: " + webhookService.isEnabled());
        System.out.println("validateToken(valid): " + webhookService.validateToken("gmail_hook_2026"));
        System.out.println("validateToken(invalid): " + webhookService.validateToken("wrong-token"));
    }

    @Test
    public void testCustomHookNotConfigured() {
        System.out.println("\n=== Testing custom hook (not configured - expected 404) ===");
        
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Custom webhook test");
        body.put("name", "CustomHook");
        
        OpenClawResponse response = client.sendToCustomHook("test", body);
        
        System.out.println("Success: " + response.isSuccess());
        System.out.println("Status: " + response.getStatusCode());
        
        assertNotNull(response);
    }

    @Test
    public void testGmailHook() {
        System.out.println("\n=== Testing /hooks/gmail ===");
        
        Map<String, Object> message1 = new HashMap<>();
        message1.put("from", "test@example.com");
        message1.put("subject", "Test Email");
        message1.put("snippet", "This is a test email");
        
        OpenClawResponse response = client.gmail("gmail", java.util.List.of(message1));
        
        System.out.println("Success: " + response.isSuccess());
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Body: " + response.getBody());
        
        assertNotNull(response);
    }

    @Test
    public void testProperties() {
        System.out.println("\n=== Testing Properties ===");
        
        assertEquals("http://127.0.0.1:18789", properties.getBaseUrl());
        assertEquals("gmail_hook_2026", properties.getToken());
        assertEquals("/hooks", properties.getHooksPath());
        assertTrue(properties.isHooksEnabled());
        assertEquals("gmail_hook_2026", properties.getHooksToken());
        assertEquals("http://127.0.0.1:18789/hooks/wake", properties.getFullWebhookUrl("/wake"));
        
        System.out.println("Base URL: " + properties.getBaseUrl());
        System.out.println("Hooks enabled: " + properties.isHooksEnabled());
        System.out.println("Full webhook URL: " + properties.getFullWebhookUrl("/wake"));
    }
}
