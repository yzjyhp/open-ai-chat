package com.yzjyhp.ai.common.vo;

import lombok.Data;

/**
 * Description: AI执行结果返回
 * Date: 2025/4/16 14:46
 *
 * @version 1.0.0
 * @author: yzjyhp
 */
@Data
public class OpenAiModelHandleErrorResult {
    public int statusCode;
    public String code;
    private String message;
    public String param;
    public String type;
    public String requestId;


}
