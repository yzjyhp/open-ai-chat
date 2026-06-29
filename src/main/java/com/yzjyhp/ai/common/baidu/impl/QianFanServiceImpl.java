package com.yzjyhp.ai.common.baidu.impl;

import com.alibaba.fastjson.JSON;
import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.QianfanV2;
import com.baidubce.qianfan.core.StreamIterator;
import com.baidubce.qianfan.core.builder.ChatV2Builder;
import com.baidubce.qianfan.model.chat.ChatUsage;
import com.baidubce.qianfan.model.chat.v2.Message;
import com.baidubce.qianfan.model.chat.v2.request.RequestV2;
import com.baidubce.qianfan.model.chat.v2.response.Choice;
import com.baidubce.qianfan.model.chat.v2.response.Delta;
import com.baidubce.qianfan.model.chat.v2.response.ResponseV2;
import com.baidubce.qianfan.model.chat.v2.response.StreamChoice;
import com.baidubce.qianfan.model.chat.v2.response.StreamResponseV2;
import com.yzjyhp.ai.common.baidu.QianFanService;
import com.yzjyhp.ai.common.vo.OpenAiModelErrorResult;
import com.yzjyhp.ai.common.vo.OpenAiModelHandleErrorResult;
import com.yzjyhp.ai.common.vo.OpenAiModelHandleParams;
import com.yzjyhp.ai.open.model.OpenAiChatCompletionChoice;
import com.yzjyhp.ai.open.model.OpenAiChatCompletionResult;
import com.yzjyhp.ai.open.model.OpenAiChatMessage;
import com.yzjyhp.ai.open.model.OpenAiUsage;
import com.yzjyhp.ai.open.service.AbstractOpenAiStreamResultCallBack;
import com.yzjyhp.ai.open.vo.AiChatSseConnectMessageParams;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description:
 * Date: 2025/4/17 20:08
 *
 * @version 1.0.0
 * @author: yzjyhp
 */
