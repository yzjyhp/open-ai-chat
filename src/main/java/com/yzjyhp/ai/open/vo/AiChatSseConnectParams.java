package com.yzjyhp.ai.open.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * Description:
 * Date: 2025/7/26 10:47
 * @author: yzjyhp
 * @version 1.0.0
 */
@Data
@ApiModel(value = "AiChatSseConnectParams", description = "AI模型调用入参")
public class AiChatSseConnectParams {

    @ApiModelProperty("调用方项目名称")
    private String appCode;

    @ApiModelProperty("模型名称:eg:qwen-plus")
    private String model;

    @ApiModelProperty("模型入参信息")
    private List<AiChatSseConnectMessageParams> messages;

    @ApiModelProperty("模型流式输出")
    private boolean stream = true;
    @ApiModelProperty("是否开启思考模式")
    private Boolean enable_thinking = false;
    @ApiModelProperty("是否使用互联网搜索")
    private Boolean enable_search = false;

    @ApiModelProperty("取值范围：各个模型不同，详细见模型列表。\n" + "模型回答最大长度（单位 token）。")
    private Integer max_tokens;

    @ApiModelProperty("取值范围为 [0, 1]。\n" + "核采样概率阈值。模型会考虑概率质量在 top_p 内的 token 结果。当取值为 0 时模型仅考虑对数概率最大的一个 token。\n" + "0.1 意味着只考虑概率质量最高的前 10% 的 token，取值越大生成的随机性越高，取值越低生成的确定性越高。通常建议仅调整 temperature 或 " + "top_p 其中之一，不建议两者都修改。")
    private Double top_p;

    @ApiModelProperty("(阿里云百炼不支持)控制模型生成文本时的内容重复度。\n" + "取值范围：[-2.0, 2.0]。正数会减少重复度，负数会增加重复度。\n" + "适用场景：\n" + "较高的presence_penalty适用于要求多样性、趣味性或创造性的场景，如创意写作或头脑风暴。\n" + "较低的presence_penalty" +
            "适用于要求一致性或专业术语的场景，如技术文档或其他正式文档。")
    private Double presence_penalty;

    @ApiModelProperty("采样温度，控制模型生成文本的多样性。\n" + "\n" + "temperature越高，生成的文本更多样，反之，生成的文本更确定。\n" + "\n" + "取值范围： [0, 2)")
    private Double temperature;

    @ApiModelProperty(value = "会话id")
    private String sessionId;

}
