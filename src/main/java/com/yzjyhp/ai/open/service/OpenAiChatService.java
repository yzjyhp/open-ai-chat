package com.yzjyhp.ai.open.service;

import com.yzjyhp.ai.open.vo.AiChatSseConnectParams;

/**
 * Description:
 * Date: 2025/7/28 15:24
 * @author: yzjyhp
 * @version 1.0.0
 */
public interface OpenAiChatService {

    /**
     * 流式：调用模型
     * @param sessionId 当前会话Id
     * @param params 入参信息
     * @return sse链接
     */
    Object streamCallOpenAi(String sessionId, AiChatSseConnectParams params);

    /**
     * 非流式：调用模型
     * @param sessionId 当前会话Id
     * @param params 入参信息
     * @return 数据信息
     */
    Object callNonStream(String sessionId, AiChatSseConnectParams params);


}
