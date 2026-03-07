package cn.welsione.client;

import cn.welsione.client.config.OpenClawProperties;
import cn.welsione.client.model.AgentRequest;
import cn.welsione.client.model.OpenClawResponse;
import cn.welsione.client.scheduler.OpenClawTaskScheduler;

public class OpenClawExample {

    public static void main(String[] args) {
        example1_BasicClient();
        example2_ClientWithConfiguration();
        example3_ScheduledTasks();
        example4_AdvancedAgentRequest();
    }

    public static void example1_BasicClient() {
        System.out.println("=== Example 1: Basic Client ===");
        
        OpenClawClient client = new OpenClawClient(
                "http://127.0.0.1:18789",
                "your-secret-token"
        );

        OpenClawResponse wakeResponse = client.wake("System line message");
        System.out.println("Wake response: " + wakeResponse);

        OpenClawResponse messageResponse = client.sendMessage("Hello, agent!");
        System.out.println("Message response: " + messageResponse);

        client.close();
    }

    public static void example2_ClientWithConfiguration() {
        System.out.println("\n=== Example 2: Client with Configuration ===");
        
        OpenClawProperties properties = new OpenClawProperties();
        properties.setBaseUrl("http://127.0.0.1:18789");
        properties.setToken("your-secret-token");
        properties.setHooksPath("/hooks");
        properties.setDefaultTimeoutSeconds(120);

        OpenClawClient client = new OpenClawClient(properties);

        OpenClawResponse response = client.runAgent(
                "Summarize my inbox",
                "EmailAgent",
                "session-123"
        );

        System.out.println("Agent response: " + response);

        client.close();
    }

    public static void example3_ScheduledTasks() {
        System.out.println("\n=== Example 3: Scheduled Tasks ===");
        
        OpenClawClient client = new OpenClawClient(
                "http://127.0.0.1:18789",
                "your-secret-token"
        );

        OpenClawTaskScheduler scheduler = new OpenClawTaskScheduler(client);

        scheduler.addTask("daily-summary", 
                OpenClawTaskScheduler.TaskConfig.builder()
                        .cron("0 0 9 * * ?")
                        .message("Give me a summary of my day")
                        .agentName("DailySummary")
                        .enabled(true)
                        .build()
        );

        scheduler.addTask("check-emails",
                OpenClawTaskScheduler.TaskConfig.builder()
                        .cron("0 */30 * * * ?")
                        .message("Check for new emails")
                        .agentName("EmailChecker")
                        .wakeMode("next-heartbeat")
                        .enabled(true)
                        .build()
        );

        scheduler.addTask("weekly-report",
                OpenClawTaskScheduler.TaskConfig.builder()
                        .cron("0 0 10 ? * MON")
                        .message("Generate weekly report")
                        .agentName("ReportGenerator")
                        .model("openai/gpt-5.2-mini")
                        .timeoutSeconds(300)
                        .channel("whatsapp")
                        .to("+15551234567")
                        .enabled(true)
                        .build()
        );

        scheduler.start();

        System.out.println("Scheduler started, press Ctrl+C to stop");
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            scheduler.stop();
            client.close();
        }));

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        scheduler.executeTaskNow("daily-summary");

        scheduler.stop();
        client.close();
    }

    public static void example4_AdvancedAgentRequest() {
        System.out.println("\n=== Example 4: Advanced Agent Request ===");
        
        OpenClawClient client = new OpenClawClient(
                "http://127.0.0.1:18789",
                "your-secret-token"
        );

        AgentRequest request = AgentRequest.builder()
                .message("Analyze the latest sales data and create a report")
                .name("SalesAnalyzer")
                .sessionKey("sales-session-001")
                .wakeMode("now")
                .deliver(true)
                .channel("discord")
                .to("sales-channel-id")
                .model("openai/gpt-5.2-mini")
                .thinking("high")
                .timeoutSeconds(180)
                .build();

        OpenClawResponse response = client.runAgent(request);
        System.out.println("Advanced agent response: " + response);

        client.close();
    }
}
