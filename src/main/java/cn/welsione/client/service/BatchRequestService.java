package cn.welsione.client.service;

import cn.welsione.client.OpenClawClient;
import cn.welsione.client.model.AgentRequest;
import cn.welsione.client.model.OpenClawResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

public class BatchRequestService {

    private static final Logger logger = LoggerFactory.getLogger(BatchRequestService.class);

    private final OpenClawClient client;
    private final ExecutorService executorService;

    public BatchRequestService(OpenClawClient client) {
        this.client = client;
        this.executorService = Executors.newFixedThreadPool(10);
    }

    public BatchRequestService(OpenClawClient client, int threadPoolSize) {
        this.client = client;
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    public List<OpenClawResponse> sendSerial(List<AgentRequest> requests) {
        List<OpenClawResponse> responses = new ArrayList<>();
        
        for (AgentRequest request : requests) {
            try {
                OpenClawResponse response = client.runAgent(request);
                responses.add(response);
                logger.debug("Serial request completed, status: {}", response.getStatusCode());
            } catch (Exception e) {
                logger.error("Serial request failed: {}", e.getMessage());
                responses.add(new OpenClawResponse(false, -1, null, e.getMessage()));
            }
        }
        
        return responses;
    }

    public List<OpenClawResponse> sendSerialWake(List<String> texts) {
        return sendSerialWake(texts, "now");
    }

    public List<OpenClawResponse> sendSerialWake(List<String> texts, String mode) {
        List<OpenClawResponse> responses = new ArrayList<>();
        
        for (String text : texts) {
            try {
                OpenClawResponse response = client.wake(text, mode);
                responses.add(response);
            } catch (Exception e) {
                logger.error("Serial wake failed: {}", e.getMessage());
                responses.add(new OpenClawResponse(false, -1, null, e.getMessage()));
            }
        }
        
        return responses;
    }

    public List<OpenClawResponse> sendParallel(List<AgentRequest> requests) {
        List<Future<OpenClawResponse>> futures = new ArrayList<>();
        
        for (AgentRequest request : requests) {
            Future<OpenClawResponse> future = executorService.submit(() -> {
                return client.runAgent(request);
            });
            futures.add(future);
        }
        
        return collectResponses(futures);
    }

    public List<OpenClawResponse> sendParallelWake(List<String> texts) {
        return sendParallelWake(texts, "now");
    }

    public List<OpenClawResponse> sendParallelWake(List<String> texts, String mode) {
        List<Future<OpenClawResponse>> futures = new ArrayList<>();
        
        for (String text : texts) {
            Future<OpenClawResponse> future = executorService.submit(() -> {
                return client.wake(text, mode);
            });
            futures.add(future);
        }
        
        return collectResponses(futures);
    }

    public <T> List<OpenClawResponse> sendParallelCustom(List<T> items, Function<T, OpenClawResponse> requestFunction) {
        List<Future<OpenClawResponse>> futures = new ArrayList<>();
        
        for (T item : items) {
            Future<OpenClawResponse> future = executorService.submit(() -> {
                return requestFunction.apply(item);
            });
            futures.add(future);
        }
        
        return collectResponses(futures);
    }

    private List<OpenClawResponse> collectResponses(List<Future<OpenClawResponse>> futures) {
        List<OpenClawResponse> responses = new ArrayList<>();
        
        for (Future<OpenClawResponse> future : futures) {
            try {
                OpenClawResponse response = future.get(60, TimeUnit.SECONDS);
                responses.add(response);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                responses.add(new OpenClawResponse(false, -1, null, "Interrupted"));
            } catch (ExecutionException e) {
                logger.error("Parallel request execution failed: {}", e.getMessage());
                responses.add(new OpenClawResponse(false, -1, null, e.getMessage()));
            } catch (TimeoutException e) {
                logger.error("Parallel request timeout: {}", e.getMessage());
                responses.add(new OpenClawResponse(false, -1, null, "Timeout"));
            }
        }
        
        return responses;
    }

    public BatchResult sendSerialWithResult(List<AgentRequest> requests) {
        List<OpenClawResponse> responses = sendSerial(requests);
        return buildResult(responses);
    }

    public BatchResult sendParallelWithResult(List<AgentRequest> requests) {
        List<OpenClawResponse> responses = sendParallel(requests);
        return buildResult(responses);
    }

    private BatchResult buildResult(List<OpenClawResponse> responses) {
        int total = responses.size();
        long success = responses.stream().filter(OpenClawResponse::isSuccess).count();
        long failed = total - success;
        
        return new BatchResult(total, (int) success, (int) failed, responses);
    }

    public void close() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class BatchResult {
        private final int total;
        private final int success;
        private final int failed;
        private final List<OpenClawResponse> responses;

        public BatchResult(int total, int success, int failed, List<OpenClawResponse> responses) {
            this.total = total;
            this.success = success;
            this.failed = failed;
            this.responses = Collections.unmodifiableList(responses);
        }

        public int getTotal() {
            return total;
        }

        public int getSuccess() {
            return success;
        }

        public int getFailed() {
            return failed;
        }

        public List<OpenClawResponse> getResponses() {
            return responses;
        }

        public boolean isAllSuccess() {
            return failed == 0;
        }

        @Override
        public String toString() {
            return String.format("BatchResult{total=%d, success=%d, failed=%d}", total, success, failed);
        }
    }
}
