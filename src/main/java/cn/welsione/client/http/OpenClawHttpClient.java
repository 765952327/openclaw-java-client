package cn.welsione.client.http;

import cn.welsione.client.config.OpenClawProperties;
import cn.welsione.client.model.OpenClawResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OpenClawHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenClawHttpClient.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String OPENCLAW_TOKEN_HEADER = "x-openclaw-token";
    private static final String TOKEN_QUERY_PARAM = "token";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenClawProperties properties;

    public OpenClawHttpClient(OpenClawProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(properties.getReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .writeTimeout(properties.getReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
    }

    private Request.Builder createRequestBuilder(String url) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json");
        
        String token = getEffectiveToken();
        if (token != null && !token.isEmpty()) {
            builder.addHeader(AUTHORIZATION_HEADER, "Bearer " + token);
            builder.addHeader(OPENCLAW_TOKEN_HEADER, token);
        }
        
        return builder;
    }

    private String getEffectiveToken() {
        if (properties.getHooksToken() != null && !properties.getHooksToken().isEmpty()) {
            return properties.getHooksToken();
        }
        return properties.getToken();
    }

    private String buildUrlWithToken(String baseUrl) {
        String token = getEffectiveToken();
        if (token != null && !token.isEmpty()) {
            String separator = baseUrl.contains("?") ? "&" : "?";
            return baseUrl + separator + TOKEN_QUERY_PARAM + "=" + token;
        }
        return baseUrl;
    }

    public OpenClawResponse post(String endpoint, Object body) {
        String url = properties.getFullWebhookUrl(endpoint);
        
        return executePost(url, endpoint, body);
    }

    public OpenClawResponse postWithTokenInQuery(String endpoint, Object body) {
        String baseUrl = properties.getFullWebhookUrl(endpoint);
        String url = buildUrlWithToken(baseUrl);
        
        return executePost(url, endpoint, body);
    }

    private OpenClawResponse executePost(String url, String endpoint, Object body) {
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            logger.debug("POST {} with body: {}", url, jsonBody);
            
            RequestBody requestBody = RequestBody.create(
                    jsonBody,
                    MediaType.get("application/json; charset=utf-8")
            );
            
            Request request = createRequestBuilder(url)
                    .post(requestBody)
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : null;
                
                if (response.isSuccessful()) {
                    logger.info("Request to {} successful, status: {}", endpoint, response.code());
                    return new OpenClawResponse(true, response.code(), responseBody);
                } else {
                    logger.warn("Request to {} failed, status: {}, body: {}", endpoint, response.code(), responseBody);
                    return new OpenClawResponse(false, response.code(), null, responseBody);
                }
            }
        } catch (IOException e) {
            logger.error("Request to {} failed: {}", endpoint, e.getMessage());
            return new OpenClawResponse(false, -1, null, e.getMessage());
        }
    }

    public <T> T post(String endpoint, Object body, Class<T> responseClass) {
        OpenClawResponse response = post(endpoint, body);
        if (response.isSuccess() && response.getBody() != null) {
            try {
                return objectMapper.readValue(response.getBody(), responseClass);
            } catch (IOException e) {
                logger.error("Failed to parse response: {}", e.getMessage());
            }
        }
        return null;
    }

    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
