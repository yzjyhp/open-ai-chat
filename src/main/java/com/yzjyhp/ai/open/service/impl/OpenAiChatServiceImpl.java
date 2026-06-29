package com.yzjyhp.ai.open.service.impl;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzjyhp.ai.common.config.vo.CommonAiModelConfigVo;
import com.yzjyhp.ai.common.queue.AiQueueUtil;
import com.yzjyhp.ai.common.service.CommonConfigService;
import com.yzjyhp.ai.common.service.OpenAiExecuteService;
import com.yzjyhp.ai.common.vo.OpenAiModelErrorResult;
import com.yzjyhp.ai.common.vo.OpenAiModelHandleErrorResult;
import com.yzjyhp.ai.common.vo.OpenAiModelHandleParams;
import com.yzjyhp.ai.open.service.OpenAiChatService;
import com.yzjyhp.ai.open.util.SseEmitterUtil;
import com.yzjyhp.ai.open.vo.AiChatSseConnectMessageParams;
import com.yzjyhp.ai.open.vo.AiChatSseConnectParams;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Description:
 * Date: 2025/7/28 15:24
 * @author: yzjyhp
 * @version 1.0.0
 */
@Service
public class OpenAiChatServiceImpl implements OpenAiChatService {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    protected OpenAiExecuteService openAiExecuteService;
    @Autowired
    private CommonConfigService commonConfigService;

