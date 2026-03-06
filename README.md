# OpenClaw Java Client

Java 客户端，用于连接 OpenClaw 自动化平台。

[![Java](https://img.shields.io/badge/Java-17%2B-blue?style=flat&logo=java)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat)](LICENSE)

## 功能

| 功能 | 说明 |
|------|------|
| WebSocket | 实时双向通信、事件监听、同步/异步调用 |
| Webhook | HTTP 方式触发自动化 |
| 请求队列 | 有界队列、防内存溢出、超时保护 |
| Spring Boot | 自动配置、零 XML 配置 |

## 快速开始

### 安装

```xml
<dependency>
    <groupId>ai.openclaw</groupId>
    <artifactId>java-openclaw-client</artifactId>
    <version>1.0.1</version>
</dependency>
```

### WebSocket 调用

```java
// 创建客户端
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

### 异步调用

```java
// 异步执行
CompletableFuture<AgentResult> future = client.runAgentAsync("你好", "main");
future.thenAccept(result -> System.out.println(result.getSummary()));

// 带 UID（用于追踪）
AgentResult result = client.runAgentWithUid("user-001", "你好", "main");
System.out.println(result.getUid());  // user-001
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
});
```

## Spring Boot Starter

添加依赖后自动配置：

```xml
<dependency>
    <groupId>ai.openclaw</groupId>
    <artifactId>java-openclaw-client</artifactId>
    <version>1.0.1</version>
</dependency>
```

### 配置

```yaml
openclaw:
  ws:
    enabled: true
    base-url: http://127.0.0.1:18789
    token: ollama
    auto-connect: false
    require-device: false
    max-queue-capacity: 500
    default-result-timeout-ms: 300000
```

### 使用

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

## 配置属性

### WebSocket

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `openclaw.ws.enabled` | true | 启用客户端 |
| `openclaw.ws.base-url` | http://127.0.0.1:18789 | Gateway 地址 |
| `openclaw.ws.token` | - | 认证令牌 |
| `openclaw.ws.require-device` | false | 需要设备认证 |
| `openclaw.ws.auto-connect` | false | 启动自动连接 |
| `openclaw.ws.max-queue-capacity` | 500 | 请求队列上限 |
| `openclaw.ws.default-result-timeout-ms` | 300000 | 结果超时(毫秒) |

### Webhook

```yaml
openclaw:
  base-url: http://127.0.0.1:18789
  token: your-token
  hooks:
    enabled: true
    token: webhook-secret
```

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

// 状态
client.isConnected();
client.health();
client.status();

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
result.getStatus();    // accepted, ok, error
result.getSummary();   // 回复内容
result.getError();    // 错误信息

result.isOk();        // 是否成功
result.isError();    // 是否有错
```

## 构建

```bash
mvn clean package
```

## 许可证

MIT License
