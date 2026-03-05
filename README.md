# OpenClaw Java Client

Java客户端包，用于通过Webhook调用OpenClaw自动化平台。

## 功能特性

- 支持Wake端点触发心跳
- 支持Agent端点运行智能体
- 支持定时任务配置
- 支持YAML配置文件
- 兼容Spring Boot

## 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>ai.openclaw</groupId>
    <artifactId>openclaw-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 基本用法

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

### 使用配置

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

## 构建

```bash
mvn clean package
```

## 测试

```bash
mvn test
```
