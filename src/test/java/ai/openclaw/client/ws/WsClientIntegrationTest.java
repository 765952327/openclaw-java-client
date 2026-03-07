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
        assertEquals("ok", result.getStatus());
    }

    @Test
    public void testRunAgentWithUid() throws IOException {
        System.out.println("\n=== Testing Agent with UID ===");
        
        wsClient.connect();
        
        String uid = "test-user-001";
        AgentResult result = wsClient.runAgentWithUid(uid, "What is 2+2?", "main");
        
        System.out.println("UID: " + result.getUid());
        System.out.println("RunId: " + result.getRunId());
        System.out.println("Status: " + result.getStatus());
        System.out.println("Summary: " + result.getSummary());
        
        assertEquals(uid, result.getUid());
        assertEquals("ok", result.getStatus());
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
    public void testRunAgentAsyncWithUid() throws Exception {
        System.out.println("\n=== Testing Async Agent with UID ===");
        
        wsClient.connect();
        
        String uid = "async-test-002";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AgentResult> resultRef = new AtomicReference<>();
        
        asyncAgentService.runAgentAsync(uid, "Hello with UID", "main", null, null, null)
            .thenAccept(result -> {
                System.out.println("Async result with UID:");
                System.out.println("  UID: " + result.getUid());
                System.out.println("  RunId: " + result.getRunId());
                System.out.println("  Summary: " + result.getSummary());
                resultRef.set(result);
                latch.countDown();
            });
        
        latch.await(60, TimeUnit.SECONDS);
        
        assertEquals(uid, resultRef.get().getUid());
    }

    @Test
    public void testQueueConfiguration() throws IOException {
        System.out.println("\n=== Testing Queue Configuration ===");
        
        int customCapacity = 100;
        OpenClawWsClient customClient = new OpenClawWsClient(
            "http://127.0.0.1:18789", "ollama",
            customCapacity, 30000, 120000
        );
        customClient.setRequireDevice(false);
        
        System.out.println("Max queue capacity: " + customClient.getMaxQueueCapacity());
        System.out.println("Is queue full: " + customClient.isQueueFull());
        
        assertEquals(customCapacity, customClient.getMaxQueueCapacity());
        
        customClient.close();
    }

    @Test
    public void testReconnectConfiguration() throws IOException {
        System.out.println("\n=== Testing Reconnect Configuration ===");
        
        OpenClawWsClient customClient = new OpenClawWsClient(
            "http://127.0.0.1:18789", "ollama",
            500, 30000, 120000,
            true, 5, 2000, 10000,
            false, 30000, 10000,
            false, 3, 500, 5000,
            true, true, null, 0
        );
        customClient.setRequireDevice(false);
        
        System.out.println("Auto reconnect: " + customClient.isAutoReconnect());
        System.out.println("Max retries: " + customClient.getMaxReconnectRetries());
        System.out.println("Current attempt: " + customClient.getCurrentReconnectAttempt());
        System.out.println("Is reconnecting: " + customClient.isReconnecting());
        
        assertTrue(customClient.isAutoReconnect());
        assertEquals(5, customClient.getMaxReconnectRetries());
        
        customClient.close();
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
    public void testChatEventListener() throws IOException, InterruptedException {
        System.out.println("\n=== Testing Chat Event Listener ===");
        
        CountDownLatch latch = new CountDownLatch(1);
        
        wsClient.addEventListener(new WsEventListener() {
            @Override
            public void onChatEvent(String runId, String sessionKey, String state, java.util.Map<String, Object> message) {
                System.out.println("Chat event - runId: " + runId);
                System.out.println("  sessionKey: " + sessionKey);
                System.out.println("  state: " + state);
                System.out.println("  message: " + message);
                latch.countDown();
            }
        });
        
        wsClient.connect();
        wsClient.runAgent("Hello for chat event test", "main");
        
        latch.await(30, TimeUnit.SECONDS);
        
        assertEquals("Should receive chat event", 0, latch.getCount());
    }

    @Test
    public void testPresenceUpdateListener() throws IOException, InterruptedException {
        System.out.println("\n=== Testing Presence Update Listener ===");
        
        CountDownLatch latch = new CountDownLatch(1);
        
        wsClient.addEventListener(new WsEventListener() {
            @Override
            public void onPresenceUpdate(java.util.Map<String, Object> presence) {
                System.out.println("Presence update: " + presence);
                latch.countDown();
            }
        });
        
        wsClient.connect();
        
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Presence latch count: " + latch.getCount());
    }

    @Test
    public void testReconnectListener() throws IOException, InterruptedException {
        System.out.println("\n=== Testing Reconnect Listener ===");
        
        CountDownLatch reconnectLatch = new CountDownLatch(1);
        CountDownLatch failLatch = new CountDownLatch(1);
        
        wsClient.addEventListener(new WsEventListener() {
            @Override
            public void onReconnected() {
                System.out.println("Reconnected successfully!");
                reconnectLatch.countDown();
            }
            
            @Override
            public void onReconnectFailed(Throwable t) {
                System.out.println("Reconnect failed: " + t.getMessage());
                failLatch.countDown();
            }
        });
        
        wsClient.connect();
        
        System.out.println("Connected, waiting for events...");
        
        Thread.sleep(5000);
        
        System.out.println("Reconnect test completed");
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
    public void testMultipleAgents() throws IOException {
        System.out.println("\n=== Testing Multiple Agents ===");
        
        wsClient.connect();
        
        System.out.println("Running agent 1...");
        AgentResult result1 = wsClient.runAgent("Say hello", "main");
        System.out.println("Result 1: " + result1.getSummary());
        
        System.out.println("Running agent 2...");
        AgentResult result2 = wsClient.runAgent("Say goodbye", "main");
        System.out.println("Result 2: " + result2.getSummary());
        
        System.out.println("Running agent 3...");
        AgentResult result3 = wsClient.runAgent("What is 3+3?", "main");
        System.out.println("Result 3: " + result3.getSummary());
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertEquals("ok", result1.getStatus());
        assertEquals("ok", result2.getStatus());
        assertEquals("ok", result3.getStatus());
    }

    @Test
    public void testQueueSize() throws IOException {
        System.out.println("\n=== Testing Queue Size ===");
        
        wsClient.connect();
        
        System.out.println("Initial queue size: " + wsClient.getQueueSize());
        
        wsClient.runAgent("Test 1", "main");
        System.out.println("After request queue size: " + wsClient.getQueueSize());
        
        assertTrue(wsClient.getQueueSize() >= 0);
    }

    @Test
    public void testIsConnected() throws IOException {
        System.out.println("\n=== Testing isConnected ===");
        
        System.out.println("Before connect: " + wsClient.isConnected());
        
        wsClient.connect();
        
        System.out.println("After connect: " + wsClient.isConnected());
        
        assertTrue(wsClient.isConnected());
        
        wsClient.close();
        
        System.out.println("After close: " + wsClient.isConnected());
        
        assertFalse(wsClient.isConnected());
    }

    @Test
    public void testAgentResultMethods() throws IOException {
        System.out.println("\n=== Testing AgentResult Methods ===");
        
        wsClient.connect();
        
        AgentResult result = wsClient.runAgent("What is 5+5?", "main");
        
        System.out.println("getUid(): " + result.getUid());
        System.out.println("getRunId(): " + result.getRunId());
        System.out.println("getStatus(): " + result.getStatus());
        System.out.println("getSummary(): " + result.getSummary());
        System.out.println("getError(): " + result.getError());
        
        System.out.println("isAccepted(): " + result.isAccepted());
        System.out.println("isOk(): " + result.isOk());
        System.out.println("isError(): " + result.isError());
        
        assertTrue(result.isOk());
        assertFalse(result.isError());
        assertFalse(result.isAccepted());
    }

    @Test
    public void testHealthCheckConfiguration() throws IOException {
        System.out.println("\n=== Testing Health Check Configuration ===");
        
        OpenClawWsClient healthCheckClient = new OpenClawWsClient(
            "http://127.0.0.1:18789", "ollama",
            500, 30000, 120000,
            true, 5, 2000, 10000,
            true, 5000, 3000,
            true, 3, 500, 5000,
            true, true, null, 0
        );
        healthCheckClient.setRequireDevice(false);
        
        System.out.println("Health check enabled: " + healthCheckClient.isHealthCheckEnabled());
        System.out.println("Health check interval: " + healthCheckClient.getHealthCheckIntervalMs() + "ms");
        System.out.println("Health check timeout: " + healthCheckClient.getHealthCheckTimeoutMs() + "ms");
        
        assertTrue(healthCheckClient.isHealthCheckEnabled());
        assertEquals(5000, healthCheckClient.getHealthCheckIntervalMs());
        assertEquals(3000, healthCheckClient.getHealthCheckTimeoutMs());
        
        healthCheckClient.close();
    }

    @Test
    public void testHealthCheckDisabledConfiguration() throws IOException {
        System.out.println("\n=== Testing Health Check Disabled ===");
        
        OpenClawWsClient disabledClient = new OpenClawWsClient(
            "http://127.0.0.1:18789", "ollama",
            500, 30000, 120000,
            true, 5, 2000, 10000,
            false, 5000, 3000,
            false, 3, 500, 5000,
            true, true, null, 0
        );
        disabledClient.setRequireDevice(false);
        
        System.out.println("Health check enabled: " + disabledClient.isHealthCheckEnabled());
        
        assertFalse(disabledClient.isHealthCheckEnabled());
        
        disabledClient.close();
    }

    @Test
    public void testHealthCheckGetters() throws IOException, InterruptedException {
        System.out.println("\n=== Testing Health Check Getters ===");
        
        OpenClawWsClient client = new OpenClawWsClient(
            "http://127.0.0.1:18789", "ollama",
            500, 30000, 120000,
            true, 5, 2000, 10000,
            true, 10000, 5000,
            true, 3, 500, 5000,
            true, true, null, 0
        );
        client.setRequireDevice(false);
        
        System.out.println("Initial last health check time: " + client.getLastHealthCheckTime());
        System.out.println("Initial last health check result: " + client.getLastHealthCheckResult());
        System.out.println("Health check running: " + client.isHealthCheckRunning());
        
        assertEquals(0, client.getLastHealthCheckTime());
        assertFalse(client.getLastHealthCheckResult());
        
        client.connect();
        
        Thread.sleep(15000);
        
        System.out.println("After connect - last health check time: " + client.getLastHealthCheckTime());
        System.out.println("After connect - last health check result: " + client.getLastHealthCheckResult());
        System.out.println("After connect - health check running: " + client.isHealthCheckRunning());
        
        assertTrue(client.getLastHealthCheckTime() > 0);
        assertTrue(client.isHealthCheckRunning());
        
        client.close();
    }

    @Test
    public void testHealthCheckListener() throws IOException, InterruptedException {
        System.out.println("\n=== Testing Health Check Listener ===");
        
        CountDownLatch healthCheckLatch = new CountDownLatch(2);
        AtomicBoolean healthyResult = new AtomicBoolean(false);
        
        OpenClawWsClient healthCheckClient = new OpenClawWsClient(
            "http://127.0.0.1:18789", "ollama",
            500, 30000, 120000,
            true, 5, 2000, 10000,
            true, 5000, 3000,
            true, 3, 500, 5000,
            true, true, null, 0
        );
        healthCheckClient.setRequireDevice(false);
        
        healthCheckClient.addEventListener(new WsEventListener() {
            @Override
            public void onHealthCheck(boolean healthy, Throwable error) {
                System.out.println("Health check result - healthy: " + healthy + ", error: " + (error != null ? error.getMessage() : "none"));
                healthyResult.set(healthy);
                healthCheckLatch.countDown();
            }
        });
        
        healthCheckClient.connect();
        
        boolean received = healthCheckLatch.await(20, TimeUnit.SECONDS);
        System.out.println("Received health check events: " + (2 - healthCheckLatch.getCount()));
        System.out.println("Final healthy result: " + healthyResult.get());
        
        assertTrue("Should receive health check events", received);
        
        healthCheckClient.close();
    }

    @Test
    public void testStreamAgent() throws Exception {
        System.out.println("\n=== Testing Stream Agent ===");
        
        CountDownLatch outputLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(1);
        StringBuilder outputBuilder = new StringBuilder();
        
        wsClient.connect();
        
        wsClient.runAgentStream("Say hello in 3 words", "main", new AgentStreamCallback() {
            @Override
            public void onStart(String runId, java.util.Map<String, Object> metadata) {
                System.out.println("Agent started, runId: " + runId);
            }

            @Override
            public void onOutput(String runId, String text, java.util.Map<String, Object> data) {
                System.out.println("Agent output: " + text);
                outputBuilder.append(text);
                outputLatch.countDown();
            }

            @Override
            public void onError(String runId, String error) {
                System.out.println("Agent error: " + error);
            }

            @Override
            public void onComplete(String runId, String summary, java.util.Map<String, Object> result) {
                System.out.println("Agent complete, summary: " + summary);
                completeLatch.countDown();
            }

            @Override
            public void onThinking(String runId, String thought) {
                System.out.println("Agent thinking: " + thought);
            }
        });
        
        boolean outputReceived = outputLatch.await(30, TimeUnit.SECONDS);
        boolean completeReceived = completeLatch.await(60, TimeUnit.SECONDS);
        
        System.out.println("Output received: " + outputReceived);
        System.out.println("Output: " + outputBuilder.toString());
        System.out.println("Complete received: " + completeReceived);
    }
}
