package com.yzjyhp.ai.open.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Description:
 * Date: 2025/7/28 11:36
 * @author: yzjyhp
 * @version 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiUsage {

    /**
     * The number of prompt tokens used.
     */
    @JsonProperty("prompt_tokens")
    long promptTokens;

    /**
     * The number of completion tokens used.
     */
    @JsonProperty("completion_tokens")
    long completionTokens;

    /**
     * The number of total tokens used
     */
    @JsonProperty("total_tokens")
    long totalTokens;

    @JsonProperty("prompt_tokens_details")
    private Object promptTokensDetails;

    @JsonProperty("completion_tokens_details")
    private Object completionTokensDetails;

    public long getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(long promptTokens) {
        this.promptTokens = promptTokens;
    }

    public long getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(long completionTokens) {
        this.completionTokens = completionTokens;
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(long totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Object getPromptTokensDetails() {
        return promptTokensDetails;
    }

    public void setPromptTokensDetails(Object promptTokensDetails) {
        this.promptTokensDetails = promptTokensDetails;
    }

    public Object getCompletionTokensDetails() {
        return completionTokensDetails;
    }

    public void setCompletionTokensDetails(Object completionTokensDetails) {
        this.completionTokensDetails = completionTokensDetails;
    }
}
