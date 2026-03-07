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

/**
 * OpenClaw WebSocket Client
 * 
 * <p>用于连接 OpenClaw Gateway 的 WebSocket 客户端，支持：
 * <ul>
 *   <li>实时双向通信</li>
 *   <li>同步/异步 Agent 执行</li>
 *   <li>事件监听</li>
 *   <li>自动重连</li>
 *   <li>请求队列</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>{@code
 * OpenClawWsClient client = new OpenClawWsClient("http://127.0.0.1:18789", "token");
 * client.connect();
 * AgentResult result = client.runAgent("Hello", "main");
 * client.close();
 * }</pre>
 */
public class OpenClawWsClient {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(OpenClawWsClient.class);

    // ==================== 默认配置常量 ====================
    
    /** 默认请求超时时间（毫秒） */
    private static final long DEFAULT_REQUEST_TIMEOUT_MS = 60000;
    /** 默认结果超时时间（毫秒） */
    private static final long DEFAULT_RESULT_TIMEOUT_MS = 300000;
    /** 默认请求队列容量 */
    private static final int DEFAULT_MAX_QUEUE_CAPACITY = 500;
    /** 默认重连初始延迟（毫秒） */
    private static final long DEFAULT_RECONNECT_INITIAL_DELAY_MS = 1000;
    /** 默认重连最大延迟（毫秒） */
    private static final long DEFAULT_RECONNECT_MAX_DELAY_MS = 30000;
    /** 默认最大重连次数 */
    private static final int DEFAULT_RECONNECT_MAX_RETRIES = 10;
    /** 默认健康检查间隔（毫秒） */
    private static final long DEFAULT_HEALTH_CHECK_INTERVAL_MS = 30000;
    /** 默认健康检查超时（毫秒） */
    private static final long DEFAULT_HEALTH_CHECK_TIMEOUT_MS = 10000;

    // ==================== 基础配置 ====================
    
    /** WebSocket 连接地址 */
    private final String baseUrl;
    /** Gateway 认证令牌 */
    private final String token;
    /** JSON 序列化/反序列化工具 */
    private final ObjectMapper objectMapper;
    
    // ==================== 连接状态 ====================
    
    /** OkHttp WebSocket 实例 */
    private WebSocket webSocket;
    /** OkHttp HTTP 客户端 */
    private OkHttpClient httpClient;
    /** 是否已连接 */
    private volatile boolean connected = false;
    /** 是否正在连接中 */
    private volatile boolean connecting = false;
    
    // ==================== 请求管理 ====================
    
    /** 等待响应的请求 Map */
    private final Map<String, WsResponse> pendingRequests = new ConcurrentHashMap<>();
    /** 事件监听器列表 */
    private final CopyOnWriteArrayList<WsEventListener> eventListeners = new CopyOnWriteArrayList<>();
    /** 连接完成 latch */
    private CountDownLatch connectLatch;
    
    // ==================== 协议信息 ====================
    
    /** 协议版本 */
    private int protocolVersion;
    /** 设备令牌 */
    private String deviceToken;
    /** 是否需要设备认证 */
    private boolean requireDevice = true;
    
    // ==================== 队列配置 ====================
    
    /** 请求队列最大容量 */
    private final int maxQueueCapacity;
    /** 默认请求超时（毫秒） */
    private final long defaultRequestTimeoutMs;
    /** 默认结果超时（毫秒） */
    private final long defaultResultTimeoutMs;
    /** 请求队列 */
    private final BlockingQueue<PendingRequest> requestQueue;
    /** 消费者线程池 */
    private final ExecutorService consumerExecutor;
    /** 运行状态标志 */
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    // ==================== 重连配置 ====================
    
    /** 是否启用自动重连 */
    private final boolean autoReconnect;
    /** 最大重连次数 */
    private final int maxReconnectRetries;
    /** 重连初始延迟（毫秒） */
    private final long reconnectInitialDelayMs;
    /** 重连最大延迟（毫秒） */
    private final long reconnectMaxDelayMs;
    /** 是否正在重连 */
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    /** 当前重连次数 */
    private volatile int currentReconnectAttempt = 0;
    
