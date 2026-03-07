# OpenClaw Java Client

Java 客户端，用于连接 OpenClaw 自动化平台。

[![Java](https://img.shields.io/badge/Java-17%2B-blue?style=flat&logo=java)](https://www.java.com/)
[![Maven Central](https://img.shields.io/maven-central/v/cn.welsione/openclaw-java-client)](https://mvnrepository.com/artifact/cn.welsione/openclaw-java-client)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat)](LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/765952327/openclaw-java-client)](https://github.com/765952327/openclaw-java-client/releases)
[![GitHub Stars](https://img.shields.io/github/stars/765952327/openclaw-java-client)](https://github.com/765952327/openclaw-java-client/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/765952327/openclaw-java-client)](https://github.com/765952327/openclaw-java-client/network)
[![GitHub Issues](https://img.shields.io/github/issues/765952327/openclaw-java-client)](https://github.com/765952327/openclaw-java-client/issues)
[![Last Commit](https://img.shields.io/github/last-commit/765952327/openclaw-java-client)](https://github.com/765952327/openclaw-java-client/commits)

## 功能

| 功能 | 说明 |
|------|------|
| WebSocket | 实时双向通信、事件监听、同步/异步调用 |
| 流式响应 | 支持实时流式输出、思考过程、工具调用 |
| 自动重连 | 指数退避策略、自动恢复连接 |
| 健康检查 | 定期检查连接状态、失败时触发重连 |
| 消息重试 | 失败请求自动重试、指数退避 |
| 请求队列 | 有界队列、防内存溢出、超时保护 |
| 连接池 | 多连接并发、自动负载均衡 |
| 本地缓存 | TTL 缓存、减少重复请求 |
| Metrics | 请求延迟、队列大小等指标统计 |
| 统一异常 | 错误码分类、清晰的错误信息 |
| DSL 构建器 | 流式 API 配置客户端 |
| TLS/SSL | 安全连接支持 |
| 代理支持 | HTTP/SOCKS 代理 |
| 消息签名 | HMAC-SHA256 签名验证 |
| Spring Boot | 自动配置、零 XML 配置 |
| 多实例 | 支持配置多个 OpenClaw 实例 |

---

## 快速开始

### 方式一：Spring Boot 自动配置（最简单）

添加依赖即可自动配置：

```xml
<dependency>
    <groupId>cn.welsione</groupId>
    <artifactId>openclaw-java-client</artifactId>
    <version>1.0.2</version>
</dependency>
```

配置 application.yml：

```yaml
openclaw:
  enabled: true
  default-instance: local
  
  instances:
    local:
      base-url: http://127.0.0.1:18789
      token: your-token
      type: websocket    # http 或 websocket
      enabled: true
```

直接注入使用：

```java
@RestController
public class ChatController {
    
    @Autowired
    private OpenClawWsClient wsClient;
    
    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        AgentResult result = wsClient.runAgent(message, "main");
        return result.getSummary();
    }
}
```

---

### 方式二：多实例配置

配置多个 OpenClaw 实例：

```yaml
openclaw:
  enabled: true
  default-instance: prod
  
  instances:
    dev:
      base-url: http://192.168.1.100:18789
      token: dev-token
      type: websocket
      pool-size: 2
      enabled: true
      
    prod:
      base-url: https://openclaw.example.com
      token: prod-token
      type: websocket
      pool-size: 5
      enabled: true
      
    # HTTP 类型的实例
    api:
      base-url: https://api.example.com
      token: api-token
      type: http
      enabled: true
```

按名称注入指定的实例：

```java
@RestController
public class MultiInstanceController {
    
    // 注入 WebSocket 客户端（bean 名称：实例名 + "-ws"）
    @Qualifier("dev-ws")
    @Autowired
    private OpenClawWsClient devClient;
    
    @Qualifier("prod-ws")
    @Autowired
    private OpenClawWsClient prodClient;
    
    // 注入 HTTP 客户端（bean 名称：实例名）
    @Qualifier("api")
    @Autowired
    private OpenClawClient apiClient;
    
    // 注入连接池（bean 名称：实例名 + "-pool"）
    @Qualifier("prod-pool")
    @Autowired
    private OpenClawWsClientPool prodPool;
}
```

**Bean 命名规则：**

| 实例类型 | Bean 名称 |
|----------|------------|
| HTTP 客户端 | `<实例名>` |
| WebSocket 客户端 | `<实例名>-ws` |
| 连接池 | `<实例名>-pool` |

---

### 方式三：编程式使用（不使用 Spring）

```java
// 创建 WebSocket 客户端
OpenClawWsClient client = new OpenClawWsClient(
    "http://127.0.0.1:18789", 
    "ollama"
);

// 连接
client.connect();

// 执行 Agent
AgentResult result = client.runAgent("1+1=?", "main");
System.out.println(result.getSummary());  // 输出: 1+1 = 2 ✨

// 关闭
client.close();
```

### 使用 DSL 构建器

```java
OpenClawWsClient client = OpenClawWsClientBuilder.create()
    .baseUrl("http://127.0.0.1:18789")
    .token("ollama")
    .healthCheckEnabled(true)
    .healthCheckIntervalMs(10000)
    .retryEnabled(true)
    .maxRetryCount(3)
    .build();
```

---

## 高级用法

### 异步调用

```java
// 异步执行
CompletableFuture<AgentResult> future = client.runAgentAsync("你好", "main");
future.thenAccept(result -> System.out.println(result.getSummary()));

// 带 UID（用于追踪）
AgentResult result = client.runAgentWithUid("user-001", "你好", "main");
System.out.println(result.getUid());  // user-001
```

### 流式响应

```java
client.runAgentStream("你好", "main", new AgentStreamCallback() {
    @Override
    public void onStart(String runId, Map<String, Object> metadata) {
        System.out.println("开始执行: " + runId);
    }

    @Override
    public void onOutput(String runId, String text, Map<String, Object> data) {
        System.out.print(text);  // 实时输出
    }

    @Override
    public void onThinking(String runId, String thought) {
        System.out.println("思考: " + thought);
    }

    @Override
    public void onComplete(String runId, String summary, Map<String, Object> result) {
        System.out.println("\n完成: " + summary);
    }

    @Override
    public void onError(String runId, String error) {
        System.out.println("错误: " + error);
    }
});
```

### 事件监听

```java
client.addEventListener(new WsEventListener() {
    @Override
    public void onAgentEvent(String runId, String status, Map<String, Object> payload) {
        System.out.println("Agent: " + status);  // start, end, error
    }
    
    @Override
    public void onChatEvent(String runId, String sessionKey, String state, Map<String, Object> message) {
        if ("final".equals(state)) {
            System.out.println("回复: " + message.get("content"));
        }
    }

    @Override
    public void onHealthCheck(boolean healthy, Throwable error) {
        System.out.println("健康检查: " + (healthy ? "正常" : "失败"));
    }

    @Override
    public void onReconnected() {
        System.out.println("已重连");
    }
});
```

### 异常处理

```java
try {
    AgentResult result = client.runAgent("你好", "main");
} catch (OpenClawAgentException e) {
    System.out.println("错误码: " + e.getCode());      // A001, A002...
    System.out.println("错误信息: " + e.getMessage());
} catch (OpenClawConnectionException e) {
    System.out.println("连接错误: " + e.getCode());    // C001, C002...
} catch (OpenClawTimeoutException e) {
    System.out.println("超时: " + e.getTimeoutMs() + "ms");
}
```

### Metrics 指标

```java
ClientMetrics metrics = client.getMetrics();
System.out.println("总请求数: " + metrics.getTotalRequests());
System.out.println("成功率: " + metrics.getSuccessRate() + "%");
System.out.println("平均响应时间: " + metrics.getAverageRequestDurationMs() + "ms");
System.out.println("队列大小: " + metrics.getCurrentQueueSize());
```

### 使用缓存

```java
@Service
public class AgentService {
    
    @Autowired
    private OpenClawWsClient wsClient;
    
    public List<Agent> getAgents() {
        // 尝试从缓存获取
        List<Agent> cached = wsClient.getCache().get("agents", List.class);
        if (cached != null) {
            return cached;
        }
        
        // 从服务器获取并缓存
        WsResponse response = wsClient.toolsCatalog(null, null, 1, 100);
        List<Agent> agents = parseAgents(response);
        wsClient.getCache().put("agents", agents, 300000); // 缓存5分钟
        return agents;
    }
}
```

### 事件监听器（Spring）

```java
@Component
public class OpenClawEventListener implements WsEventListener {
    
    @Override
    public void onHealthCheck(boolean healthy, Throwable error) {
        System.out.println("Health check: " + (healthy ? "OK" : "FAILED"));
    }
    
    @Override
    public void onReconnected() {
        System.out.println("Reconnected!");
    }
    
    @Override
    public void onAgentComplete(String runId, String summary, Map<String, Object> result) {
        System.out.println("Agent " + runId + " completed: " + summary);
    }
}
```

---

## 配置属性

### 基础配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `openclaw.enabled` | true | 启用客户端 |
| `openclaw.default-instance` | - | 默认实例名称 |

### 实例配置（instances）

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `instances.<name>.base-url` | - | Gateway 地址 |
| `instances.<name>.token` | - | 认证令牌 |
| `instances.<name>.type` | http | 实例类型（http/websocket） |
| `instances.<name>.pool-size` | 1 | 连接池大小 |
| `instances.<name>.enabled` | true | 是否启用 |

### WebSocket 高级配置

```yaml
openclaw:
  instances:
    prod:
      base-url: https://openclaw.example.com
      token: your-token
      type: websocket
      ws-properties:
        max-queue-capacity: 500
        default-request-timeout-ms: 60000
        default-result-timeout-ms: 300000
        
        # 自动重连
        auto-reconnect: true
        max-reconnect-retries: 10
        reconnect-initial-delay-ms: 1000
        reconnect-max-delay-ms: 30000
        
        # 健康检查
        health-check-enabled: true
        health-check-interval-ms: 30000
        health-check-timeout-ms: 10000
        
        # 消息重试
        retry-enabled: true
        max-retry-count: 3
        retry-initial-delay-ms: 500
        retry-max-delay-ms: 5000
        
        # 压缩与安全
        compression-enabled: true
        ssl-verify-enabled: true
        
        # 代理（可选）
        # proxy-host: localhost
        # proxy-port: 8080
```

---

## API 参考

### OpenClawWsClient

```java
// 同步
client.runAgent(message);
client.runAgent(message, agentId);
client.runAgentWithUid(uid, message, agentId);

// 异步
client.runAgentAsync(message);
client.runAgentAsync(message, agentId);

// 流式
client.runAgentStream(message, callback);
client.runAgentStream(message, agentId, callback);

// 会话管理
client.sessionsHistory(sessionKey, limit);
client.sessionsList(page, pageSize);
client.sessionsDelete(sessionKey);

// 工具集成
client.toolsCatalog(category, keyword, page, pageSize);
client.toolsInvoke(toolName, arguments);
client.toolsRegister(toolName, description, schema);

// 连接池
OpenClawWsClientPool pool = new OpenClawWsClientPool(baseUrl, token, 5);
pool.connectAll();
OpenClawWsClient client = pool.getClient();
pool.closeAll();

// 缓存
client.getCache().put("agents", agentsList);
client.getCache().put("data", data, 60000);
Object cached = client.getCache().get("key");

// 签名验证
MessageSigner signer = new MessageSigner(secret);
String signature = signer.sign(payload);
boolean valid = signer.verify(payload, signature);

// 状态
client.isConnected();
client.health();
client.status();

// Metrics
client.getMetrics();

// 事件
client.addEventListener(listener);
client.removeEventListener(listener);

// 关闭
client.close();
```

### AgentResult

```java
result.getUid();       // 请求 UID
result.getRunId();     // 运行 ID
result.getStatus();    // accepted, ok, error, timeout
result.getSummary();   // 回复内容
result.getError();     // 错误信息

result.isOk();         // 是否成功
result.isError();     // 是否有错
result.isAccepted();  // 是否已接受
```

### 异常类

| 异常类 | 错误码前缀 | 说明 |
|--------|------------|------|
| `OpenClawConnectionException` | C001-C005 | 连接相关 |
| `OpenClawRequestException` | R001-R004 | 请求相关 |
| `OpenClawTimeoutException` | R001, A003 | 超时相关 |
| `OpenClawAgentException` | A001-A004 | Agent 相关 |

---

## 构建

```bash
mvn clean package
```

## 许可证

MIT License
