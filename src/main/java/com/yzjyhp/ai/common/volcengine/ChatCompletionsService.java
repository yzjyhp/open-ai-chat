package com.yzjyhp.ai.common.volcengine;

import com.yzjyhp.ai.common.vo.OpenAiModelHandleParams;
import com.yzjyhp.ai.open.service.AbstractOpenAiStreamResultCallBack;

/**
 * 火山方舟大模型服务
 * Description:
 * Date: 2025/4/16 14:07
 *
 * @version 1.0.0
 * @author: yzjyhp
 */
public interface ChatCompletionsService {

    /**
     * openAi非流式输出：执行模型调用
     *
     * @param params
     * @return
     */
    Object callOpenAi(String sessionId, OpenAiModelHandleParams params);

    /**
     * openAI流式输出：执行模型调用
     *
     * @param params
     * @return
     */
    void streamCallOpenAi(String sessionId, OpenAiModelHandleParams params, AbstractOpenAiStreamResultCallBack callBack);


}
