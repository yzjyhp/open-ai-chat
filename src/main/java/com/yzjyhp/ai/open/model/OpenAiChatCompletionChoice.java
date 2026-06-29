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
public class OpenAiChatCompletionChoice {


    /**
     * This index of this completion in the returned list.
     */
    Integer index;

    /**
     * The assistant message or delta (when streaming) which was generated
     */
    @JsonProperty("delta")
    OpenAiChatMessage delta;

    /**
     * The reason why GPT stopped generating, for example "length".
     */
    @JsonProperty("finish_reason")
    String finishReason;

    /**
     * The type of content moderation service hit.
     */
    @JsonProperty("moderation_hit_type")
    String moderationHitType;

    /**
     * Log probability information for the choice.
     */
    @JsonProperty("logprobs")
    Object logprobs;

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public OpenAiChatMessage getDelta() {
        return delta;
    }

    public void setDelta(OpenAiChatMessage delta) {
        this.delta = delta;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public String getModerationHitType() {
        return moderationHitType;
    }

    public void setModerationHitType(String moderationHitType) {
        this.moderationHitType = moderationHitType;
    }

    public Object getLogprobs() {
        return logprobs;
    }

    public void setLogprobs(Object logprobs) {
        this.logprobs = logprobs;
    }
}
