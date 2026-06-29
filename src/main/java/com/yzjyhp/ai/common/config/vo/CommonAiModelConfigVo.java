package com.yzjyhp.ai.common.config.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Description:模型配置信息
 * Date: 2024/9/14 13:50
 *
 * @version 1.0.0
 * @author: yzjyhp
 */
@Data
public class CommonAiModelConfigVo {
    /**
     * 模型类型：1:alibaba,2:volcengine,3:baidu
     */
    private int type;
    /**
     * 调用接口秘钥-默认
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
     * 模型回答最大长度（单位 token）。
     */
    private Integer max_tokens;

    /**
     * 是否是流式输出：0:文本输出,1:流式输出
     */
    private boolean stream = false;


    /**
     * 系统内容
     */
    private String sysContent;

    @ApiModelProperty("取值范围为 [0, 1]。\n" + "核采样概率阈值。模型会考虑概率质量在 top_p 内的 token 结果。当取值为 0 时模型仅考虑对数概率最大的一个 token。\n" + "0.1 意味着只考虑概率质量最高的前 10% 的 token，取值越大生成的随机性越高，取值越低生成的确定性越高。通常建议仅调整 temperature 或 " + "top_p 其中之一，不建议两者都修改。")
    private Double top_p;

    @ApiModelProperty("(阿里云百炼不支持)控制模型生成文本时的内容重复度。\n" + "取值范围：[-2.0, 2.0]。正数会减少重复度，负数会增加重复度。\n" + "适用场景：\n" + "较高的presence_penalty适用于要求多样性、趣味性或创造性的场景，如创意写作或头脑风暴。\n" + "较低的presence_penalty" +
            "适用于要求一致性或专业术语的场景，如技术文档或其他正式文档。")
    private Double presence_penalty;

    @ApiModelProperty("采样温度，控制模型生成文本的多样性。\n" + "\n" + "temperature越高，生成的文本更多样，反之，生成的文本更确定。\n" + "\n" + "取值范围： [0, 2)")
    private Double temperature;

    /**
     * 模型在生成文本时是否使用互联网搜索结果进行参考
     */
    private Boolean enable_search;
    /**
     * 是否开启思考模式
     */
    private Boolean enable_thinking;
    /**
     * 是否多模态模型(只有阿里支持，因为调用方式不一样)
     */
    private Boolean multi_modal;

}
