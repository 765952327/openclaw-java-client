package ai.openclaw.client.ws;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MessageSignerTest {

    @Test
    public void testSignAndVerify() {
        String secret = "test-secret";
        MessageSigner signer = new MessageSigner(secret);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Hello");
        payload.put("timestamp", System.currentTimeMillis());
        
        String signature = signer.sign(payload);
        assertNotNull(signature);
        
        boolean verified = signer.verify(payload, signature);
        assertTrue(verified);
    }

    @Test
    public void testMd5() {
        String input = "test";
        String hash = MessageSigner.md5(input);
        
        assertNotNull(hash);
        assertEquals(32, hash.length());
    }

    @Test
    public void testSha256() {
        String input = "test";
        String hash = MessageSigner.sha256(input);
        
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    public void testVerifyFailure() {
        String secret = "test-secret";
        MessageSigner signer = new MessageSigner(secret);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Hello");
        
        String wrongSignature = "wrong-signature";
        boolean verified = signer.verify(payload, wrongSignature);
        
        assertFalse(verified);
    }
}
