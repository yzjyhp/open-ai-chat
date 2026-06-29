package com.yzjyhp.ai.common.vo;

import lombok.Data;

/**
 * Description: AI执行结果返回
 * Date: 2025/4/16 14:46
 *
 * @version 1.0.0
 * @author: yzjyhp
 */
@Data
public class OpenAiModelErrorResult {
    public OpenAiModelHandleErrorResult error;
    public String requestId;

    /**
     * 当前会话id
     */
    public String sessionId;
}
