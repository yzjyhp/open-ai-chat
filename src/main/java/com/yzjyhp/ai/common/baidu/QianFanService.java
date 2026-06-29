package com.yzjyhp.ai.common.baidu;

import com.yzjyhp.ai.common.vo.OpenAiModelHandleParams;
import com.yzjyhp.ai.open.service.AbstractOpenAiStreamResultCallBack;

/**
 * Description:
 * Date: 2025/4/17 20:08
 * @author: yzjyhp
 * @version 1.0.0
 */
public interface QianFanService {

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
