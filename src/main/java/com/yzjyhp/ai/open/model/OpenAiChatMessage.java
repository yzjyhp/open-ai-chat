package com.yzjyhp.ai.open.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Description:
 * Date: 2025/7/28 11:37
 * @author: yzjyhp
 * @version 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiChatMessage {

    String role;
    Object content;
    @JsonProperty("reasoning_content")
    String reasoningContent;
    @JsonProperty("function_call")
    Object functionCall;
    @JsonProperty("tool_calls")
    Object toolCalls;

    @JsonProperty("tool_call_id")
    String toolCallId;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String getReasoningContent() {
        return reasoningContent;
    }

    public void setReasoningContent(String reasoningContent) {
        this.reasoningContent = reasoningContent;
    }

    public Object getFunctionCall() {
        return functionCall;
    }

    public void setFunctionCall(Object functionCall) {
        this.functionCall = functionCall;
    }

    public Object getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(Object toolCalls) {
        this.toolCalls = toolCalls;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }
}
