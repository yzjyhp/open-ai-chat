package com.yzjyhp.ai.open.service;

import com.yzjyhp.ai.common.vo.OpenAiModelErrorResult;
import com.yzjyhp.ai.open.model.OpenAiChatCompletionResult;

/**
 * Description:
 * Date: 2025/7/28 18:00
 * @author: yzjyhp
 * @version 1.0.0
 */
public interface AbstractOpenAiStreamResultCallBack {

    /**
     * 执行结果回调
     *
     *
     * @param sessionId 会话id
     * @param type  模型类型：1:alibaba,2:volcengine
     * @param result 结果
     */
    void callBackOpenAiStream(String sessionId, int type, OpenAiChatCompletionResult result);

    /**
     * 执行异常回调
     * @param sessionId
     * @param sessionId
     * @param result
     */
    void callBackOpenAiStreamError(String sessionId, OpenAiModelErrorResult result);
}