@Service
public class QianFanServiceImpl implements QianFanService {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * openAi非流式输出：执行模型调用
     *
     * @param sessionId
     * @param params
     * @return
     */
    @Override
    public Object callOpenAi(String sessionId, OpenAiModelHandleParams params) {
        logger.info("callOpenAi model:{},sessionId:{}", params.getModel(), sessionId);
        long startTime = System.currentTimeMillis();
        QianfanV2 client = new Qianfan(params.getApiKey()).v2();
        List<Message> messages = new ArrayList<>();
        List<AiChatSseConnectMessageParams> messageList = params.getMessages();
        for (int kt = 0; kt < messageList.size(); kt++) {
            AiChatSseConnectMessageParams p = messageList.get(kt);
            if (p != null) {
                Message userMsg = new Message();
                userMsg.setRole(p.getRole());
                userMsg.setContent(p.getContent());
                messages.add(userMsg);
            }
        }
        ChatV2Builder builder = client.chatCompletion().model(params.getMapping()).messages(messages) // 添加用户消息 (此方法可以调用多次，以实现多轮对话的消息传递)
                .temperature(0.7); // 自定义超参数
        if (params.getTemperature() != null) {
            builder.temperature(params.getTemperature());
        }
        if (params.getMax_tokens() != null) {
            builder.maxCompletionTokens(params.getMax_tokens());
        }
        if (params.getPresence_penalty() != null) {
            builder.presencePenalty(params.getPresence_penalty());
        }
        if (params.getTop_p() != null) {
            builder.topP(params.getTop_p());
        }
        RequestV2 request = builder.build(); // 发起请求
        try {
            long totalTime = (System.currentTimeMillis() - startTime);
            logger.info("callOpenAi 获取QPS控制把柄,model:{},sessionId:{},耗时:{}毫秒", params.getModel(), sessionId, totalTime);
            long startTime2 = System.currentTimeMillis();
            OpenAiChatCompletionResult handleResult = new OpenAiChatCompletionResult();
            ResponseV2 result = client.chatCompletion(request);
            long totalTime0 = (System.currentTimeMillis() - startTime);
            totalTime = (System.currentTimeMillis() - startTime2);
            logger.info("callOpenAi model:{},sessionId:{},result:{},总耗时:{}毫秒,执行耗时:{}毫秒", params.getModel(), sessionId, JSON.toJSONString(result), totalTime0, totalTime);
            if (result == null) {
                logger.info("call model:{},sessionId:{} result==null", params.getModel(), sessionId);
                OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
                OpenAiModelHandleErrorResult error = new OpenAiModelHandleErrorResult();
                error.setMessage("执行结果为空");
                error.setStatusCode(-1);
                errorResult.setError(error);
                return errorResult;
            }
            handleResult.setId(result.getId());
            handleResult.setCreated(result.getCreated());
            handleResult.setModel(result.getModel());
            handleResult.setSessionId(sessionId);
            handleResult.setObject(result.getObject());
            ChatUsage usage = result.getUsage();
            if (usage != null) {
                OpenAiUsage aiUsage = new OpenAiUsage();
                aiUsage.setPromptTokens(usage.getPromptTokens() == null ? 0 : usage.getPromptTokens());
                aiUsage.setCompletionTokens(usage.getCompletionTokens() == null ? 0 : usage.getCompletionTokens());
                aiUsage.setTotalTokens(usage.getTotalTokens() == null ? 0 : usage.getTotalTokens());
                handleResult.setUsage(aiUsage);
            }
            //处理结果信息
            List<OpenAiChatCompletionChoice> choices2 = new ArrayList<>();
            List<Choice> choices = result.getChoices();
            if (choices != null && !choices.isEmpty()) {
                //处理结果信息
                for (Choice choice : choices) {
                    OpenAiChatCompletionChoice model = new OpenAiChatCompletionChoice();
                    model.setIndex(choice.getIndex());
                    if (StringUtils.isNotEmpty(choice.getFinishReason())) {
                        model.setFinishReason(choice.getFinishReason());
                    }
                    Message message = choice.getMessage();
                    if (message != null) {
                        OpenAiChatMessage chatMessage = new OpenAiChatMessage();
                        chatMessage.setRole(message.getRole());
                        chatMessage.setContent(message.getContent());
                        chatMessage.setReasoningContent(message.getReasoningContent());
                        chatMessage.setToolCalls(message.getToolCalls());
                        chatMessage.setToolCallId(message.getToolCallId());
                        model.setDelta(chatMessage);
                    }
                    choices2.add(model);
                }
            }
            handleResult.setChoices(choices2);
            totalTime = (System.currentTimeMillis() - startTime);
            logger.info("callOpenAi model:{},sessionId:{},返回耗时:{}毫秒", params.getModel(), sessionId, totalTime);
            return handleResult;
        } catch (Exception e) {
            logger.warn("callOpenAi Exception", e);
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            handleResult.setMessage(e.getMessage());
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            return errorResult;
        }
    }