    // ==================== 健康检查配置 ====================
    
    /** 是否启用健康检查 */
    private final boolean healthCheckEnabled;
    /** 健康检查间隔（毫秒） */
    private final long healthCheckIntervalMs;
    /** 健康检查超时（毫秒） */
    private final long healthCheckTimeoutMs;
    /** 健康检查线程 */
    private volatile Thread healthCheckThread;
    /** 上次健康检查时间 */
    private volatile long lastHealthCheckTime = 0;
    /** 上次健康检查结果 */
    private volatile boolean lastHealthCheckResult = false;

    /**
     * 构造函数（使用默认配置）
     * 
     * @param baseUrl Gateway 地址
     * @param token   Gateway 令牌
     */
    public OpenClawWsClient(String baseUrl, String token) {
        this(baseUrl, token, DEFAULT_MAX_QUEUE_CAPACITY, DEFAULT_REQUEST_TIMEOUT_MS, DEFAULT_RESULT_TIMEOUT_MS,
            true, DEFAULT_RECONNECT_MAX_RETRIES, DEFAULT_RECONNECT_INITIAL_DELAY_MS, DEFAULT_RECONNECT_MAX_DELAY_MS,
            true, DEFAULT_HEALTH_CHECK_INTERVAL_MS, DEFAULT_HEALTH_CHECK_TIMEOUT_MS);
    }

    /**
     * 构造函数（自定义队列和超时配置）
     * 
     * @param baseUrl                  Gateway 地址
     * @param token                    Gateway 令牌
     * @param maxQueueCapacity         请求队列最大容量
     * @param defaultRequestTimeoutMs   请求超时（毫秒）
     * @param defaultResultTimeoutMs    结果超时（毫秒）
     */
    public OpenClawWsClient(String baseUrl, String token, int maxQueueCapacity, 
            long defaultRequestTimeoutMs, long defaultResultTimeoutMs) {
        this(baseUrl, token, maxQueueCapacity, defaultRequestTimeoutMs, defaultResultTimeoutMs,
            true, DEFAULT_RECONNECT_MAX_RETRIES, DEFAULT_RECONNECT_INITIAL_DELAY_MS, DEFAULT_RECONNECT_MAX_DELAY_MS,
            true, DEFAULT_HEALTH_CHECK_INTERVAL_MS, DEFAULT_HEALTH_CHECK_TIMEOUT_MS);
    }

    /**
     * 构造函数（自定义所有配置）
     * 
     * @param baseUrl                   Gateway 地址
     * @param token                     Gateway 令牌
     * @param maxQueueCapacity          请求队列最大容量
     * @param defaultRequestTimeoutMs    请求超时（毫秒）
     * @param defaultResultTimeoutMs     结果超时（毫秒）
     * @param autoReconnect              是否启用自动重连
     * @param maxReconnectRetries        最大重连次数
     * @param reconnectInitialDelayMs    重连初始延迟（毫秒）
     * @param reconnectMaxDelayMs        重连最大延迟（毫秒）
     * @param healthCheckEnabled        是否启用健康检查
     * @param healthCheckIntervalMs      健康检查间隔（毫秒）
     * @param healthCheckTimeoutMs      健康检查超时（毫秒）
     */
    public OpenClawWsClient(String baseUrl, String token, int maxQueueCapacity, 
            long defaultRequestTimeoutMs, long defaultResultTimeoutMs,
            boolean autoReconnect, int maxReconnectRetries, long reconnectInitialDelayMs, long reconnectMaxDelayMs,
            boolean healthCheckEnabled, long healthCheckIntervalMs, long healthCheckTimeoutMs) {
        this.baseUrl = baseUrl.replace("http", "ws") + "/ws";
        this.token = token;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.maxQueueCapacity = maxQueueCapacity;
        this.defaultRequestTimeoutMs = defaultRequestTimeoutMs;
        this.defaultResultTimeoutMs = defaultResultTimeoutMs;
        this.autoReconnect = autoReconnect;
        this.maxReconnectRetries = maxReconnectRetries;
        this.reconnectInitialDelayMs = reconnectInitialDelayMs;
        this.reconnectMaxDelayMs = reconnectMaxDelayMs;
        this.healthCheckEnabled = healthCheckEnabled;
        this.healthCheckIntervalMs = healthCheckIntervalMs;
        this.healthCheckTimeoutMs = healthCheckTimeoutMs;
        
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

    /**
     * 连接到 OpenClaw Gateway
     * 
     * @return 连接是否成功
     * @throws IOException 连接失败时抛出
     */
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
                handleDisconnection(code, reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                logger.error("WebSocket failure: {}", t.getMessage());
                connected = false;
                notifyError(t);
                if (connectLatch != null) {
                    connectLatch.countDown();
                }
                handleDisconnection(1001, t.getMessage());
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
            startHealthCheck();
        }
        
        return connected;
    }
    
