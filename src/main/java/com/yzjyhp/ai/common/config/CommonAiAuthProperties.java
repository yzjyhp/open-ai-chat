
package com.yzjyhp.ai.common.config;

import com.yzjyhp.ai.common.config.vo.CommonAiSimpleConfigVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author : yuanjh
 * @version : 1.0.0
 * @description : 调用escommon统一配置以及类
 * @date : 2024/7/31 20:05
 */
@Data
@ConfigurationProperties(value = "ai")
@Component
public class CommonAiAuthProperties {

    /**
     * 阿里云AI
     */
    private CommonAiSimpleConfigVo alibaba;
    /**
     * 火山
     */
    private CommonAiSimpleConfigVo volcengine;
    /**
     * 百度
     */
    private CommonAiSimpleConfigVo baidu;

}
