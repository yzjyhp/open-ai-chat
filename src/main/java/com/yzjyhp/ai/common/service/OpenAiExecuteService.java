package com.yzjyhp.ai.common.service;

import com.yzjyhp.ai.common.vo.OpenAiModelHandleParams;

/**
 * Description: openAI:统一AI执行入口
 * Date: 2025/4/16 15:53
 *
 * @version 1.0.0
 * @author: yzjyhp
 */
public interface OpenAiExecuteService {

    /**
     * openAi-执行任务
     * @param sessionId 当前会话Id
     * @param params 入参信息
     * @return
     */
    void streamCallOpenAi(String sessionId, OpenAiModelHandleParams params);

    /**
     * openAi-执行任务
     * @param sessionId 当前会话Id
     * @param params 入参信息
     * @return
     */
    Object callOpenAi(String sessionId, OpenAiModelHandleParams params);

}