    /**
     * 启动健康检查线程
     */
    private void startHealthCheck() {
        if (!healthCheckEnabled) {
            return;
        }
        
        if (healthCheckThread != null && healthCheckThread.isAlive()) {
            return;
        }
        
        healthCheckThread = new Thread(this::healthCheckLoop, "openclaw-ws-health-check");
        healthCheckThread.setDaemon(true);
        healthCheckThread.start();
        logger.info("Health check thread started, interval: {}ms", healthCheckIntervalMs);
    }
    
    /**
     * 健康检查线程主循环
     */
    private void healthCheckLoop() {
        logger.info("Health check loop started");
        
        while (running.get() && connected) {
            try {
                Thread.sleep(healthCheckIntervalMs);
                
                if (!running.get() || !connected) {
                    break;
                }
                
                performHealthCheck();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Health check error: {}", e.getMessage());
            }
        }
        
        logger.info("Health check loop stopped");
    }
    
    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        try {
            WsResponse response = health();
            boolean healthy = response != null && Boolean.TRUE.equals(response.getOk());
            
            lastHealthCheckTime = System.currentTimeMillis();
            lastHealthCheckResult = healthy;
            
            if (healthy) {
                logger.debug("Health check passed");
                for (WsEventListener listener : eventListeners) {
                    try {
                        listener.onHealthCheck(true, null);
                    } catch (Exception e) {
                        logger.error("Health check listener error: {}", e.getMessage());
                    }
                }
            } else {
                logger.warn("Health check failed: {}", response);
                for (WsEventListener listener : eventListeners) {
                    try {
                        listener.onHealthCheck(false, new IOException("Health check failed"));
                    } catch (Exception e) {
                        logger.error("Health check listener error: {}", e.getMessage());
                    }
                }
                
                if (autoReconnect && !reconnecting.get()) {
                    logger.warn("Health check failed, triggering reconnection...");
                    handleDisconnection(1001, "Health check failed");
                }
            }
            
        } catch (Exception e) {
            logger.error("Health check exception: {}", e.getMessage());
            lastHealthCheckResult = false;
            
            for (WsEventListener listener : eventListeners) {
                try {
                    listener.onHealthCheck(false, e);
                } catch (Exception ex) {
                    logger.error("Health check listener error: {}", ex.getMessage());
                }
            }
            
            if (autoReconnect && !reconnecting.get()) {
                logger.warn("Health check exception, triggering reconnection...");
                handleDisconnection(1001, "Health check exception: " + e.getMessage());
            }
        }
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

