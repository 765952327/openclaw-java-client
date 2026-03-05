package ai.openclaw.client.scheduler;

import ai.openclaw.client.OpenClawClient;
import ai.openclaw.client.config.OpenClawProperties;
import ai.openclaw.client.model.AgentRequest;
import ai.openclaw.client.model.OpenClawResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OpenClawScheduler implements SchedulingConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(OpenClawScheduler.class);

    private final OpenClawClient openClawClient;
    private final OpenClawProperties properties;
    private final Map<String, ScheduledTaskHandle> activeTasks = new ConcurrentHashMap<>();

    public OpenClawScheduler(OpenClawClient openClawClient, OpenClawProperties properties) {
        this.openClawClient = openClawClient;
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Initializing OpenClaw Scheduler");
        Map<String, OpenClawProperties.ScheduledTaskConfig> tasks = properties.getScheduledTasks();
        
        if (tasks != null && !tasks.isEmpty()) {
            logger.info("Found {} scheduled tasks in configuration", tasks.size());
            for (Map.Entry<String, OpenClawProperties.ScheduledTaskConfig> entry : tasks.entrySet()) {
                String name = entry.getKey();
                OpenClawProperties.ScheduledTaskConfig config = entry.getValue();
                
                if (config.isEnabled()) {
                    registerTask(name, config);
                } else {
                    logger.info("Scheduled task '{}' is disabled", name);
                }
            }
        }
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        logger.info("Configuring scheduled tasks");
    }

    public void registerTask(String name, OpenClawProperties.ScheduledTaskConfig config) {
        if (config.getCron() == null || config.getCron().isEmpty()) {
            logger.warn("No cron expression provided for task: {}", name);
            return;
        }

        ScheduledTaskHandle handle = new ScheduledTaskHandle(name, config);
        activeTasks.put(name, handle);
        logger.info("Registered scheduled task: {} with cron: {}", name, config.getCron());
    }

    public void unregisterTask(String name) {
        ScheduledTaskHandle handle = activeTasks.remove(name);
        if (handle != null) {
            logger.info("Unregistered scheduled task: {}", name);
        }
    }

    @Scheduled(cron = "${openclaw.scheduler.cron:* * * * *}")
    public void executeScheduledTasks() {
        for (Map.Entry<String, ScheduledTaskHandle> entry : activeTasks.entrySet()) {
            String name = entry.getKey();
            ScheduledTaskHandle handle = entry.getValue();
            
            if (handle.shouldRun()) {
                executeTask(name, handle.getConfig());
                handle.updateLastRun();
            }
        }
    }

    public void executeTaskNow(String taskName) {
        ScheduledTaskHandle handle = activeTasks.get(taskName);
        if (handle != null) {
            executeTask(taskName, handle.getConfig());
            handle.updateLastRun();
        } else {
            logger.warn("Task not found: {}", taskName);
        }
    }

    private void executeTask(String name, OpenClawProperties.ScheduledTaskConfig config) {
        logger.info("Executing scheduled task: {}", name);
        
        try {
            AgentRequest.Builder builder = AgentRequest.builder()
                    .message(config.getMessage())
                    .name(config.getAgentName())
                    .sessionKey(config.getSessionKey())
                    .wakeMode(config.getWakeMode());

            if (config.getModel() != null) {
                builder.model(config.getModel());
            }
            if (config.getThinking() != null) {
                builder.thinking(config.getThinking());
            }
            if (config.getTimeoutSeconds() != null) {
                builder.timeoutSeconds(config.getTimeoutSeconds());
            }
            if (config.getChannel() != null) {
                builder.channel(config.getChannel());
            }
            if (config.getTo() != null) {
                builder.to(config.getTo());
            }

            OpenClawResponse response = openClawClient.runAgent(builder.build());
            
            if (response.isSuccess()) {
                logger.info("Scheduled task '{}' executed successfully", name);
            } else {
                logger.error("Scheduled task '{}' failed: {}", name, response.getError());
            }
        } catch (Exception e) {
            logger.error("Error executing scheduled task '{}': {}", name, e.getMessage(), e);
        }
    }

    public Map<String, ScheduledTaskHandle> getActiveTasks() {
        return activeTasks;
    }

    public static class ScheduledTaskHandle {
        private final String name;
        private final OpenClawProperties.ScheduledTaskConfig config;
        private volatile long lastRunTime;
        private volatile long nextRunTime;

        public ScheduledTaskHandle(String name, OpenClawProperties.ScheduledTaskConfig config) {
            this.name = name;
            this.config = config;
            this.lastRunTime = 0;
            this.nextRunTime = System.currentTimeMillis();
        }

        public boolean shouldRun() {
            return System.currentTimeMillis() >= nextRunTime;
        }

        public void updateLastRun() {
            this.lastRunTime = System.currentTimeMillis();
            this.nextRunTime = System.currentTimeMillis() + 60000;
        }

        public String getName() {
            return name;
        }

        public OpenClawProperties.ScheduledTaskConfig getConfig() {
            return config;
        }

        public long getLastRunTime() {
            return lastRunTime;
        }

        public long getNextRunTime() {
            return nextRunTime;
        }
    }
}
