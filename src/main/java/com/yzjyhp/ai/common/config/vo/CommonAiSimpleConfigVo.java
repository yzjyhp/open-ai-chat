package com.yzjyhp.ai.common.config.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Description:模型配置信息
 * Date: 2024/9/14 13:50
 *
 * @version 1.0.0
 * @author: yzjyhp
 */
@Data
public class CommonAiSimpleConfigVo {

    /**
     * 调用接口秘钥-默认
     */
    private String apiKey;
    /**
     * 执行模型列表
     */
    private List<String> model;
    /**
     * 多模态模型集合
     */
    private List<String> multi_modal;
    /**
     * 支持搜索的模型集合
     */
    private List<String> enable_search;
    /**
     * 支持思考的模型集合
     */
    private List<String> enable_thinking;

    /**
     * 模型外显名称和真实模型映射
     */
    private Map<String, String> mapping;
}
