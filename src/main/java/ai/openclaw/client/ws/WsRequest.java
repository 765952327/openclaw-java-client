package ai.openclaw.client.ws;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsRequest {

    @JsonProperty("type")
    private String type = "req";

    @JsonProperty("id")
    private String id;

    @JsonProperty("method")
    private String method;

    @JsonProperty("params")
    private Map<String, Object> params;

    public WsRequest() {
    }

    public WsRequest(String method, Map<String, Object> params) {
        this.method = method;
        this.params = params;
    }

    public static WsRequest connect(ConnectParams params) {
        WsRequest request = new WsRequest();
        request.setId(java.util.UUID.randomUUID().toString());
        request.setMethod("connect");
        request.setParams(params.toMap());
        return request;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public static class ConnectParams {
        @JsonProperty("minProtocol")
        private int minProtocol = 3;

        @JsonProperty("maxProtocol")
        private int maxProtocol = 3;

        @JsonProperty("client")
        private ClientInfo client;

        @JsonProperty("role")
        private String role = "operator";

        @JsonProperty("scopes")
        private List<String> scopes = List.of("operator.read", "operator.write");

        @JsonProperty("auth")
        private Map<String, String> auth;

        @JsonProperty("locale")
        private String locale = "en-US";

        @JsonProperty("userAgent")
        private String userAgent = "java-openclaw-client/1.0.0";

        @JsonProperty("device")
        private Map<String, Object> device;

        public ConnectParams() {
            this.client = new ClientInfo();
        }

        public ConnectParams withoutDevice() {
            this.device = null;
            return this;
        }

    public ConnectParams(String token) {
        this();
        if (token != null && !token.isEmpty()) {
            this.auth = Map.of("token", token);
        }
    }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("minProtocol", minProtocol);
            map.put("maxProtocol", maxProtocol);
            map.put("client", client);
            map.put("role", role);
            map.put("scopes", scopes);
            if (auth != null) {
                map.put("auth", auth);
            }
            if (device != null && !device.isEmpty()) {
                map.put("device", device);
            }
            map.put("locale", locale);
            map.put("userAgent", userAgent);
            return map;
        }

        public int getMinProtocol() {
            return minProtocol;
        }

        public void setMinProtocol(int minProtocol) {
            this.minProtocol = minProtocol;
        }

        public int getMaxProtocol() {
            return maxProtocol;
        }

        public void setMaxProtocol(int maxProtocol) {
            this.maxProtocol = maxProtocol;
        }

        public ClientInfo getClient() {
            return client;
        }

        public void setClient(ClientInfo client) {
            this.client = client;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }

        public Map<String, String> getAuth() {
            return auth;
        }

        public void setAuth(Map<String, String> auth) {
            this.auth = auth;
        }

        public String getLocale() {
            return locale;
        }

        public void setLocale(String locale) {
            this.locale = locale;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }
    }

    public static class ClientInfo {
        @JsonProperty("id")
        private String id = "cli";

        @JsonProperty("version")
        private String version = "1.0.0";

        @JsonProperty("platform")
        private String platform = "macos";

        @JsonProperty("mode")
        private String mode = "cli";

        @JsonProperty("instanceId")
        private String instanceId;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }
    }
}
