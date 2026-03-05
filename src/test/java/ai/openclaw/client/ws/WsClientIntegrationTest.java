package ai.openclaw.client.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class WsClientIntegrationTest {

    private OpenClawWsClient wsClient;
    private AsyncAgentService asyncAgentService;

    @Before
    public void setUp() {
        wsClient = new OpenClawWsClient("http://127.0.0.1:18789", "ollama");
        wsClient.setRequireDevice(false);
        asyncAgentService = new AsyncAgentService(wsClient);
    }

    @After
    public void tearDown() {
        if (wsClient != null) {
            wsClient.close();
        }
    }

    @Test
    public void testConnect() throws IOException {
        System.out.println("\n=== Testing WebSocket Connect ===");
        
        boolean connected = wsClient.connect();
        
        System.out.println("Connected: " + connected);
        System.out.println("Protocol version: " + wsClient.getProtocolVersion());
        
        assertTrue("Should connect successfully", connected);
        
        if (connected) {
            WsResponse health = wsClient.health();
            System.out.println("Health response ok: " + health.getOk());
            System.out.println("Health payload: " + health.getPayload());
        }
    }

    @Test
    public void testHealth() throws IOException {
        System.out.println("\n=== Testing Health ===");
        
        wsClient.connect();
        
        WsResponse health = wsClient.health();
        
        System.out.println("Ok: " + health.getOk());
        System.out.println("Payload: " + health.getPayload());
        
        assertNotNull(health);
    }

    @Test
    public void testStatus() throws IOException {
        System.out.println("\n=== Testing Status ===");
        
        wsClient.connect();
        
        WsResponse status = wsClient.status();
        
        System.out.println("Ok: " + status.getOk());
        System.out.println("Status payload: " + status.getPayload());
        
        assertNotNull(status);
    }

    @Test
    public void testRunAgentSync() throws IOException {
        System.out.println("\n=== Testing Sync Agent ===");
        
        boolean connected = wsClient.connect();
        System.out.println("Connected: " + connected);
        
        if (!connected) {
            System.out.println("Failed to connect, skipping test");
            return;
        }
        
        System.out.println("Running agent synchronously...");
        AgentResult result = wsClient.runAgent("What is 1+1?", "main");
        
        System.out.println("RunId: " + result.getRunId());
        System.out.println("Status: " + result.getStatus());
        System.out.println("Summary: " + result.getSummary());
        System.out.println("Error: " + result.getError());
        
        assertNotNull(result);
    }

    @Test
    public void testRunAgentAsync() throws Exception {
        System.out.println("\n=== Testing Async Agent ===");
        
        wsClient.connect();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<AgentResult> resultRef = new AtomicReference<>();
        
        asyncAgentService.runAgentAsync("Hello", "main").thenAccept(result -> {
            System.out.println("Async result received:");
            System.out.println("  RunId: " + result.getRunId());
            System.out.println("  Status: " + result.getStatus());
            System.out.println("  Summary: " + result.getSummary());
            resultRef.set(result);
            completed.set(true);
            latch.countDown();
        });
        
        boolean waitResult = latch.await(60, TimeUnit.SECONDS);
        
        assertTrue("Should complete within timeout", waitResult);
        assertTrue("Should complete successfully", completed.get());
    }

    @Test
    public void testEventListener() throws IOException {
        System.out.println("\n=== Testing Event Listener ===");
        
        CountDownLatch latch = new CountDownLatch(3);
        
        wsClient.addEventListener(new WsEventListener() {
            @Override
            public void onEvent(String event, java.util.Map<String, Object> payload) {
                System.out.println("Event: " + event + " - " + payload);
                latch.countDown();
            }
            
            @Override
            public void onAgentEvent(String runId, String status, java.util.Map<String, Object> payload) {
                System.out.println("Agent event - runId: " + runId + ", status: " + status);
            }
            
            @Override
            public void onTick() {
                System.out.println("Tick received");
            }
        });
        
        wsClient.connect();
        
        wsClient.runAgent("Test message for event listener", "main");
        
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertTrue("Should receive events", latch.getCount() < 3);
    }

    @Test
    public void testSendMessage() throws IOException {
        System.out.println("\n=== Testing Send Message ===");
        
        wsClient.connect();
        
        System.out.println("Sending message to channel...");
        
        try {
            WsResponse response = wsClient.sendMessage("test-channel", "Hello from Java WS client");
            System.out.println("Response ok: " + response.getOk());
            System.out.println("Response payload: " + response.getPayload());
        } catch (Exception e) {
            System.out.println("Send message not available or failed: " + e.getMessage());
        }
    }

    @Test
    public void testSystemEvent() throws IOException {
        System.out.println("\n=== Testing System Event ===");
        
        wsClient.connect();
        
        WsResponse response = wsClient.systemEvent("Test event from Java client");
        
        System.out.println("Response ok: " + response.getOk());
        
        assertNotNull(response);
    }

    @Test
    public void test() throws IOException {
        OpenClawWsClient client = new OpenClawWsClient("http://127.0.0.1:18789", "ollama");
        client.connect();

        AgentResult result = client.runAgent("今天天气怎么样", "main");
        System.out.println(result.getSummary());
    }
}
