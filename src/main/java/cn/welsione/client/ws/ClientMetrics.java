package cn.welsione.client.ws;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientMetrics {

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalRequestDurationMs = new AtomicLong(0);
    private final AtomicInteger currentQueueSize = new AtomicInteger(0);
    private final AtomicLong maxQueueSize = new AtomicLong(0);
    private final AtomicLong reconnections = new AtomicLong(0);
    private final AtomicLong healthCheckFailures = new AtomicLong(0);

    public void recordRequest(long durationMs) {
        totalRequests.incrementAndGet();
        totalRequestDurationMs.addAndGet(durationMs);
    }

    public void recordSuccess() {
        successfulRequests.incrementAndGet();
    }

    public void recordFailure() {
        failedRequests.incrementAndGet();
    }

    public void updateQueueSize(int size) {
        currentQueueSize.set(size);
        maxQueueSize.updateAndGet(current -> Math.max(current, size));
    }

    public void recordReconnection() {
        reconnections.incrementAndGet();
    }

    public void recordHealthCheckFailure() {
        healthCheckFailures.incrementAndGet();
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getSuccessfulRequests() {
        return successfulRequests.get();
    }

    public long getFailedRequests() {
        return failedRequests.get();
    }

    public double getAverageRequestDurationMs() {
        long total = totalRequests.get();
        return total > 0 ? (double) totalRequestDurationMs.get() / total : 0;
    }

    public int getCurrentQueueSize() {
        return currentQueueSize.get();
    }

    public long getMaxQueueSize() {
        return maxQueueSize.get();
    }

    public long getReconnections() {
        return reconnections.get();
    }

    public long getHealthCheckFailures() {
        return healthCheckFailures.get();
    }

    public double getSuccessRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) successfulRequests.get() / total * 100 : 0;
    }

    public void reset() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalRequestDurationMs.set(0);
        currentQueueSize.set(0);
        maxQueueSize.set(0);
        reconnections.set(0);
        healthCheckFailures.set(0);
    }

    @Override
    public String toString() {
        return String.format(
            "ClientMetrics{totalRequests=%d, successful=%d, failed=%d, avgDuration=%.2fms, " +
            "queueSize=%d, maxQueueSize=%d, reconnections=%d, healthCheckFailures=%d, successRate=%.2f%%}",
            totalRequests.get(), successfulRequests.get(), failedRequests.get(),
            getAverageRequestDurationMs(), currentQueueSize.get(), maxQueueSize.get(),
            reconnections.get(), healthCheckFailures.get(), getSuccessRate()
        );
    }
}
