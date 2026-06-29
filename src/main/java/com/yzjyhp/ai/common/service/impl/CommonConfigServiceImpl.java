package com.yzjyhp.ai.common.service.impl;

import com.yzjyhp.ai.common.config.CommonAiAuthProperties;
import com.yzjyhp.ai.common.config.vo.CommonAiModelConfigVo;
import com.yzjyhp.ai.common.config.vo.CommonAiSimpleConfigVo;
import com.yzjyhp.ai.common.service.CommonConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Description: 获取配置信息
 * Date: 2025/4/16 15:58
 *
 * @version 1.0.0
 * @author: yzjyhp
 */
@Slf4j
@Service
public class CommonConfigServiceImpl implements CommonConfigService {

    @Autowired
    private CommonAiAuthProperties commonAiAuthProperties;

    /**
     * 通过模型获取配置信息
     *
     * @param model
     * @return
     */
    @Override
    public CommonAiModelConfigVo getConfigByModel(String model) {
        if (StringUtils.isBlank(model)) {
            return null;
        }
        CommonAiSimpleConfigVo configVo1 = commonAiAuthProperties.getAlibaba();
        if (configVo1 != null) {
            List<String> modes = configVo1.getModel();
            if (modes != null && !modes.isEmpty()) {
                if (modes.contains(model)) {
                    CommonAiModelConfigVo vo = new CommonAiModelConfigVo();
                    vo.setModel(model);
                    vo.setMapping(model);
                    vo.setType(1);
                    if ("qwq-plus".equals(model)) {
                        vo.setStream(true);
                    }
                    getConfigVo(model, vo, configVo1);
                    return vo;
                }
            }
        }
        CommonAiSimpleConfigVo configVo2 = commonAiAuthProperties.getVolcengine();
        if (configVo2 != null) {
            List<String> modes = configVo2.getModel();
            if (modes != null && !modes.isEmpty()) {
                if (modes.contains(model)) {
                    CommonAiModelConfigVo vo = new CommonAiModelConfigVo();
                    vo.setModel(model);
                    vo.setMapping(model);
                    vo.setType(2);
                    getConfigVo(model, vo, configVo2);
                    return vo;
                }
            }
        }
        CommonAiSimpleConfigVo configVo3 = commonAiAuthProperties.getBaidu();
        if (configVo3 != null) {
            List<String> modes = configVo3.getModel();
            if (modes != null && !modes.isEmpty()) {
                if (modes.contains(model)) {
                    CommonAiModelConfigVo vo = new CommonAiModelConfigVo();
                    vo.setModel(model);
                    vo.setMapping(model);
                    vo.setType(3);
                    getConfigVo(model, vo, configVo3);
                    return vo;
                }
            }
        }
        return null;
    }

    /**
     * 转化对象
     * @param model
     * @param vo
     * @param configVo
     */
    private void getConfigVo(String model, CommonAiModelConfigVo vo, CommonAiSimpleConfigVo configVo) {

        vo.setApiKey(configVo.getApiKey());

        Map<String, String> mapping = configVo.getMapping();
        if (mapping != null) {
            String mp = mapping.get(model);
            if (StringUtils.isNotBlank(mp)) {
                vo.setMapping(mp);
            }
        }
        List<String> multi_modal = configVo.getMulti_modal();
        if (multi_modal != null && multi_modal.contains(model)) {
            vo.setMulti_modal(true);
        }

        List<String> enable_search = configVo.getEnable_search();
        if (enable_search != null && enable_search.contains(model)) {
            vo.setEnable_search(true);
        }
        List<String> enable_thinking = configVo.getEnable_thinking();
        if (enable_thinking != null && enable_thinking.contains(model)) {
            vo.setEnable_thinking(true);
        }

    }
}
