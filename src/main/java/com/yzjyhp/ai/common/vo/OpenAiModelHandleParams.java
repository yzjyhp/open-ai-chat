package com.yzjyhp.ai.common.vo;

import com.yzjyhp.ai.open.vo.AiChatSseConnectParams;
import lombok.Data;

/**
 * Description:
 * Date: 2025/7/28 15:34
 * @author: yzjyhp
 * @version 1.0.0
 */
@Data
public class OpenAiModelHandleParams extends AiChatSseConnectParams {

    /**
     * 模型类型：1:alibaba,2:volcengine,3:百度
     */
    private int type;
    /**
     * 调用接口秘钥
     */
    private String apiKey;
    /**
     * 映射对外-执行模型
     */
    private String model;
    /**
     * 真正-执行模型
     */
    private String mapping;

    /**
     * 是否多模态模型(只有阿里支持，因为调用方式不一样)
     */
    private Boolean multi_modal;

}