    /**
     * 流式：调用模型
     *
     * @param sessionId 当前会话Id
     * @param params    入参信息
     * @return sse链接
     */
    @Override
    public Object streamCallOpenAi(String sessionId, AiChatSseConnectParams params) {
        logger.info("streamCallOpenAi params:{},sessionId:{}", JSON.toJSONString(params), sessionId);
        SseEmitter connect = SseEmitterUtil.connect(sessionId);
        try {
            OpenAiModelHandleParams handleParams = checkParams(sessionId, params);
            AiQueueUtil.execute(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    openAiExecuteService.streamCallOpenAi(sessionId, handleParams);
                    long totalTime = (System.currentTimeMillis() - startTime);
                    logger.info("streamCallOpenAi sessionId:{},耗时:{}毫秒", sessionId, totalTime);
                } catch (Exception ex) {
                    logger.error("streamCallOpenAi", ex);
                    String msg = ex.getMessage();
                    connectAndSendError(sessionId, msg);
                }
            });
        } catch (Exception e) {
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            handleResult.setMessage(e.getMessage());
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            errorResult.setRequestId(sessionId);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(errorResult);
        }
        return connect;
    }

    /**
     * 校验入参信息
     * @param sessionId
     * @param params
     * @return
     */
    private OpenAiModelHandleParams checkParams(String sessionId, AiChatSseConnectParams params) throws Exception {
        logger.info("checkParams sessionId:{},params:{}", sessionId, JSON.toJSONString(params));
        long startTime = System.currentTimeMillis();
        String msg = "";
        String appCode = params.getAppCode();
        if (StringUtils.isBlank(appCode)) {
            logger.warn("checkParams sessionId:{},params:{} 调用方项目名称不为空", sessionId, JSON.toJSONString(params));
            msg = "调用方项目名称不为空";
            throw new RuntimeException(msg);
        }

        //校验数据
        String modelType = params.getModel();
        if (StringUtils.isBlank(modelType)) {
            logger.warn("checkParams sessionId:{},params:{} modelType不为空", sessionId, JSON.toJSONString(params));
            msg = "模型名称不为空";
            throw new Exception(msg);
        }
        List<AiChatSseConnectMessageParams> messageList = params.getMessages();
        if (messageList == null || messageList.isEmpty()) {
            logger.warn("checkParams sessionId:{},params:{} 模型入参信息不为空", sessionId, JSON.toJSONString(params));
            msg = "模型入参信息不为空";
            throw new Exception(msg);
        }
        CommonAiModelConfigVo configVo = commonConfigService.getConfigByModel(modelType);
        if (configVo == null) {
            logger.warn("checkParams sessionId:{},params:{},modelType找不到配置信息", sessionId, JSON.toJSONString(params));
            msg = "找不到配置信息";
            throw new Exception(msg);
        }
        if (StringUtils.isBlank(configVo.getApiKey())) {
            logger.warn("checkParams sessionId:{},params:{} modelType配置找不到秘钥信息", sessionId, JSON.toJSONString(params));
            msg = "找不到配置秘钥信息";
            throw new Exception(msg);
        }
        OpenAiModelHandleParams handleParams = new OpenAiModelHandleParams();
        BeanUtils.copyProperties(params, handleParams);
        Integer max_tokens = params.getMax_tokens();
        if (max_tokens == null || max_tokens <= 0) {
            handleParams.setMax_tokens(null);
        }

        handleParams.setModel(params.getModel());
        handleParams.setMapping(configVo.getMapping());
        handleParams.setType(configVo.getType());
        String apiKey = configVo.getApiKey();
        handleParams.setApiKey(apiKey);

        handleParams.setPresence_penalty(params.getPresence_penalty());
        handleParams.setTemperature(params.getTemperature());
        handleParams.setMulti_modal(configVo.getMulti_modal());
        if (configVo.getEnable_search() != null && params.getEnable_search() != null) {
            handleParams.setEnable_search(params.getEnable_search());
        }
        if (configVo.getEnable_thinking() != null && params.getEnable_thinking() != null) {
            handleParams.setEnable_thinking(params.getEnable_thinking());
        }

        long totalTime = (System.currentTimeMillis() - startTime);
        logger.info("checkParams sessionId:{},params:{} 校验数据耗时:{}毫秒", sessionId, JSON.toJSONString(params), totalTime);
        return handleParams;
    }


    /**
     * 长连接：异常数据推送
     * @param sessionId
     * @param msg
     */
    private void connectAndSendError(String sessionId, String msg) {
        SseEmitter connect = SseEmitterUtil.getSseEmitter(sessionId);
        if (connect == null) {
            return;
        }
        try {
            //流控后吐特定的文案
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            handleResult.setMessage(msg);
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            errorResult.setRequestId(sessionId);

            //处理数据格式
            String id = UUID.randomUUID().toString().replace("-", "");
            SseEmitter.SseEventBuilder sseEventBuilder3 = SseEmitter.event().id(id).reconnectTime(SseEmitterUtil.RECONNECT_TIME);
            //判断是不是结束语句
            sseEventBuilder3.data(errorResult, org.springframework.http.MediaType.APPLICATION_JSON);
            connect.send(sseEventBuilder3);
            try {
                SseEmitterUtil.removeSession(sessionId, null);
            } catch (Exception ex) {
            }
        } catch (IOException ex) {
        }
    }

    /**
     * 非流式：调用模型
     *
     * @param sessionId 当前会话Id
     * @param params    入参信息
     * @return 数据信息
     */
    @Override
    public Object callNonStream(String sessionId, AiChatSseConnectParams params) {
        logger.info("callNonStream sessionId:{},params:{}", sessionId, JSON.toJSONString(params));
        try {
            long startTime = System.currentTimeMillis();
            OpenAiModelHandleParams handleParams = checkParams(sessionId, params);
            Object result = openAiExecuteService.callOpenAi(sessionId, handleParams);
            long totalTime = (System.currentTimeMillis() - startTime);
            logger.info("callNonStream sessionId:{},耗时:{}毫秒", sessionId, totalTime);
            //处理结果信息
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            if (result != null) {
                try {
                    return mapper.writeValueAsString(result);
                } catch (Exception e) {
                    return JSON.toJSONString(result);
                }
            }
        } catch (Exception e) {
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            handleResult.setMessage(e.getMessage());
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            errorResult.setRequestId(sessionId);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(errorResult);
        }
        OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
        OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
        //异常信息
        handleResult.setMessage("请求模型异常，返回数据为空");
        handleResult.setStatusCode(-1);
        errorResult.setError(handleResult);
        errorResult.setRequestId(sessionId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(errorResult);
    }
}