    /**
     * openAI流式输出：执行模型调用
     *
     * @param sessionId
     * @param params
     * @param callBack
     * @return
     */
    @Override
    public void streamCallOpenAi(String sessionId, OpenAiModelHandleParams params, AbstractOpenAiStreamResultCallBack callBack) {
        logger.info("streamCallOpenAi model:{},sessionId:{}", params.getModel(), sessionId);
        // 方式一：使用API Key值鉴权
        // 替换下列示例中参数，将your_APIKey替换为真实值，如何获取API Key请查看https://cloud.baidu.com/doc/WENXINWORKSHOP/s/Um2wxbaps#步骤二-获取api-key
        QianfanV2 client = new Qianfan(params.getApiKey()).v2();

        List<Message> messages = new ArrayList<>();
        List<AiChatSseConnectMessageParams> messageList = params.getMessages();
        for (int kt = 0; kt < messageList.size(); kt++) {
            AiChatSseConnectMessageParams p = messageList.get(kt);
            if (p != null) {
                Message userMsg = new Message();
                userMsg.setRole(p.getRole());
                userMsg.setContent(p.getContent());
                messages.add(userMsg);
            }
        }
        Map<String, Object> streamOptions = new HashMap<>();
        streamOptions.put("include_usage", true);
        ChatV2Builder builder = client.chatCompletion().streamOptions(streamOptions).model(params.getMapping()).messages(messages) // 添加用户消息 (此方法可以调用多次，以实现多轮对话的消息传递)
                .temperature(0.7); // 自定义超参数
        if (params.getTemperature() != null) {
            builder.temperature(params.getTemperature());
        }
        if (params.getMax_tokens() != null) {
            builder.maxCompletionTokens(params.getMax_tokens());
        }
        if (params.getPresence_penalty() != null) {
            builder.presencePenalty(params.getPresence_penalty());
        }
        if (params.getTop_p() != null) {
            builder.topP(params.getTop_p());
        }

        RequestV2 request = builder.build(); // 发起请求
        try {
            logger.info("streamCallOpenAi 获取QPS控制把柄,model:{},sessionId:{}", params.getModel(), sessionId);
            long startTime = System.currentTimeMillis();
            StreamIterator<StreamResponseV2> streamCallResult = client.chatCompletionStream(request);
            streamCallResult.forEachRemaining(result -> {
                long totalTime = (System.currentTimeMillis() - startTime);
                logger.info("streamCallOpenAi model:{},sessionId:{},result:{},耗时:{}毫秒", params.getModel(), sessionId, JSON.toJSONString(result), totalTime);
                OpenAiChatCompletionResult handleResult = new OpenAiChatCompletionResult();
                handleResult.setId(result.getId());
                handleResult.setCreated(result.getCreated());
                handleResult.setModel(result.getModel());
                handleResult.setObject(result.getObject());
                ChatUsage usage = result.getUsage();
                if (usage != null) {
                    OpenAiUsage aiUsage = new OpenAiUsage();
                    aiUsage.setPromptTokens(usage.getPromptTokens() == null ? 0 : usage.getPromptTokens());
                    aiUsage.setCompletionTokens(usage.getCompletionTokens() == null ? 0 : usage.getCompletionTokens());
                    aiUsage.setTotalTokens(usage.getTotalTokens() == null ? 0 : usage.getTotalTokens());
                    handleResult.setUsage(aiUsage);
                }
                List<OpenAiChatCompletionChoice> choices2 = new ArrayList<>();
                List<StreamChoice> choices = result.getChoices();
                if (choices != null && !choices.isEmpty()) {
                    //处理结果信息
                    for (StreamChoice choice : choices) {
                        OpenAiChatCompletionChoice model = new OpenAiChatCompletionChoice();
                        model.setIndex(choice.getIndex());
                        if (StringUtils.isNotEmpty(choice.getFinishReason())) {
                            model.setFinishReason(choice.getFinishReason());
                        }
                        model.setIndex(choice.getIndex());
                        model.setFinishReason(choice.getFinishReason());
                        Delta message = choice.getDelta();
                        if (message != null) {
                            OpenAiChatMessage chatMessage = new OpenAiChatMessage();
                            chatMessage.setContent(message.getContent());
                            chatMessage.setReasoningContent(message.getReasoningContent());
                            chatMessage.setToolCalls(message.getToolCalls());
                            model.setDelta(chatMessage);
                        }
                        choices2.add(model);
                    }
                }
                handleResult.setChoices(choices2);
                handleResult.setSessionId(sessionId);
                callBack.callBackOpenAiStream(sessionId, params.getType(), handleResult);
            });
        } catch (Exception e) {
            logger.warn("streamCallOpenAi Exception", e);
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            handleResult.setMessage(e.getMessage());
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            errorResult.setSessionId(sessionId);
            callBack.callBackOpenAiStreamError(sessionId, errorResult);
        }
    }
}
