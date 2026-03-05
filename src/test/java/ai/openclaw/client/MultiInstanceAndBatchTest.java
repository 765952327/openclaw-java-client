package ai.openclaw.client;

import ai.openclaw.client.config.MultiOpenClawProperties;
import ai.openclaw.client.config.OpenClawProperties;
import ai.openclaw.client.model.AgentRequest;
import ai.openclaw.client.model.OpenClawResponse;
import ai.openclaw.client.service.BatchRequestService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class MultiInstanceAndBatchTest {

    private OpenClawClientManager manager;
    private BatchRequestService batchService;

    @Before
    public void setUp() {
        MultiOpenClawProperties multiProps = new MultiOpenClawProperties();
        
        MultiOpenClawProperties.InstanceConfig localConfig = new MultiOpenClawProperties.InstanceConfig();
        localConfig.setBaseUrl("http://127.0.0.1:18789");
        localConfig.setToken("gmail_hook_2026");
        localConfig.setEnabled(true);
        
        MultiOpenClawProperties.InstanceConfig prodConfig = new MultiOpenClawProperties.InstanceConfig();
        prodConfig.setBaseUrl("https://openclaw.example.com");
        prodConfig.setToken("prod-token");
        prodConfig.setEnabled(true);
        
        java.util.Map<String, MultiOpenClawProperties.InstanceConfig> instances = new java.util.HashMap<>();
        instances.put("local", localConfig);
        instances.put("production", prodConfig);
        
        multiProps.setInstances(instances);
        multiProps.setDefaultInstance("local");
        
        manager = new OpenClawClientManager(multiProps);
        
        OpenClawClient client = manager.getClient("local");
        batchService = new BatchRequestService(client, 5);
    }

    @After
    public void tearDown() {
        if (batchService != null) {
            batchService.close();
        }
        if (manager != null) {
            manager.close();
        }
    }

    @Test
    public void testMultiInstanceConfig() {
        System.out.println("\n=== Testing Multi Instance Config ===");
        
        assertTrue(manager.hasInstance("local"));
        assertTrue(manager.hasInstance("production"));
        assertFalse(manager.hasInstance("nonexistent"));
        
        OpenClawClient localClient = manager.getClient("local");
        assertEquals("http://127.0.0.1:18789", localClient.getProperties().getBaseUrl());
        
        OpenClawClient prodClient = manager.getClient("production");
        assertEquals("https://openclaw.example.com", prodClient.getProperties().getBaseUrl());
        
        System.out.println("Local baseUrl: " + localClient.getProperties().getBaseUrl());
        System.out.println("Production baseUrl: " + prodClient.getProperties().getBaseUrl());
    }

    @Test
    public void testDefaultInstance() {
        System.out.println("\n=== Testing Default Instance ===");
        
        OpenClawClient defaultClient = manager.getClient();
        assertEquals("http://127.0.0.1:18789", defaultClient.getProperties().getBaseUrl());
        
        System.out.println("Default instance baseUrl: " + defaultClient.getProperties().getBaseUrl());
    }

    @Test
    public void testSerialWake() {
        System.out.println("\n=== Testing Serial Wake ===");
        
        List<String> texts = Arrays.asList(
            "Test message 1",
            "Test message 2", 
            "Test message 3"
        );
        
        List<OpenClawResponse> responses = batchService.sendSerialWake(texts);
        
        System.out.println("Total requests: " + responses.size());
        long successCount = responses.stream().filter(OpenClawResponse::isSuccess).count();
        System.out.println("Successful: " + successCount);
        
        assertEquals(3, responses.size());
    }

    @Test
    public void testParallelWake() {
        System.out.println("\n=== Testing Parallel Wake ===");
        
        List<String> texts = Arrays.asList(
            "Parallel test 1",
            "Parallel test 2",
            "Parallel test 3"
        );
        
        List<OpenClawResponse> responses = batchService.sendParallelWake(texts);
        
        System.out.println("Total requests: " + responses.size());
        long successCount = responses.stream().filter(OpenClawResponse::isSuccess).count();
        System.out.println("Successful: " + successCount);
        
        assertEquals(3, responses.size());
    }

    @Test
    public void testSerialAgent() {
        System.out.println("\n=== Testing Serial Agent ===");
        
        List<AgentRequest> requests = Arrays.asList(
            AgentRequest.builder().message("Serial test 1").name("TestAgent").build(),
            AgentRequest.builder().message("Serial test 2").name("TestAgent").build(),
            AgentRequest.builder().message("Serial test 3").name("TestAgent").build()
        );
        
        BatchRequestService.BatchResult result = batchService.sendSerialWithResult(requests);
        
        System.out.println("Result: " + result);
        System.out.println("Total: " + result.getTotal());
        System.out.println("Success: " + result.getSuccess());
        System.out.println("Failed: " + result.getFailed());
        
        assertEquals(3, result.getTotal());
    }

    @Test
    public void testParallelAgent() {
        System.out.println("\n=== Testing Parallel Agent ===");
        
        List<AgentRequest> requests = Arrays.asList(
            AgentRequest.builder().message("Parallel test 1").name("TestAgent").build(),
            AgentRequest.builder().message("Parallel test 2").name("TestAgent").build(),
            AgentRequest.builder().message("Parallel test 3").name("TestAgent").build()
        );
        
        BatchRequestService.BatchResult result = batchService.sendParallelWithResult(requests);
        
        System.out.println("Result: " + result);
        System.out.println("Total: " + result.getTotal());
        System.out.println("Success: " + result.getSuccess());
        
        assertEquals(3, result.getTotal());
    }

    @Test
    public void testBatchResult() {
        System.out.println("\n=== Testing Batch Result ===");
        
        List<AgentRequest> requests = Arrays.asList(
            AgentRequest.builder().message("Test 1").name("TestAgent").build(),
            AgentRequest.builder().message("Test 2").name("TestAgent").build()
        );
        
        BatchRequestService.BatchResult result = batchService.sendParallelWithResult(requests);
        
        assertTrue(result.getTotal() > 0);
        assertNotNull(result.getResponses());
        assertEquals(result.getSuccess() + result.getFailed(), result.getTotal());
        
        System.out.println("Batch result: " + result);
    }
}
