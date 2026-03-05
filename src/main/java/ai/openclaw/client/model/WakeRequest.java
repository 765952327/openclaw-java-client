package ai.openclaw.client.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WakeRequest {

    @JsonProperty("text")
    private String text;

    @JsonProperty("mode")
    private String mode = "now";

    public WakeRequest() {
    }

    public WakeRequest(String text) {
        this.text = text;
    }

    public WakeRequest(String text, String mode) {
        this.text = text;
        this.mode = mode;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