    private void handleDisconnection(int code, String reason) {
        if (!running.get()) {
            logger.info("Client stopped, not attempting reconnection");
            return;
        }
        
        if (!autoReconnect) {
            logger.info("Auto-reconnect disabled, not attempting reconnection");
            return;
        }
        
        if (reconnecting.getAndSet(true)) {
            logger.info("Already reconnecting, skipping");
            return;
        }
        
        try {
            while (currentReconnectAttempt < maxReconnectRetries && running.get()) {
                currentReconnectAttempt++;
                long delay = Math.min(
                    reconnectInitialDelayMs * (long) Math.pow(2, currentReconnectAttempt - 1),
                    reconnectMaxDelayMs
                );
                
                logger.info("Attempting reconnection {}/{} after {}ms", 
                    currentReconnectAttempt, maxReconnectRetries, delay);
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
                if (!running.get()) {
                    break;
                }
                
                try {
                    connectLatch = new CountDownLatch(1);
                    
                    String wsUrl = baseUrl.startsWith("ws") ? baseUrl : baseUrl.replace("http", "ws");
                    if (!wsUrl.contains("/ws")) {
                        wsUrl = wsUrl + "/ws";
                    }
                    
                    Request request = new Request.Builder().url(wsUrl).build();
                    webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
                        @Override
                        public void onOpen(WebSocket webSocket, Response response) {
                            logger.info("Reconnection opened, sending connect request...");
                            connecting = true;
                            sendConnect();
                        }

                        @Override
                        public void onMessage(WebSocket webSocket, String text) {
                            handleMessage(text);
                        }

                        @Override
                        public void onClosing(WebSocket webSocket, int code, String reason) {
                            logger.info("Reconnection closing: {} - {}", code, reason);
                            connected = false;
                        }

                        @Override
                        public void onClosed(WebSocket webSocket, int code, String reason) {
                            logger.info("Reconnection closed: {} - {}", code, reason);
                            connected = false;
                            if (connectLatch != null) {
                                connectLatch.countDown();
                            }
                        }

                        @Override
                        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                            logger.error("Reconnection failure: {}", t.getMessage());
                            connected = false;
                            if (connectLatch != null) {
                                connectLatch.countDown();
                            }
                        }
                    });
                    
                    try {
                        connectLatch.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    
                    if (connected) {
                        logger.info("Reconnection successful!");
                        currentReconnectAttempt = 0;
                        
                        for (WsEventListener listener : eventListeners) {
                            try {
                                listener.onReconnected();
                            } catch (Exception e) {
                                logger.error("Reconnect listener error: {}", e.getMessage());
                            }
                        }
                        break;
                    }
                    
                } catch (Exception e) {
                    logger.error("Reconnection attempt failed: {}", e.getMessage());
                }
            }
            
