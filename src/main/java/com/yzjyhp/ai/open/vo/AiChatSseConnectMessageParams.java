package com.yzjyhp.ai.open.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Description:
 * Date: 2025/7/26 10:56
 * @author: yzjyhp
 * @version 1.0.0
 */
@Data
@ApiModel(value = "AiChatSseConnectMessageParams", description = "模型入参信息")
public class AiChatSseConnectMessageParams {
    @ApiModelProperty("角色")
    private String role;
    @ApiModelProperty("提示词")
    private String content;
    @ApiModelProperty("思考信息(保存记录的时候可能有)")
    private String reasoning;

}
