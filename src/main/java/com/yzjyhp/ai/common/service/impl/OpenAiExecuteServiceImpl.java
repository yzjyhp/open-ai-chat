package com.yzjyhp.ai.common.service.impl;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzjyhp.ai.common.alibaba.DashsCopeService;
import com.yzjyhp.ai.common.baidu.QianFanService;
import com.yzjyhp.ai.common.service.OpenAiExecuteService;
import com.yzjyhp.ai.common.vo.OpenAiModelErrorResult;
import com.yzjyhp.ai.common.vo.OpenAiModelHandleErrorResult;
import com.yzjyhp.ai.common.vo.OpenAiModelHandleParams;
import com.yzjyhp.ai.common.volcengine.ChatCompletionsService;
import com.yzjyhp.ai.open.model.OpenAiChatCompletionChoice;
import com.yzjyhp.ai.open.model.OpenAiChatCompletionResult;
import com.yzjyhp.ai.open.service.AbstractOpenAiStreamResultCallBack;
import com.yzjyhp.ai.open.util.SseEmitterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

/**
 * Description:
 * Date: 2025/7/29 10:41
 * @author: yzjyhp
 * @version 1.0.0
 */
@Service
public class OpenAiExecuteServiceImpl implements OpenAiExecuteService, AbstractOpenAiStreamResultCallBack {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private DashsCopeService dashsCopeService;
    @Autowired
    private ChatCompletionsService chatCompletionsService;
    @Autowired
    private QianFanService qianFanService;

    /**
     * openAi-执行任务
     *
     * @param sessionId 当前会话Id
     * @param params    入参信息
     * @return
     */
    @Override
    public void streamCallOpenAi(String sessionId, OpenAiModelHandleParams params) {
        logger.info("streamCallOpenAi sessionId:{},params:{}", sessionId, JSON.toJSONString(params));
        int type = params.getType();
        // 发送请求代码
        try {
            if (1 == type) {
                //只支持流失
                dashsCopeService.streamCallOpenAi(sessionId, params, this);
            } else if (2 == type) {
                //只支持流失
                chatCompletionsService.streamCallOpenAi(sessionId, params, this);
            } else if (3 == type) {
                //只支持流失
                qianFanService.streamCallOpenAi(sessionId, params, this);
            } else {
                logger.warn("streamCallOpenAi params:{} 找不到可执行类型信息", JSON.toJSONString(params));
                throw new RuntimeException("找不到模型厂商信息");
            }
        } catch (Exception e) {
            logger.warn("streamCallOpenAi", e);
            logger.warn("streamCallOpenAi params:{} 执行失败", JSON.toJSONString(params));
            throw e;
        }
    }

    /**
     * openAi-执行任务
     *
     * @param sessionId 当前会话Id
     * @param params    入参信息
     * @return
     */
    @Override
    public Object callOpenAi(String sessionId, OpenAiModelHandleParams params) {
        logger.info("callOpenAi sessionId:{},params:{}", sessionId, JSON.toJSONString(params));
        int type = params.getType();
        // 发送请求代码
        try {
            if (1 == type) {
                //只支持流失
                return dashsCopeService.callOpenAi(sessionId, params);
            } else if (2 == type) {
                //只支持流失
                return chatCompletionsService.callOpenAi(sessionId, params);
            } else if (3 == type) {
                //只支持流失
                return qianFanService.callOpenAi(sessionId, params);
            } else {
                logger.warn("callOpenAi params:{} 找不到可执行类型信息", JSON.toJSONString(params));
                OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
                OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
                //异常信息
                handleResult.setMessage("找不到模型厂商信息");
                handleResult.setStatusCode(-1);
                errorResult.setError(handleResult);
                errorResult.setRequestId(sessionId);
                return errorResult;
            }
        } catch (Exception e) {
            logger.warn("callOpenAi", e);
            logger.warn("callOpenAi params:{} 执行失败", JSON.toJSONString(params));
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            handleResult.setMessage(e.getMessage());
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            errorResult.setRequestId(sessionId);
            return errorResult;
        }
    }

    /**
     * 流式：执行结果回调
     *
     * @param sessionId
     * @param type 模型类型：1:alibaba,2:volcengine
     * @param result
     */
    @Override
    public void callBackOpenAiStream(String sessionId, int type, OpenAiChatCompletionResult result) {
        logger.info("callBackOpenAi sessionId:{},type:{},result:{}", sessionId, type, JSON.toJSONString(result));
        SseEmitter connect = SseEmitterUtil.getSseEmitter(sessionId);
        if (connect == null) {
            return;
        }
        result.setSessionId(sessionId);
        String event = "transfer";
        List<OpenAiChatCompletionChoice> choices = result.getChoices();
        boolean stop = false;
        if (choices == null || choices.isEmpty()) {
            //抖音：在stop节点后面会有一个空节点来返回使用token数据
            stop = true;
        } else {
            //阿里云:token数量在推送过程就有，只有一个stop节点
            long total = choices.stream().filter(choice -> {
                if ("stop".equalsIgnoreCase(choice.getFinishReason())) {
                    return true;
                }
                return false;
            }).count();
            if (total > 0) {
                stop = true;
            }
        }
        if (stop) {
            event = "end";
        }
        try {
            String id= UUID.randomUUID().toString().replace("-", "");
            SseEmitter.SseEventBuilder sseEventBuilder = SseEmitter.event().name(event).id(id).reconnectTime(SseEmitterUtil.RECONNECT_TIME);
            //处理结果信息
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            sseEventBuilder.data(mapper.writeValueAsString(result), org.springframework.http.MediaType.APPLICATION_JSON);
            connect.send(sseEventBuilder);
        } catch (Exception e) {
            logger.warn("callBackOpenAi", e);
            logger.warn("callBackOpenAi sessionId:{},error:{}", sessionId, e.getMessage());
        }
        if (stop) {
            //暂停一会再进行关闭链接，让上游有个缓冲时间
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            try {
                SseEmitterUtil.removeSession(sessionId, null);
            } catch (Exception e) {
                logger.error("callBackOpenAi", e);
                logger.error("callBackOpenAi sessionId:{},error:{}", sessionId, e.getMessage());
            }
        }
    }

    /**
     * 流式：执行结果异常回调
     *
     * @param sessionId
     * @param result
     */
    @Override
    public void callBackOpenAiStreamError(String sessionId, OpenAiModelErrorResult result) {
        logger.info("callBackOpenAiStreamError sessionId:{},result:{}", sessionId, JSON.toJSONString(result));
        SseEmitter connect = SseEmitterUtil.getSseEmitter(sessionId);
        if (connect == null) {
            return;
        }
        result.setSessionId(sessionId);
        try {
            String id= UUID.randomUUID().toString().replace("-", "");
            SseEmitter.SseEventBuilder sseEventBuilder = SseEmitter.event().id(id).reconnectTime(SseEmitterUtil.RECONNECT_TIME);
            //处理结果信息
            sseEventBuilder.data(result, org.springframework.http.MediaType.APPLICATION_JSON);
            connect.send(sseEventBuilder);
            SseEmitterUtil.removeSession(sessionId, null);
        } catch (Exception e) {
            logger.error("callBackOpenAi", e);
            logger.error("callBackOpenAi sessionId:{},error:{}", sessionId, e.getMessage());
        }
    }

}
