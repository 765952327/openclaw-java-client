package ai.openclaw.client.ws;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ClientCacheTest {

    @Test
    public void testPutAndGet() {
        ClientCache cache = new ClientCache(1000);
        
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));
        
        cache.put("key2", 123);
        assertEquals(123, cache.get("key2"));
        
        cache.shutdown();
    }

    @Test
    public void testExpiration() throws InterruptedException {
        ClientCache cache = new ClientCache(100);
        
        cache.put("key", "value");
        assertTrue(cache.contains("key"));
        
        Thread.sleep(150);
        
        assertFalse(cache.contains("key"));
        assertNull(cache.get("key"));
        
        cache.shutdown();
    }

    @Test
    public void testRemove() {
        ClientCache cache = new ClientCache(1000);
        
        cache.put("key", "value");
        assertTrue(cache.contains("key"));
        
        cache.remove("key");
        assertFalse(cache.contains("key"));
        
        cache.shutdown();
    }

    @Test
    public void testClear() {
        ClientCache cache = new ClientCache(1000);
        
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        
        assertEquals(2, cache.size());
        
        cache.clear();
        assertEquals(0, cache.size());
        
        cache.shutdown();
    }
}