            if (!connected && currentReconnectAttempt >= maxReconnectRetries) {
                logger.error("Max reconnection attempts reached, giving up");
                for (WsEventListener listener : eventListeners) {
                    try {
                        listener.onReconnectFailed(new IOException("Max reconnection attempts reached"));
                    } catch (Exception e) {
                        logger.error("Reconnect failed listener error: {}", e.getMessage());
                    }
                }
            }
            
        } finally {
            reconnecting.set(false);
        }
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
                result.setUid(request.getUid());
                request.getFuture().complete(result);
            } else if ("ok".equals(status)) {
                AgentResult result = new AgentResult();
                result.setUid(request.getUid());
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

    /**
     * 同步执行 Agent（使用默认 agentId）
     * 
     * @param message 消息内容
     * @return Agent 执行结果
     * @throws IOException 执行失败时抛出
     */
    public AgentResult runAgent(String message) throws IOException {
        return runAgent((String) null, message, null, null, 0);
    }

    /**
     * 同步执行 Agent
     * 
     * @param message  消息内容
     * @param agentId  Agent ID
     * @return Agent 执行结果
     * @throws IOException 执行失败时抛出
     */
    public AgentResult runAgent(String message, String agentId) throws IOException {
        return runAgent(null, message, agentId, null, 0);
    }

    /**
     * 同步执行 Agent（完整参数）
     * 
     * @param message   消息内容
     * @param agentId   Agent ID
     * @param deliver   是否投递响应
     * @param timeoutMs 超时时间（毫秒），0 使用默认值
     * @return Agent 执行结果
     * @throws IOException 执行失败时抛出
     */
    public AgentResult runAgent(String message, String agentId, Boolean deliver, long timeoutMs) throws IOException {
        return runAgent(null, message, agentId, deliver, timeoutMs);
    }

    /**
     * 同步执行 Agent（带自定义 UID）
     * 
     * @param uid      自定义 UID，用于追踪请求
     * @param message  消息内容
     * @return Agent 执行结果
     * @throws IOException 执行失败时抛出
     */
    public AgentResult runAgentWithUid(String uid, String message) throws IOException {
        return runAgent(uid, message, null, null, 0);
    }

    /**
     * 同步执行 Agent（带自定义 UID）
     * 
     * @param uid      自定义 UID
     * @param message  消息内容
     * @param agentId  Agent ID
     * @return Agent 执行结果
     * @throws IOException 执行失败时抛出
     */
    public AgentResult runAgentWithUid(String uid, String message, String agentId) throws IOException {
        return runAgent(uid, message, agentId, null, 0);
    }

    /**
     * 同步执行 Agent（带自定义 UID 和完整参数）
     * 
     * @param uid       自定义 UID
     * @param message   消息内容
     * @param agentId   Agent ID
     * @param deliver   是否投递响应
     * @param timeoutMs 超时时间（毫秒）
     * @return Agent 执行结果
     * @throws IOException 执行失败时抛出
     */
    public AgentResult runAgentWithUid(String uid, String message, String agentId, Boolean deliver, long timeoutMs) throws IOException {
        long effectiveTimeout = timeoutMs > 0 ? timeoutMs : defaultResultTimeoutMs;
        try {
            return runAgentAsync(uid, message, agentId, deliver, null, null).get(effectiveTimeout, TimeUnit.MILLISECONDS);
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

    private AgentResult runAgent(String uid, String message, String agentId, Boolean deliver, long timeoutMs) throws IOException {
        long effectiveTimeout = timeoutMs > 0 ? timeoutMs : defaultResultTimeoutMs;
        try {
            return runAgentAsync(uid, message, agentId, deliver, null, null).get(effectiveTimeout, TimeUnit.MILLISECONDS);
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

    /**
     * 异步执行 Agent（使用默认 agentId）
     * 
     * @param message 消息内容
     * @return CompletableFuture，包含 Agent 执行结果
     */
    public CompletableFuture<AgentResult> runAgentAsync(String message) {
        return runAgentAsync(null, message, null, null, null, null);
    }

    /**
     * 异步执行 Agent
     * 
     * @param message  消息内容
     * @param agentId  Agent ID
     * @return CompletableFuture，包含 Agent 执行结果
     */
    public CompletableFuture<AgentResult> runAgentAsync(String message, String agentId) {
        return runAgentAsync(null, message, agentId, null, null, null);
    }

    /**
     * 异步执行 Agent
     * 
     * @param message  消息内容
     * @param agentId  Agent ID
     * @param deliver  是否投递响应
     * @return CompletableFuture，包含 Agent 执行结果
     */
    public CompletableFuture<AgentResult> runAgentAsync(String message, String agentId, Boolean deliver) {
        return runAgentAsync(null, message, agentId, deliver, null, null);
    }

    /**
     * 异步执行 Agent（完整参数）
     * 
     * @param uid                自定义 UID
     * @param message            消息内容
     * @param agentId           Agent ID
     * @param deliver            是否投递响应
     * @param requestTimeoutMs   请求超时（毫秒）
     * @param resultTimeoutMs    结果超时（毫秒）
     * @return CompletableFuture，包含 Agent 执行结果
     */
    public CompletableFuture<AgentResult> runAgentAsync(String uid, String message, String agentId, 
            Boolean deliver, Long requestTimeoutMs, Long resultTimeoutMs) {
        
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
            uid,
            message,
            agentId,
            deliver,
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
    
    /**
     * 获取当前队列大小
     * 
     * @return 当前等待处理的请求数量
     */
    public int getQueueSize() {
        return requestQueue.size();
    }

    /**
     * 检查队列是否已满
     * 
     * @return 队列是否已满
     */
    public boolean isQueueFull() {
        return requestQueue.size() >= maxQueueCapacity;
    }

    /**
     * 获取队列最大容量
     * 
     * @return 队列最大容量
     */
    public int getMaxQueueCapacity() {
        return maxQueueCapacity;
    }

    /**
     * 检查是否启用自动重连
     * 
     * @return 是否启用自动重连
     */
    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    /**
     * 获取当前重连尝试次数
     * 
     * @return 当前重连尝试次数
     */
    public int getCurrentReconnectAttempt() {
        return currentReconnectAttempt;
    }

    /**
     * 获取最大重连次数
     * 
     * @return 最大重连次数
     */
    public int getMaxReconnectRetries() {
        return maxReconnectRetries;
    }

    /**
     * 检查是否正在重连
     * 
     * @return 是否正在重连
     */
    public boolean isReconnecting() {
        return reconnecting.get();
    }

    /**
     * 发送消息到指定目标
     * 
     * @param target  目标标识
     * @param message 消息内容
     * @return 响应结果
     * @throws IOException 发送失败时抛出
     */
    public WsResponse sendMessage(String target, String message) throws IOException {
        Map<String, Object> params = Map.of("target", target, "message", message);
        return sendAndWait("send", params, 30000);
    }

    /**
     * 发送系统事件
     * 
     * @param text 事件内容
     * @return 响应结果
     * @throws IOException 发送失败时抛出
     */
    public WsResponse systemEvent(String text) throws IOException {
        Map<String, Object> params = Map.of("text", text);
        return sendAndWait("system-event", params, 10000);
    }

    /**
     * 添加事件监听器
     * 
     * @param listener 事件监听器
     */
    public void addEventListener(WsEventListener listener) {
        eventListeners.add(listener);
    }

    /**
     * 移除事件监听器
     * 
     * @param listener 事件监听器
     */
    public void removeEventListener(WsEventListener listener) {
        eventListeners.remove(listener);
    }

    /**
     * 关闭客户端连接
     */
    public void close() {
        running.set(false);
        
        requestQueue.clear();
        
        if (healthCheckThread != null && healthCheckThread.isAlive()) {
            healthCheckThread.interrupt();
            try {
                healthCheckThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
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

    /**
     * 检查是否已连接
     * 
     * @return 是否已连接
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * 设置是否需要设备认证
     * 
     * @param requireDevice 是否需要设备认证
     */
    public void setRequireDevice(boolean requireDevice) {
        this.requireDevice = requireDevice;
    }

    /**
     * 获取协议版本
     * 
     * @return 协议版本号
     */
    public int getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * 获取设备令牌
     * 
     * @return 设备令牌
     */
    public String getDeviceToken() {
        return deviceToken;
    }

    /**
     * 检查是否启用健康检查
     * 
     * @return 是否启用健康检查
     */
    public boolean isHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    /**
     * 获取健康检查间隔
     * 
     * @return 健康检查间隔（毫秒）
     */
    public long getHealthCheckIntervalMs() {
        return healthCheckIntervalMs;
    }

    /**
     * 获取健康检查超时
     * 
     * @return 健康检查超时（毫秒）
     */
    public long getHealthCheckTimeoutMs() {
        return healthCheckTimeoutMs;
    }

    /**
     * 获取上次健康检查时间
     * 
     * @return 上次健康检查时间戳（毫秒），0 表示未检查过
     */
    public long getLastHealthCheckTime() {
        return lastHealthCheckTime;
    }

    /**
     * 获取上次健康检查结果
     * 
     * @return 上次健康检查结果，false 表示未检查过或检查失败
     */
    public boolean getLastHealthCheckResult() {
        return lastHealthCheckResult;
    }

    /**
     * 检查健康检查线程是否在运行
     * 
     * @return 健康检查线程是否在运行
     */
    public boolean isHealthCheckRunning() {
        return healthCheckThread != null && healthCheckThread.isAlive();
    }
}
