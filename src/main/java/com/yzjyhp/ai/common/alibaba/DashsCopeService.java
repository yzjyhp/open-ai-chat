package com.yzjyhp.ai.common.alibaba;

import com.yzjyhp.ai.common.vo.OpenAiModelHandleParams;
import com.yzjyhp.ai.open.service.AbstractOpenAiStreamResultCallBack;

/**
 * Description:
 * Date: 2025/4/16 13:47
 *
 * @version 1.0.0
 * @author: yzjyhp
 */
public interface DashsCopeService {

    /**
     * openAi非流式输出文本输出：执行模型调用-多模态模型
     *
     * @param params
     * @return
     */
    Object callOpenMultiModal(String sessionId, OpenAiModelHandleParams params);

    /**
     * openAi非流式输出：执行模型调用
     *
     * @param params
     * @return
     */
    Object callOpenAi(String sessionId, OpenAiModelHandleParams params);

    /**
     * openAi流式输出：执行模型调用
     *
     * @param params
     * @return
     */
    void streamCallOpenAi(String sessionId, OpenAiModelHandleParams params, AbstractOpenAiStreamResultCallBack callBack);

    /**
     * openAi流式输出：执行模型调用(多模态模型)
     *
     * @param params
     * @return
     */
    void streamCallOpenMultiModal(String sessionId, OpenAiModelHandleParams params, AbstractOpenAiStreamResultCallBack callBack);



}
