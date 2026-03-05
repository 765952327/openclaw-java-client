package ai.openclaw.client.ws;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentResult {

    @JsonProperty("uid")
    private String uid;

    @JsonProperty("runId")
    private String runId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("error")
    private String error;

    @JsonProperty("messages")
    private List<Map<String, Object>> messages;

    @JsonProperty("events")
    private List<Map<String, Object>> events;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<Map<String, Object>> getMessages() {
        return messages;
    }

    public void setMessages(List<Map<String, Object>> messages) {
        this.messages = messages;
    }

    public List<Map<String, Object>> getEvents() {
        return events;
    }

    public void setEvents(List<Map<String, Object>> events) {
        this.events = events;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public boolean isAccepted() {
        return "accepted".equals(status);
    }

    public boolean isOk() {
        return "ok".equals(status);
    }

    public boolean isError() {
        return "error".equals(status);
    }

    @Override
    public String toString() {
        return "AgentResult{" +
                "uid='" + uid + '\'' +
                ", runId='" + runId + '\'' +
                ", status='" + status + '\'' +
                ", summary='" + summary + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
