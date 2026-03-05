package ai.openclaw.client.scheduler;

import ai.openclaw.client.OpenClawClient;
import ai.openclaw.client.config.OpenClawProperties;
import ai.openclaw.client.model.AgentRequest;
import ai.openclaw.client.model.OpenClawResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class OpenClawTaskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OpenClawTaskScheduler.class);

    private final OpenClawClient openClawClient;
    private final Map<String, TaskConfig> tasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();
    private final AtomicBoolean started = new AtomicBoolean(false);

    public OpenClawTaskScheduler(OpenClawClient openClawClient) {
        this.openClawClient = openClawClient;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "openclaw-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public void addTask(String name, TaskConfig config) {
        tasks.put(name, config);
        logger.info("Added task: {} with cron: {}", name, config.getCronExpression());
        
        if (started.get()) {
            scheduleTask(name, config);
        }
    }

    public void removeTask(String name) {
        TaskConfig removed = tasks.remove(name);
        ScheduledFuture<?> future = scheduledFutures.remove(name);
        
        if (future != null) {
            future.cancel(false);
        }
        
        if (removed != null) {
            logger.info("Removed task: {}", name);
        }
    }

    public void start() {
        if (started.compareAndSet(false, true)) {
            logger.info("Starting OpenClaw Task Scheduler");
            
            for (Map.Entry<String, TaskConfig> entry : tasks.entrySet()) {
                scheduleTask(entry.getKey(), entry.getValue());
            }
        }
    }

    public void stop() {
        if (started.compareAndSet(true, false)) {
            logger.info("Stopping OpenClaw Task Scheduler");
            
            for (Map.Entry<String, ScheduledFuture<?>> entry : scheduledFutures.entrySet()) {
                entry.getValue().cancel(false);
            }
            scheduledFutures.clear();
            scheduler.shutdown();
            
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void scheduleTask(String name, TaskConfig config) {
        if (!config.isEnabled()) {
            logger.info("Task {} is disabled, skipping", name);
            return;
        }

        CronExpression cron;
        try {
            cron = new CronExpression(config.getCronExpression());
        } catch (Exception e) {
            logger.error("Invalid cron expression for task {}: {}", name, config.getCronExpression());
            return;
        }

        Runnable task = () -> {
            logger.info("Executing scheduled task: {}", name);
            executeTask(name, config);
        };

        long initialDelay = calculateInitialDelay(cron);
        long period = 60000;

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                task,
                initialDelay,
                period,
                TimeUnit.MILLISECONDS
        );

        scheduledFutures.put(name, future);
        logger.info("Scheduled task: {} with initial delay: {}ms", name, initialDelay);
    }

    private long calculateInitialDelay(CronExpression cron) {
        Date next = cron.getNextValidTimeAfter(new Date());
        if (next != null) {
            return Math.max(0, next.getTime() - System.currentTimeMillis());
        }
        return 60000;
    }

    public void executeTaskNow(String name) {
        TaskConfig config = tasks.get(name);
        if (config != null) {
            executeTask(name, config);
        } else {
            logger.warn("Task not found: {}", name);
        }
    }

    private void executeTask(String name, TaskConfig config) {
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
                logger.info("Task '{}' executed successfully", name);
            } else {
                logger.error("Task '{}' failed: {}", name, response.getError());
            }
        } catch (Exception e) {
            logger.error("Error executing task '{}': {}", name, e.getMessage(), e);
        }
    }

    public Map<String, TaskConfig> getTasks() {
        return new HashMap<>(tasks);
    }

    public boolean isStarted() {
        return started.get();
    }

    public static class TaskConfig {
        private String cronExpression;
        private String message;
        private String agentName;
        private String sessionKey;
        private String wakeMode = "now";
        private boolean enabled = true;
        private String model;
        private String thinking;
        private Integer timeoutSeconds;
        private String channel;
        private String to;

        public String getCronExpression() {
            return cronExpression;
        }

        public void setCronExpression(String cronExpression) {
            this.cronExpression = cronExpression;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getAgentName() {
            return agentName;
        }

        public void setAgentName(String agentName) {
            this.agentName = agentName;
        }

        public String getSessionKey() {
            return sessionKey;
        }

        public void setSessionKey(String sessionKey) {
            this.sessionKey = sessionKey;
        }

        public String getWakeMode() {
            return wakeMode;
        }

        public void setWakeMode(String wakeMode) {
            this.wakeMode = wakeMode;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getThinking() {
            return thinking;
        }

        public void setThinking(String thinking) {
            this.thinking = thinking;
        }

        public Integer getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final TaskConfig config = new TaskConfig();

            public Builder cron(String cronExpression) {
                config.cronExpression = cronExpression;
                return this;
            }

            public Builder message(String message) {
                config.message = message;
                return this;
            }

            public Builder agentName(String agentName) {
                config.agentName = agentName;
                return this;
            }

            public Builder sessionKey(String sessionKey) {
                config.sessionKey = sessionKey;
                return this;
            }

            public Builder wakeMode(String wakeMode) {
                config.wakeMode = wakeMode;
                return this;
            }

            public Builder enabled(boolean enabled) {
                config.enabled = enabled;
                return this;
            }

            public Builder model(String model) {
                config.model = model;
                return this;
            }

            public Builder thinking(String thinking) {
                config.thinking = thinking;
                return this;
            }

            public Builder timeoutSeconds(Integer timeoutSeconds) {
                config.timeoutSeconds = timeoutSeconds;
                return this;
            }

            public Builder channel(String channel) {
                config.channel = channel;
                return this;
            }

            public Builder to(String to) {
                config.to = to;
                return this;
            }

            public TaskConfig build() {
                return config;
            }
        }
    }

    public static class CronExpression {
        private final String expression;

        public CronExpression(String expression) throws Exception {
            this.expression = expression;
            validate();
        }

        private void validate() throws Exception {
            String[] parts = expression.trim().split("\\s+");
            if (parts.length < 5 || parts.length > 6) {
                throw new Exception("Invalid cron expression");
            }
        }

        public Date getNextValidTimeAfter(Date date) {
            return new Date(date.getTime() + 60000);
        }
    }
}
