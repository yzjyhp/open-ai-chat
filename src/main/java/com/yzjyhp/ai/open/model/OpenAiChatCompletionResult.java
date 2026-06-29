package com.yzjyhp.ai.open.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Description:
 * Date: 2025/7/28 11:35
 * @author: yzjyhp
 * @version 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiChatCompletionResult {

    /**
     * 当前会话id
     */
    String sessionId;

    /**
     * Unique id assigned to this chat completion.
     */
    String id;

    /**
     * The type of object returned, should be "chat.completion"
     */
    String object;

    /**
     * The creation time in epoch seconds.
     */
    long created;

    /**
     * The GPT model used.
     */
    String model;

    /**
     * Specifies the latency tier to use for processing the request.
     *
     *     This parameter is relevant for customers subscribed to the scale tier service:
     *
     *     - If set to 'auto', and the endpoint is Scale tier enabled, the system will
     *       utilize scale tier credits until they are exhausted.
     *     - If set to 'auto', and the endpoint is not Scale tier enabled, the request will
     *       be processed using the default service tier with a lower uptime SLA and no
     *       latency guarentee.
     *     - If set to 'default', the request will be processed using the default service
     *       tier with a lower uptime SLA and no latency guarentee.
     *     - When not set, the default behavior is 'auto'.
     *
     *     When this parameter is set, the response body will include the `service_tier`
     *     utilized.
     */
    @JsonProperty("service_tier")
    String serviceTier;

    /**
     * A list of all generated completions.
     */
    List<OpenAiChatCompletionChoice> choices;

    /**
     * The API usage for this request.
     */
    OpenAiUsage usage;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getServiceTier() {
        return serviceTier;
    }

    public void setServiceTier(String serviceTier) {
        this.serviceTier = serviceTier;
    }

    public List<OpenAiChatCompletionChoice> getChoices() {
        return choices;
    }

    public void setChoices(List<OpenAiChatCompletionChoice> choices) {
        this.choices = choices;
    }

    public OpenAiUsage getUsage() {
        return usage;
    }

    public void setUsage(OpenAiUsage usage) {
        this.usage = usage;
    }
}
