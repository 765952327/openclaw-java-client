# OpenClaw Java Client

Java客户端包，用于通过Webhook和WebSocket调用OpenClaw自动化平台。

[![Java Version](https://img.shields.io/badge/Java-17%2B-blue)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## 功能特性

### 核心功能
- **Webhook 调用** - 通过HTTP调用OpenClaw自动化平台
- **WebSocket 实时通信** - 双向实时连接，支持事件监听
- **定时任务** - 支持Cron表达式配置定时任务
- **多实例管理** - 支持连接多个OpenClaw网关实例
- **批处理请求** - 支持串行和并行请求

### 通信方式

| 方式 | 协议 | 适用场景 |
|------|------|---------|
| Webhook | HTTP | 简单请求、触发自动化 |
| WebSocket | WS | 实时交互、长时间运行、事件监听 |

### WebSocket 特性
- 双向实时通信
- 支持 `agent` 和 `chat` 事件监听
- 会话管理 (`sessionKey`)
- 同步/异步 agent 执行
- 心跳保活

## 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>ai.openclaw</groupId>
    <artifactId>openclaw-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 基本用法 (Webhook)

```java
// 创建客户端
OpenClawClient client = new OpenClawClient(
    "http://127.0.0.1:18789",
    "your-secret-token"
);

// 触发心跳
client.wake("System line message");

// 运行智能体
client.runAgent("Summarize my inbox", "EmailAgent");

// 关闭客户端
client.close();
```

### WebSocket 用法

```java
// 创建WebSocket客户端
OpenClawWsClient wsClient = new OpenClawWsClient(
    "http://127.0.0.1:18789",  // Gateway地址
    "ollama"                     // Gateway令牌
);

// 跳过设备认证（开发环境）
wsClient.setRequireDevice(false);

// 连接
boolean connected = wsClient.connect();
if (connected) {
    System.out.println("Connected to OpenClaw Gateway");
    
    // 同步执行Agent
    AgentResult result = wsClient.runAgent("What is 1+1?", "main");
    System.out.println("Result: " + result.getSummary());
    
    // 添加事件监听器
    wsClient.addEventListener(new WsEventListener() {
        @Override
        public void onAgentEvent(String runId, String status, Map<String, Object> payload) {
            System.out.println("Agent event: " + status);
        }
        
        @Override
        public void onChatEvent(String runId, String sessionKey, String state, Map<String, Object> message) {
            System.out.println("Chat event: " + state);
        }
    });
}

// 关闭连接
wsClient.close();
```

### 异步 Agent 执行

```java
AsyncAgentService asyncService = new AsyncAgentService(wsClient);

// 异步执行
asyncService.runAgentAsync("Hello", "main").thenAccept(result -> {
    System.out.println("Summary: " + result.getSummary());
});
```

## 使用配置

```java
// 通过配置类
OpenClawProperties properties = new OpenClawProperties();
properties.setBaseUrl("http://127.0.0.1:18789");
properties.setToken("your-secret-token");

OpenClawClient client = new OpenClawClient(properties);
```

## 定时任务

### 使用Spring Boot

在`application.yml`中配置：

```yaml
openclaw:
  base-url: http://127.0.0.1:18789
  token: your-secret-token
  
  scheduled-tasks:
    daily-summary:
      cron: "0 0 9 * * ?"
      message: "Give me a summary of my day"
      agent-name: DailySummary
      enabled: true
```

### 编程方式

```java
OpenClawClient client = new OpenClawClient(baseUrl, token);
OpenClawTaskScheduler scheduler = new OpenClawTaskScheduler(client);

scheduler.addTask("daily-summary", 
    OpenClawTaskScheduler.TaskConfig.builder()
        .cron("0 0 9 * * ?")
        .message("Give me a summary of my day")
        .agentName("DailySummary")
        .enabled(true)
        .build()
);

scheduler.start();
```

## API参考

### OpenClawClient

- `wake(String text)` - 触发心跳
- `wake(String text, String mode)` - 触发心跳（指定模式）
- `sendMessage(String message)` - 发送消息给默认智能体
- `runAgent(String message, String agentName)` - 运行指定智能体
- `runAgent(AgentRequest request)` - 使用完整请求运行智能体
- `sendToCustomHook(String hookName, Object body)` - 发送请求到自定义Hook
- `gmail(String source, List<Map> messages)` - 发送Gmail事件

### WebhookService

```java
WebhookService webhookService = new WebhookService(properties);

// 检查是否启用
boolean enabled = webhookService.isEnabled();

// 验证Token
boolean valid = webhookService.validateToken(token);

// 获取映射配置
Map<String, Object> mapping = webhookService.getMappingConfig("github");

// 构建Agent请求
Map<String, Object> agentRequest = webhookService.buildAgentRequestFromMapping("github", requestBody);
```

### AgentRequest.Builder

```java
AgentRequest.builder()
    .message("Your message")
    .name("AgentName")
    .sessionKey("session-id")
    .wakeMode("now")
    .model("openai/gpt-5.2-mini")
    .thinking("high")
    .timeoutSeconds(120)
    .channel("whatsapp")
    .to("+15551234567")
    .build()
```

## Webhook配置

### 启用Webhook

在`application.yml`中配置：

```yaml
openclaw:
  base-url: http://127.0.0.1:18789
  token: your-secret-token
  hooks-path: /hooks
  
  hooks:
    enabled: true
    token: webhook-shared-secret
    path: /hooks
```

### 认证方式

客户端支持三种Webhook认证方式：

1. **Authorization头部** (推荐): `Authorization: Bearer <token>`
2. **x-openclaw-token头部**: `x-openclaw-token: <token>`
3. **Query参数**: `?token=<token>` (已弃用)

### 映射配置

配置自定义Webhook映射：

```yaml
hooks:
  enabled: true
  token: webhook-shared-secret
  mappings:
    github:
      action: agent
      name: GitHub
      message: "Process GitHub webhook event"
      wakeMode: now
      deliver: true
      channel: discord
    slack-event:
      action: agent
      name: SlackBot
      wakeMode: next-heartbeat
      deliver: true
      channel: slack
    custom-webhook:
      action: wake
      wakeMode: now
```

映射属性说明：

| 属性 | 描述 |
|------|------|
| `action` | 操作类型：`agent` 或 `wake` |
| `name` | 智能体名称 |
| `message` | 智能体消息 |
| `sessionKey` | 会话键 |
| `wakeMode` | 唤醒模式：`now` 或 `next-heartbeat` |
| `deliver` | 是否投递响应 |
| `channel` | 投递渠道：`last`、`whatsapp`、`telegram`、`discord`、`slack`等 |
| `to` | 接收者标识符 |
| `model` | 模型覆盖 |
| `thinking` | 思考级别：`low`、`medium`、`high` |
| `timeoutSeconds` | 超时时间(秒) |
| `allowUnsafeExternalContent` | 允许不受信任的外部内容 |

### 预设

启用内置预设：

```yaml
hooks:
  enabled: true
  presets:
    - gmail
```

### 转换目录

```yaml
hooks:
  enabled: true
  transforms-dir: ./transforms
```

## 配置属性

| 属性 | 默认值 | 描述 |
|------|--------|------|
| `base-url` | http://127.0.0.1:18789 | OpenClaw服务器地址 |
| `token` | - | Webhook认证令牌 |
| `hooks-path` | /hooks | Hook端点路径 |
| `hooks.enabled` | false | 是否启用Webhook |
| `hooks.token` | - | Webhook共享密钥 |
| `hooks.path` | /hooks | Webhook路径 |
| `default-timeout-seconds` | 120 | 默认超时时间 |
| `connect-timeout-ms` | 10000 | 连接超时 |
| `read-timeout-ms` | 30000 | 读取超时 |

## 多实例支持

### 配置多个OpenClaw实例

```yaml
openclaw:
  multi-instances:
    enabled: true
    default-instance: local
    instances:
      local:
        base-url: http://127.0.0.1:18789
        token: local-token
        enabled: true
        hooks:
          enabled: true
          token: webhook-token
      production:
        base-url: https://openclaw.example.com
        token: prod-token
        enabled: true
      staging:
        base-url: https://staging.openclaw.example.com
        token: staging-token
        enabled: false
```

### 使用多实例

```java
MultiOpenClawProperties multiProps = new MultiOpenClawProperties();

// 配置实例
MultiOpenClawProperties.InstanceConfig localConfig = new MultiOpenClawProperties.InstanceConfig();
localConfig.setBaseUrl("http://127.0.0.1:18789");
localConfig.setToken("token");

MultiOpenClawProperties.InstanceConfig prodConfig = new MultiOpenClawProperties.InstanceConfig();
prodConfig.setBaseUrl("https://openclaw.example.com");
prodConfig.setToken("prod-token");

Map<String, MultiOpenClawProperties.InstanceConfig> instances = new HashMap<>();
instances.put("local", localConfig);
instances.put("production", prodConfig);

multiProps.setInstances(instances);
multiProps.setDefaultInstance("local");

// 创建管理器
OpenClawClientManager manager = new OpenClawClientManager(multiProps);

// 使用指定实例
OpenClawClient localClient = manager.getClient("local");
OpenClawClient prodClient = manager.getClient("production");

// 使用默认实例
manager.getClient().runAgent("message");
```

## 批处理请求

### 串行请求

```java
OpenClawClient client = new OpenClawClient(baseUrl, token);
BatchRequestService batchService = new BatchRequestService(client);

List<String> messages = Arrays.asList(
    "Task 1",
    "Task 2",
    "Task 3"
);

// 串行wake请求
List<OpenClawResponse> responses = batchService.sendSerialWake(messages);

// 或使用AgentRequest
List<AgentRequest> requests = Arrays.asList(
    AgentRequest.builder().message("Task 1").name("Agent").build(),
    AgentRequest.builder().message("Task 2").name("Agent").build()
);

BatchRequestService.BatchResult result = batchService.sendSerialWithResult(requests);
System.out.println(result); // BatchResult{total=2, success=2, failed=0}
```

### 并行请求

```java
// 并行wake请求
List<OpenClawResponse> responses = batchService.sendParallelWake(messages);

// 并行agent请求
BatchRequestService.BatchResult result = batchService.sendParallelWithResult(requests);
```

### 自定义并行请求

```java
List<OpenClawResponse> responses = batchService.sendParallelCustom(items, item -> {
    return client.runAgent(item.getMessage(), item.getAgent());
});
```

### BatchResult

```java
BatchRequestService.BatchResult result = batchService.sendParallelWithResult(requests);

result.getTotal();     // 总数
result.getSuccess();   // 成功数
result.getFailed();    // 失败数
result.isAllSuccess(); // 是否全部成功
result.getResponses(); // 响应列表
```

## WebSocket API 参考

### OpenClawWsClient

```java
// 创建客户端
OpenClawWsClient wsClient = new OpenClawWsClient(baseUrl, token);

// 设置是否需要设备认证（开发环境设为false）
wsClient.setRequireDevice(false);

// 连接网关
boolean connected = wsClient.connect();

// 健康检查
WsResponse health = wsClient.health();

// 状态查询
WsResponse status = wsClient.status();

// 同步执行Agent
AgentResult result = wsClient.runAgent(message);
AgentResult result = wsClient.runAgent(message, agentId);
AgentResult result = wsClient.runAgent(message, agentId, deliver, timeoutMs);

// 发送消息
wsClient.sendMessage(target, message);

// 发送系统事件
wsClient.systemEvent(text);

// 添加事件监听器
wsClient.addEventListener(listener);

// 移除事件监听器
wsClient.removeEventListener(listener);

// 检查连接状态
wsClient.isConnected();

// 关闭连接
wsClient.close();
```

### AsyncAgentService

```java
// 创建异步服务
AsyncAgentService asyncService = new AsyncAgentService(wsClient);

// 异步执行Agent
CompletableFuture<AgentResult> future = asyncService.runAgentAsync(message);
CompletableFuture<AgentResult> future = asyncService.runAgentAsync(message, agentId);

// 注册回调
asyncService.registerCallback(runId, callback);

// 注销回调
asyncService.unregisterCallback(runId);
```

### AgentResult

```java
AgentResult result = wsClient.runAgent("message", "main");

result.getRunId();      // 获取RunId
result.getStatus();     // 获取状态 (accepted, ok, error)
result.getSummary();    // 获取Agent回复
result.getError();      // 获取错误信息

result.isAccepted();     // 是否已接受
result.isOk();          // 是否成功
result.isError();       // 是否有错误
```

### WsEventListener

```java
wsClient.addEventListener(new WsEventListener() {
    @Override
    public void onEvent(String event, Map<String, Object> payload) {
        // 通用事件处理
    }
    
    @Override
    public void onAgentEvent(String runId, String status, Map<String, Object> payload) {
        // Agent事件 (start, progress, end, error)
    }
    
    @Override
    public void onChatEvent(String runId, String sessionKey, String state, Map<String, Object> message) {
        // Chat事件 (delta, final)
    }
    
    @Override
    public void onPresenceUpdate(Map<String, Object> presence) {
        // 在线状态更新
    }
    
    @Override
    public void onTick() {
        // 心跳 tick
    }
    
    @Override
    public void onError(Throwable t) {
        // 错误处理
    }
});
```

### 会话键 (sessionKey)

OpenClaw 使用 `sessionKey` 标识会话，格式如下：

| 类型 | 格式 | 说明 |
|------|------|------|
| 主会话 | `agent:<agentId>:main` | 直接聊天 |
| 私信 | `agent:<agentId>:dm:<peerId>` | 按发送者隔离 |
| 群组 | `agent:<agentId>:<channel>:group:<id>` | 群组聊天 |
| Webhook | `agent:<agentId>:hook:<uuid>` | Webhook触发 |
| 定时任务 | `agent:<agentId>:cron:<jobId>` | 定时任务 |

## 未来优化方向

### 高优先级

- [ ] **流式响应支持** - 实现 Server-Sent Events (SSE) 流式读取 agent 输出
- [ ] **连接自动重连** - WebSocket 断开后自动重连机制
- [ ] **消息队列** - 支持消息持久化和可靠投递

### 中优先级

- [ ] **会话历史查询** - 通过 `sessions.list` 和 `sessions.history` API 查询历史会话
- [ ] **多代理并发** - 支持同时运行多个 agent 实例
- [ ] **代理工具目录** - 通过 `tools.catalog` API 获取可用工具列表

### 低优先级

- [ ] **TLS/SSL 支持** - WebSocket TLS 加密连接
- [ ] **代理隧道** - 支持通过代理服务器连接
- [ ] **指标监控** - 连接状态、请求延迟等 metrics 收集
- [ ] **Spring Boot Starter** - 自动配置支持

## 构建

```bash
mvn clean package
```

## 测试

```bash
mvn test
```

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 相关链接

- [OpenClaw 官方文档](https://docs.openclaw.ai)
- [OpenClaw GitHub](https://github.com/openwebf/openclaw)
- [Gateway Protocol](https://docs.openclaw.ai/gateway/protocol)
