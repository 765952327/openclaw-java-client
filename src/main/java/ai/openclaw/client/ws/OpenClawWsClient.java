package ai.openclaw.client.ws;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class OpenClawWsClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenClawWsClient.class);

    private static final long DEFAULT_REQUEST_TIMEOUT_MS = 60000;
    private static final long DEFAULT_RESULT_TIMEOUT_MS = 300000;
    private static final int DEFAULT_MAX_QUEUE_CAPACITY = 500;

    private final String baseUrl;
    private final String token;
    private final ObjectMapper objectMapper;
    
    private WebSocket webSocket;
    private OkHttpClient httpClient;
    private volatile boolean connected = false;
    private volatile boolean connecting = false;
    
    private final Map<String, WsResponse> pendingRequests = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<WsEventListener> eventListeners = new CopyOnWriteArrayList<>();
    private CountDownLatch connectLatch;
    
    private int protocolVersion;
    private String deviceToken;
    private boolean requireDevice = true;
    
    private final int maxQueueCapacity;
    private final long defaultRequestTimeoutMs;
    private final long defaultResultTimeoutMs;
    private final BlockingQueue<PendingRequest> requestQueue;
    private final ExecutorService consumerExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public OpenClawWsClient(String baseUrl, String token) {
        this(baseUrl, token, DEFAULT_MAX_QUEUE_CAPACITY, DEFAULT_REQUEST_TIMEOUT_MS, DEFAULT_RESULT_TIMEOUT_MS);
    }

    public OpenClawWsClient(String baseUrl, String token, int maxQueueCapacity, 
            long defaultRequestTimeoutMs, long defaultResultTimeoutMs) {
        this.baseUrl = baseUrl.replace("http", "ws") + "/ws";
        this.token = token;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.maxQueueCapacity = maxQueueCapacity;
        this.defaultRequestTimeoutMs = defaultRequestTimeoutMs;
        this.defaultResultTimeoutMs = defaultResultTimeoutMs;
        
        this.requestQueue = new LinkedBlockingQueue<>(maxQueueCapacity);
        this.consumerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "openclaw-ws-consumer");
            t.setDaemon(true);
            return t;
        });
        
        this.httpClient = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build();
    }

    public boolean connect() throws IOException {
        if (connected) {
            return true;
        }

        connectLatch = new CountDownLatch(1);
        
        String wsUrl = baseUrl.startsWith("ws") ? baseUrl : baseUrl.replace("http", "ws");
        if (!wsUrl.contains("/ws")) {
            wsUrl = wsUrl + "/ws";
        }

        Request request = new Request.Builder()
                .url(wsUrl)
                .build();

        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                logger.info("WebSocket connected, sending connect request...");
                connecting = true;
                sendConnect();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleMessage(text);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                logger.info("WebSocket closing: {} - {}", code, reason);
                connected = false;
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                logger.info("WebSocket closed: {} - {}", code, reason);
                connected = false;
                if (connectLatch != null) {
                    connectLatch.countDown();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                logger.error("WebSocket failure: {}", t.getMessage());
                connected = false;
                notifyError(t);
                if (connectLatch != null) {
                    connectLatch.countDown();
                }
            }
        });

        try {
            connectLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("Connect completed, connected: {}", connected);
        
        if (connected) {
            startConsumer();
        }
        
        return connected;
    }

    private void startConsumer() {
        if (running.compareAndSet(false, true)) {
            consumerExecutor.submit(this::consumerLoop);
            logger.info("Request consumer started");
        }
    }

    private void consumerLoop() {
        logger.info("Consumer loop started");
        
        while (running.get()) {
            try {
                PendingRequest request = requestQueue.poll(1, TimeUnit.SECONDS);
                
                if (request == null) {
                    continue;
                }
                
                if (!connected) {
                    request.getFuture().completeExceptionally(
                        new IOException("WebSocket not connected")
                    );
                    continue;
                }
                
                processRequest(request);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Consumer loop error: {}", e.getMessage());
            }
        }
        
        logger.info("Consumer loop stopped");
    }

    private void processRequest(PendingRequest request) {
        try {
            pendingRequests.entrySet().removeIf(entry -> 
                entry.getKey().startsWith("agent:") || entry.getKey().startsWith("chat:")
            );
            
            Map<String, Object> params = new HashMap<>();
            String idempotencyKey = UUID.randomUUID().toString();
            params.put("idempotencyKey", idempotencyKey);
            params.put("message", request.getMessage());
            
            if (request.getSessionKey() != null && !request.getSessionKey().isEmpty()) {
                params.put("sessionKey", request.getSessionKey());
            }
            if (request.getDeliver() != null) {
                params.put("deliver", request.getDeliver());
            }
            
            WsRequest wsRequest = new WsRequest();
            wsRequest.setId(request.getId());
            wsRequest.setMethod("agent");
            wsRequest.setParams(params);
            
            sendRequest(wsRequest);
            
            WsResponse response = waitForResponse(request.getId(), request.getRequestTimeoutMs());
            
            if (!Boolean.TRUE.equals(response.getOk())) {
                request.getFuture().completeExceptionally(
                    new IOException("Agent request failed: " + response.getErrorMessage())
                );
                return;
            }
            
            String runId = (String) response.getPayloadValue("runId");
            String status = (String) response.getPayloadValue("status");
            
            if (runId != null) {
                request.setRunId(runId);
            }
            
            if ("accepted".equals(status)) {
                AgentResult result = waitForAgentResult(runId, request.getResultTimeoutMs());
                request.getFuture().complete(result);
            } else if ("ok".equals(status)) {
                AgentResult result = new AgentResult();
                result.setRunId(runId);
                result.setStatus(status);
                
                Map<String, Object> payload = response.getPayload();
                Map<String, Object> resultObj = (Map<String, Object>) payload.get("result");
                if (resultObj != null) {
                    List<Map<String, Object>> payloads = (List<Map<String, Object>>) resultObj.get("payloads");
                    if (payloads != null && !payloads.isEmpty()) {
                        String text = (String) payloads.get(0).get("text");
                        result.setSummary(text);
                    }
                }
                request.getFuture().complete(result);
            } else {
                request.getFuture().completeExceptionally(
                    new IOException("Unknown status: " + status)
                );
            }
            
        } catch (TimeoutException e) {
            request.getFuture().completeExceptionally(e);
        } catch (Exception e) {
            request.getFuture().completeExceptionally(e);
        }
    }

    private void sendConnect() {
        WsRequest.ConnectParams params;
        if (requireDevice) {
            params = new WsRequest.ConnectParams(token);
        } else {
            params = new WsRequest.ConnectParams(token).withoutDevice();
        }
        WsRequest connectRequest = WsRequest.connect(params);
        
        try {
            String json = objectMapper.writeValueAsString(connectRequest);
            logger.info("Sending connect request: {}", json);
            webSocket.send(json);
        } catch (IOException e) {
            logger.error("Failed to send connect: {}", e.getMessage());
        }
    }

    private void handleMessage(String text) {
        try {
            WsResponse response = objectMapper.readValue(text, WsResponse.class);
            logger.debug("Received message: {}", text);
            
            if (response.isResponse()) {
                String id = response.getId();
                if (id != null) {
                    pendingRequests.put(id, response);
                    
                    if (response.getOk() && "hello-ok".equals(response.getPayloadType())) {
                        connected = true;
                        Map<String, Object> payload = response.getPayload();
                        if (payload != null) {
                            protocolVersion = (int) payload.getOrDefault("protocol", 3);
                            Map<String, Object> auth = (Map<String, Object>) payload.get("auth");
                            if (auth != null) {
                                deviceToken = (String) auth.get("deviceToken");
                            }
                        }
                        logger.info("Connected with protocol version: {}", protocolVersion);
                        if (connectLatch != null) {
                            connectLatch.countDown();
                        }
                    }
                }
            } else if (response.isEvent()) {
                handleEvent(response);
            }
        } catch (IOException e) {
            logger.error("Failed to parse message: {}", e.getMessage());
        }
    }

    private void handleEvent(WsResponse response) {
        String event = response.getEvent();
        Map<String, Object> payload = response.getPayload();
        
        logger.debug("Received event: {}", event);
        
        for (WsEventListener listener : eventListeners) {
            try {
                listener.onEvent(event, payload);
            } catch (Exception e) {
                logger.error("Event listener error: {}", e.getMessage());
            }
        }
        
        if ("agent".equals(event) && payload != null) {
            String runId = (String) payload.get("runId");
            String status = (String) payload.get("status");
            if (runId != null) {
                pendingRequests.put("agent:" + runId, response);
                logger.debug("Agent event for runId: {}, status: {}", runId, status);
            }
        }
        
        if ("chat".equals(event) && payload != null) {
            String runId = (String) payload.get("runId");
            String sessionKey = (String) payload.get("sessionKey");
            String state = (String) payload.get("state");
            if (runId != null) {
                pendingRequests.put("chat:" + runId, response);
                logger.debug("Chat event for runId: {}, sessionKey: {}, state: {}", runId, sessionKey, state);
                
                Map<String, Object> message = (Map<String, Object>) payload.get("message");
                for (WsEventListener listener : eventListeners) {
                    try {
                        listener.onChatEvent(runId, sessionKey, state, message);
                    } catch (Exception e) {
                        logger.error("Chat event listener error: {}", e.getMessage());
                    }
                }
            }
        }
    }

    private void notifyError(Throwable t) {
        for (WsEventListener listener : eventListeners) {
            try {
                listener.onError(t);
            } catch (Exception e) {
                logger.error("Error listener error: {}", e.getMessage());
            }
        }
    }

    private void sendRequest(WsRequest request) {
        if (webSocket == null) {
            throw new IllegalStateException("WebSocket not initialized");
        }
        
        if (!connected && !connecting) {
            throw new IllegalStateException("WebSocket not connected");
        }
        
        try {
            String json = objectMapper.writeValueAsString(request);
            boolean sent = webSocket.send(json);
            logger.debug("Sent request: {}, success: {}", request.getMethod(), sent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to send request", e);
        }
    }

    private WsResponse waitForResponse(String requestId, long timeoutMs) throws TimeoutException, IOException {
        if (timeoutMs <= 0) {
            timeoutMs = DEFAULT_REQUEST_TIMEOUT_MS;
        }
        
        long start = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - start < timeoutMs) {
            WsResponse response = pendingRequests.remove(requestId);
            if (response != null) {
                return response;
            }
            
            if (!connected) {
                throw new IOException("WebSocket disconnected");
            }
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        throw new TimeoutException("Request timeout: " + requestId);
    }

    private AgentResult waitForAgentResult(String runId, long timeoutMs) throws TimeoutException, IOException {
        if (timeoutMs <= 0) {
            timeoutMs = DEFAULT_RESULT_TIMEOUT_MS;
        }
        
        long start = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - start < timeoutMs) {
            WsResponse eventResponse = pendingRequests.remove("agent:" + runId);
            if (eventResponse != null) {
                Map<String, Object> payload = eventResponse.getPayload();
                if (payload != null) {
                    String stream = (String) payload.get("stream");
                    Map<String, Object> data = (Map<String, Object>) payload.get("data");
                    
                    if ("lifecycle".equals(stream) && data != null) {
                        String phase = (String) data.get("phase");
                        logger.info("Agent lifecycle event: phase={}", phase);
                        
                        if ("complete".equals(phase)) {
                            AgentResult result = new AgentResult();
                            result.setRunId(runId);
                            result.setStatus("ok");
                            result.setSummary((String) data.get("summary"));
                            
                            logger.info("Agent completed with status: {}, summary: {}", result.getStatus(), result.getSummary());
                            return result;
                        } else if ("end".equals(phase)) {
                            logger.info("Agent ended, waiting for final response...");
                        } else if ("error".equals(phase)) {
                            AgentResult result = new AgentResult();
                            result.setRunId(runId);
                            result.setStatus("error");
                            result.setError((String) data.get("error"));
                            
                            logger.info("Agent error: {}", result.getError());
                            return result;
                        }
                    }
                }
            }
            
            WsResponse chatResponse = pendingRequests.remove("chat:" + runId);
            if (chatResponse != null) {
                Map<String, Object> payload = chatResponse.getPayload();
                if (payload != null) {
                    String state = (String) payload.get("state");
                    logger.info("Chat event received: state={}", state);
                    
                    if ("final".equals(state)) {
                        Map<String, Object> message = (Map<String, Object>) payload.get("message");
                        if (message != null) {
                            String summary = extractTextFromMessage(message);
                            if (summary != null) {
                                AgentResult result = new AgentResult();
                                result.setRunId(runId);
                                result.setStatus("ok");
                                result.setSummary(summary);
                                
                                logger.info("Agent completed via chat event, summary: {}", summary);
                                return result;
                            }
                        }
                    }
                }
            }
            
            if (!connected) {
                throw new IOException("WebSocket disconnected");
            }
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        AgentResult timeoutResult = new AgentResult();
        timeoutResult.setRunId(runId);
        timeoutResult.setStatus("timeout");
        timeoutResult.setError("Wait for result timeout after " + timeoutMs + "ms");
        return timeoutResult;
    }
    
    private String extractTextFromMessage(Map<String, Object> message) {
        if (message == null) {
            return null;
        }
        
        Object contentObj = message.get("content");
        if (contentObj instanceof List) {
            List<?> contentList = (List<?>) contentObj;
            for (Object item : contentList) {
                if (item instanceof Map) {
                    Map<?, ?> contentItem = (Map<?, ?>) item;
                    if ("text".equals(contentItem.get("type"))) {
                        return (String) contentItem.get("text");
                    }
                }
            }
        }
        
        String directText = (String) message.get("content");
        if (directText != null) {
            return directText;
        }
        
        return null;
    }

    public WsResponse sendAndWait(String method, Map<String, Object> params, long timeoutMs) throws IOException {
        String id = java.util.UUID.randomUUID().toString();
        
        WsRequest request = new WsRequest();
        request.setId(id);
        request.setMethod(method);
        request.setParams(params);
        
        sendRequest(request);
        
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            WsResponse response = pendingRequests.remove(id);
            if (response != null) {
                return response;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        throw new IOException("Request timeout: " + method);
    }

    public WsResponse health() throws IOException {
        return sendAndWait("health", null, 10000);
    }

    public WsResponse status() throws IOException {
        return sendAndWait("status", null, 10000);
    }

    public AgentResult runAgent(String message) throws IOException {
        try {
            return runAgentAsync(message, null, null, null).get(defaultResultTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException(e.getCause());
        } catch (TimeoutException e) {
            throw new IOException("Agent execution timeout", e);
        }
    }

    public AgentResult runAgent(String message, String agentId) throws IOException {
        try {
            return runAgentAsync(message, agentId, null, null).get(defaultResultTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException(e.getCause());
        } catch (TimeoutException e) {
            throw new IOException("Agent execution timeout", e);
        }
    }

    public AgentResult runAgent(String message, String agentId, Boolean deliver, long timeoutMs) throws IOException {
        long effectiveTimeout = timeoutMs > 0 ? timeoutMs : defaultResultTimeoutMs;
        try {
            return runAgentAsync(message, agentId, null, timeoutMs).get(effectiveTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException(e.getCause());
        } catch (TimeoutException e) {
            throw new IOException("Agent execution timeout", e);
        }
    }

    public CompletableFuture<AgentResult> runAgentAsync(String message, String agentId, 
            Long requestTimeoutMs, Long resultTimeoutMs) {
        
        if (requestQueue.size() >= maxQueueCapacity) {
            return CompletableFuture.failedFuture(
                new RejectedExecutionException("Request queue is full (" + maxQueueCapacity + "), please try again later")
            );
        }
        
        CompletableFuture<AgentResult> future = new CompletableFuture<>();
        
        String sessionKey = null;
        if (agentId != null && !agentId.isEmpty()) {
            sessionKey = "agent:main:" + agentId;
        }
        
        PendingRequest request = new PendingRequest(
            message,
            agentId,
            null,
            sessionKey,
            requestTimeoutMs != null ? requestTimeoutMs : defaultRequestTimeoutMs,
            resultTimeoutMs != null ? resultTimeoutMs : defaultResultTimeoutMs,
            future
        );
        
        boolean offered = requestQueue.offer(request);
        if (!offered) {
            future.completeExceptionally(
                new RejectedExecutionException("Failed to add request to queue")
            );
        }
        
        return future;
    }

    public int getQueueSize() {
        return requestQueue.size();
    }

    public boolean isQueueFull() {
        return requestQueue.size() >= maxQueueCapacity;
    }

    public int getMaxQueueCapacity() {
        return maxQueueCapacity;
    }

    public WsResponse sendMessage(String target, String message) throws IOException {
        Map<String, Object> params = Map.of("target", target, "message", message);
        return sendAndWait("send", params, 30000);
    }

    public WsResponse systemEvent(String text) throws IOException {
        Map<String, Object> params = Map.of("text", text);
        return sendAndWait("system-event", params, 10000);
    }

    public void addEventListener(WsEventListener listener) {
        eventListeners.add(listener);
    }

    public void removeEventListener(WsEventListener listener) {
        eventListeners.remove(listener);
    }

    public void close() {
        running.set(false);
        
        requestQueue.clear();
        
        if (webSocket != null) {
            try {
                webSocket.close(1000, "Client closed");
            } catch (Exception e) {
                logger.warn("Error closing WebSocket: {}", e.getMessage());
            }
        }
        
        connected = false;
        
        consumerExecutor.shutdown();
        try {
            if (!consumerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                consumerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            consumerExecutor.shutdownNow();
        }
        
        logger.info("WebSocket client closed");
    }

    public boolean isConnected() {
        return connected;
    }

    public void setRequireDevice(boolean requireDevice) {
        this.requireDevice = requireDevice;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public String getDeviceToken() {
        return deviceToken;
    }
}
