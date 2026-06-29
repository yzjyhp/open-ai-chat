package com.yzjyhp.ai.common.service;

import com.yzjyhp.ai.common.config.vo.CommonAiModelConfigVo;

/**
 * Description:获取配置信息
 * Date: 2025/4/16 15:58
 *
 * @version 1.0.0
 * @author: yzjyhp
 */
public interface CommonConfigService {

    /**
     * 通过模型获取配置信息
     *
     * @param model
     * @return
     */
    CommonAiModelConfigVo getConfigByModel(String model);

}
