package ai.openclaw.client.ws;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClientMetricsTest {

    private ClientMetrics metrics;

    @Before
    public void setUp() {
        metrics = new ClientMetrics();
    }

    @Test
    public void testRecordRequest() {
        metrics.recordRequest(100);
        assertEquals(1, metrics.getTotalRequests());
    }

    @Test
    public void testRecordSuccessAndFailure() {
        metrics.recordRequest(100);
        metrics.recordSuccess();
        metrics.recordRequest(200);
        metrics.recordFailure();
        
        assertEquals(2, metrics.getTotalRequests());
        assertEquals(1, metrics.getSuccessfulRequests());
        assertEquals(1, metrics.getFailedRequests());
    }

    @Test
    public void testAverageDuration() {
        metrics.recordRequest(100);
        metrics.recordSuccess();
        metrics.recordRequest(200);
        metrics.recordSuccess();
        
        assertEquals(150.0, metrics.getAverageRequestDurationMs(), 0.1);
    }

    @Test
    public void testQueueSize() {
        metrics.updateQueueSize(5);
        assertEquals(5, metrics.getCurrentQueueSize());
        
        metrics.updateQueueSize(10);
        assertEquals(10, metrics.getCurrentQueueSize());
        assertEquals(10, metrics.getMaxQueueSize());
    }

    @Test
    public void testSuccessRate() {
        metrics.recordRequest(100);
        metrics.recordSuccess();
        metrics.recordRequest(200);
        metrics.recordSuccess();
        metrics.recordRequest(300);
        metrics.recordFailure();
        
        assertEquals(66.67, metrics.getSuccessRate(), 0.1);
    }

    @Test
    public void testReset() {
        metrics.recordRequest(100);
        metrics.recordSuccess();
        metrics.updateQueueSize(10);
        
        metrics.reset();
        
        assertEquals(0, metrics.getTotalRequests());
        assertEquals(0, metrics.getSuccessfulRequests());
        assertEquals(0, metrics.getCurrentQueueSize());
    }

    @Test
    public void testToString() {
        metrics.recordRequest(100);
        metrics.recordSuccess();
        metrics.updateQueueSize(5);
        
        String str = metrics.toString();
        assertNotNull(str);
        assertTrue(str.contains("totalRequests=1"));
    }
}
