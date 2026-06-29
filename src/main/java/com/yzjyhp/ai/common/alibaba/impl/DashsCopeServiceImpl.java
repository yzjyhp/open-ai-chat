package com.yzjyhp.ai.common.alibaba.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationOutput;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.GenerationUsage;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationOutput;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationUsage;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.fastjson.JSON;
import com.yzjyhp.ai.common.alibaba.DashsCopeService;
import com.yzjyhp.ai.common.vo.OpenAiModelErrorResult;
import com.yzjyhp.ai.common.vo.OpenAiModelHandleErrorResult;
import com.yzjyhp.ai.common.vo.OpenAiModelHandleParams;
import com.yzjyhp.ai.open.model.OpenAiChatCompletionChoice;
import com.yzjyhp.ai.open.model.OpenAiChatCompletionResult;
import com.yzjyhp.ai.open.model.OpenAiChatMessage;
import com.yzjyhp.ai.open.model.OpenAiUsage;
import com.yzjyhp.ai.open.service.AbstractOpenAiStreamResultCallBack;
import com.yzjyhp.ai.open.vo.AiChatSseConnectMessageParams;
import io.reactivex.Flowable;
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
 * Date: 2025/4/16 13:47
 *
 * @version 1.0.0
 * @author: yzjyhp
 */
@Service
public class DashsCopeServiceImpl implements DashsCopeService {
    protected Logger logger = LoggerFactory.getLogger(this.getClass());


    /**
     * openAi非流式输出文本输出：执行模型调用-多模态模型
     *
     * @param sessionId
     * @param params
     * @return
     */
    @Override
    public Object callOpenMultiModal(String sessionId, OpenAiModelHandleParams params) {
        logger.info("callOpenMultiModal model:{},sessionId:{}", params.getModel(), sessionId);
        MultiModalConversation conv = new MultiModalConversation();
        List<MultiModalMessage> messages = new ArrayList<>();

        List<AiChatSseConnectMessageParams> messageList = params.getMessages();
        for (int kt = 0; kt < messageList.size(); kt++) {
            AiChatSseConnectMessageParams p = messageList.get(kt);
            if (p != null) {
                List<Map<String, Object>> content = new ArrayList<>();
                Map<String, Object> map = new HashMap<>();
                map.put("text", p.getContent());
                content.add(map);
                MultiModalMessage message = MultiModalMessage.builder()
                        .role(p.getRole())
                        .content(content).build();
                messages.add(message);
            }
        }

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                // 各地域的API Key不同。获取API Key：https://help.aliyun.com/zh/model-studio/get-api-key
                .apiKey(params.getApiKey())
                .model(params.getMapping())  // 此处以qwen3.5-plus为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/models
                .messages(messages)
                .build();

        if (params.getMax_tokens() != null && params.getMax_tokens() > 0) {
            param.setMaxTokens(params.getMax_tokens());
        }
        if (params.getTop_p() != null) {
            param.setTopP(params.getTop_p());
        }
        if (params.getTemperature() != null) {
            param.setTemperature(params.getTemperature().floatValue());
        }
        if (params.getPresence_penalty() != null) {
            param.setRepetitionPenalty(params.getPresence_penalty().floatValue());
        }
        if (params.getEnable_search() != null) {
            param.setEnableSearch(params.getEnable_search());
        }
        if (params.getEnable_thinking() != null) {
            param.setEnableThinking(params.getEnable_thinking());
        }
        long startTime = System.currentTimeMillis();
        try {
            long totalTime = (System.currentTimeMillis() - startTime);
            logger.info("callOpenMultiModal 获取QPS控制把柄,model:{},sessionId:{},耗时:{}毫秒", params.getModel(), sessionId, totalTime);
            long startTime2 = System.currentTimeMillis();
            MultiModalConversationResult result = conv.call(param);
            long totalTime0 = (System.currentTimeMillis() - startTime);
            totalTime = (System.currentTimeMillis() - startTime2);
            logger.info("callOpenMultiModal model:{},sessionId:{},result:{},总耗时:{}毫秒,执行耗时:{}毫秒", params.getModel(), sessionId, JsonUtils.toJson(result), totalTime0, totalTime);
            OpenAiChatCompletionResult handleResult = new OpenAiChatCompletionResult();
            handleResult.setId(result.getRequestId());
            handleResult.setModel(params.getMapping());
            handleResult.setSessionId(sessionId);

            MultiModalConversationOutput output = result.getOutput();
            if (output == null) {
                logger.info("callOpenMultiModal model:{},sessionId:{} output==null", params.getModel(), sessionId);
                OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
                OpenAiModelHandleErrorResult handleResult2 = new OpenAiModelHandleErrorResult();
                handleResult2.setMessage("output is null");
                handleResult2.setStatusCode(-1);
                errorResult.setError(handleResult2);
                return errorResult;
            }
            List<MultiModalConversationOutput.Choice> choices = output.getChoices();
            if (choices == null || choices.isEmpty()) {
                logger.info("callOpenMultiModal model:{},sessionId:{} choices.isEmpty()", params.getModel(), sessionId);
                OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
                OpenAiModelHandleErrorResult handleResult2 = new OpenAiModelHandleErrorResult();
                handleResult2.setMessage("choices == null || choices.isEmpty()");
                handleResult2.setStatusCode(-1);
                errorResult.setError(handleResult2);
                return errorResult;
            }
            //处理结果信息
            List<OpenAiChatCompletionChoice> choices2 = new ArrayList<>();
            for (int kt = 0; kt < choices.size(); kt++) {
                MultiModalConversationOutput.Choice choice = choices.get(kt);
                OpenAiChatCompletionChoice model = new OpenAiChatCompletionChoice();
                model.setIndex(kt);
                //SDK格式化问题，会存在"null"
                if (StringUtils.isNotEmpty(choice.getFinishReason()) && !"null".equalsIgnoreCase(choice.getFinishReason())) {
                    model.setFinishReason(choice.getFinishReason());
                }
                MultiModalMessage message = choice.getMessage();
                if (message != null) {
                    OpenAiChatMessage chatMessage = new OpenAiChatMessage();
                    chatMessage.setRole(message.getRole());
                    chatMessage.setContent(message.getContent());
                    chatMessage.setReasoningContent(message.getReasoningContent());
                    chatMessage.setToolCalls(message.getToolCalls());
                    chatMessage.setToolCallId(message.getToolCallId());
                    model.setDelta(chatMessage);
                    List<Map<String, Object>> content = message.getContent();
                    if (content != null && !content.isEmpty()) {
                        Map<String, Object> m0 = content.get(0);
                        if (m0 != null) {
                            Object text = m0.get("text");
                            if (text instanceof String) {
                                chatMessage.setContent((String) text);
                            } else {
                                String msg = JSON.toJSONString(text);
                                chatMessage.setContent(msg);
                            }
                        }
                    }
                    choices2.add(model);
                }
            }
            handleResult.setChoices(choices2);

            MultiModalConversationUsage usage = result.getUsage();
            if (usage != null) {
                OpenAiUsage aiUsage = new OpenAiUsage();
                aiUsage.setPromptTokens(usage.getInputTokens() == null ? 0 : usage.getInputTokens());
                aiUsage.setCompletionTokens(usage.getOutputTokens() == null ? 0 : usage.getOutputTokens());
                aiUsage.setTotalTokens(usage.getTotalTokens() == null ? 0 : usage.getTotalTokens());
                aiUsage.setCompletionTokensDetails(usage.getOutputTokensDetails() == null ? null : usage.getOutputTokensDetails());
                handleResult.setUsage(aiUsage);
            }
            totalTime = (System.currentTimeMillis() - startTime);
            logger.info("callOpenMultiModal model:{},sessionId:{},返回耗时:{}毫秒", params.getModel(), sessionId, totalTime);
            return handleResult;
        } catch (ApiException e) {
            //异常信息
            logger.warn("callOpenMultiModal ApiException", e);
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            Status status = e.getStatus();
            if (status != null) {
                errorResult.setRequestId(status.getRequestId());
                handleResult.setRequestId(status.getRequestId());
                handleResult.setCode(status.getCode());
                handleResult.setStatusCode(status.getStatusCode());
                handleResult.setMessage(status.getMessage());
            }
            handleResult.setMessage(e.getMessage());
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            return errorResult;
        } catch (Exception e1) {
            //异常信息
            logger.warn("callOpenMultiModal Exception", e1);
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            handleResult.setMessage(e1.getMessage());
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            return errorResult;
        }
    }

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
        //判断是否走多模态模型
        if (params.getMulti_modal() != null && params.getMulti_modal()) {
            return callOpenMultiModal(sessionId, params);
        }
        long startTime = System.currentTimeMillis();
        Generation gen = new Generation();
        List<Message> messages = new ArrayList<>();
        List<AiChatSseConnectMessageParams> messageList = params.getMessages();
        for (int kt = 0; kt < messageList.size(); kt++) {
            AiChatSseConnectMessageParams p = messageList.get(kt);
            if (p != null) {
                Message userMsg = Message.builder().role(p.getRole()).content(p.getContent()).build();
                messages.add(userMsg);
            }
        }
        GenerationParam.GenerationParamBuilder builder = GenerationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(params.getApiKey())
                // 此处以qwen-plus为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/getting-started/models
                .model(params.getMapping()).messages(messages).resultFormat(GenerationParam.ResultFormat.MESSAGE);
        if (params.getMax_tokens() != null && params.getMax_tokens() > 0) {
            builder.maxTokens(params.getMax_tokens());
        }
        if (params.getTop_p() != null) {
            builder.topP(params.getTop_p());
        }
        if (params.getTemperature() != null) {
            builder.temperature(params.getTemperature().floatValue());
        }
        if (params.getPresence_penalty() != null) {
            builder.repetitionPenalty(params.getPresence_penalty().floatValue());
        }
        if (params.getEnable_search() != null) {
            builder.enableSearch(params.getEnable_search());
        }

        GenerationParam param = builder.build();
        try {
            long totalTime = (System.currentTimeMillis() - startTime);
            logger.info("callOpenAi 获取QPS控制把柄,model:{},sessionId:{},耗时:{}毫秒", params.getModel(), sessionId, totalTime);
            long startTime2 = System.currentTimeMillis();
            GenerationResult result = gen.call(param);
            long totalTime0 = (System.currentTimeMillis() - startTime);
            totalTime = (System.currentTimeMillis() - startTime2);
            logger.info("callOpenAi model:{},sessionId:{},result:{},总耗时:{}毫秒,执行耗时:{}毫秒", params.getModel(), sessionId, JsonUtils.toJson(result), totalTime0, totalTime);
            OpenAiChatCompletionResult handleResult = new OpenAiChatCompletionResult();
            handleResult.setId(result.getRequestId());
            handleResult.setModel(params.getMapping());
            handleResult.setSessionId(sessionId);
            GenerationUsage usage = result.getUsage();
            if (usage != null) {
                OpenAiUsage aiUsage = new OpenAiUsage();
                aiUsage.setPromptTokens(usage.getInputTokens() == null ? 0 : usage.getInputTokens());
                aiUsage.setCompletionTokens(usage.getOutputTokens() == null ? 0 : usage.getOutputTokens());
                aiUsage.setTotalTokens(usage.getTotalTokens() == null ? 0 : usage.getTotalTokens());
                aiUsage.setCompletionTokensDetails(usage.getOutputTokensDetails() == null ? null : usage.getOutputTokensDetails());
                handleResult.setUsage(aiUsage);
            }
            //处理结果信息
            List<OpenAiChatCompletionChoice> choices2 = new ArrayList<>();
            GenerationOutput output = result.getOutput();
            if (output != null) {
                List<GenerationOutput.Choice> choices = output.getChoices();
                if (choices != null && !choices.isEmpty()) {
                    //处理结果信息
                    for (GenerationOutput.Choice choice : choices) {
                        OpenAiChatCompletionChoice model = new OpenAiChatCompletionChoice();
                        model.setIndex(choice.getIndex());
                        //SDK格式化问题，会存在"null"
                        if (StringUtils.isNotEmpty(choice.getFinishReason()) && !"null".equalsIgnoreCase(choice.getFinishReason())) {
                            model.setFinishReason(choice.getFinishReason());
                        }
                        model.setLogprobs(choice.getLogprobs());
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
            }
            handleResult.setChoices(choices2);
            totalTime = (System.currentTimeMillis() - startTime);
            logger.info("callOpenAi model:{},sessionId:{},返回耗时:{}毫秒", params.getModel(), sessionId, totalTime);
            return handleResult;
        } catch (ApiException e) {
            //异常信息
            logger.warn("callOpenAi ApiException", e);
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            Status status = e.getStatus();
            if (status != null) {
                errorResult.setRequestId(status.getRequestId());
                handleResult.setRequestId(status.getRequestId());
                handleResult.setCode(status.getCode());
                handleResult.setStatusCode(status.getStatusCode());
                handleResult.setMessage(status.getMessage());
            }
            handleResult.setMessage(e.getMessage());
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            return errorResult;
        } catch (Exception e1) {
            //异常信息
            logger.warn("callOpenAi Exception", e1);
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            handleResult.setMessage(e1.getMessage());
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            return errorResult;
        }

    }

    /**
     * openAi流式输出：执行模型调用
     *
     * @param sessionId
     * @param params
     * @return
     */
    @Override
    public void streamCallOpenAi(String sessionId, OpenAiModelHandleParams params, AbstractOpenAiStreamResultCallBack callBack) {
        logger.info("streamCallOpenAi model:{},sessionId:{}", params.getModel(), sessionId);
        //判断是否走多模态模型
        if (params.getMulti_modal() != null && params.getMulti_modal()) {
            streamCallOpenMultiModal(sessionId, params, callBack);
            return;
        }
        Generation gen = new Generation();
        List<Message> messages = new ArrayList<>();
        List<AiChatSseConnectMessageParams> messageList = params.getMessages();
        for (int kt = 0; kt < messageList.size(); kt++) {
            AiChatSseConnectMessageParams p = messageList.get(kt);
            if (p != null) {
                Message userMsg = Message.builder().role(p.getRole()).content(p.getContent()).build();
                messages.add(userMsg);
            }
        }
        GenerationParam.GenerationParamBuilder builder = GenerationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(params.getApiKey())
                // 此处以qwen-plus为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/getting-started/models
                .model(params.getMapping()).messages(messages).resultFormat(GenerationParam.ResultFormat.MESSAGE).incrementalOutput(true);
        if (params.getMax_tokens() != null && params.getMax_tokens() > 0) {
            builder.maxTokens(params.getMax_tokens());
        }
        if (params.getTop_p() != null) {
            builder.topP(params.getTop_p());
        }
        if (params.getTemperature() != null) {
            builder.temperature(params.getTemperature().floatValue());
        }
        if (params.getPresence_penalty() != null) {
            builder.repetitionPenalty(params.getPresence_penalty().floatValue());
        }
        if (params.getEnable_search() != null) {
            builder.enableSearch(params.getEnable_search());
        }
        if (params.getEnable_thinking() != null) {
            builder.enableThinking(params.getEnable_thinking());
        }

        GenerationParam param = builder.build();
        try {
            logger.info("streamCallOpenAi 获取QPS控制把柄,model:{},sessionId:{}", params.getModel(), sessionId);
            long startTime = System.currentTimeMillis();
            Flowable<GenerationResult> streamCallResult = gen.streamCall(param);
            streamCallResult.blockingForEach(result -> {
                long totalTime = (System.currentTimeMillis() - startTime);
                logger.info("streamCallOpenAi model:{},sessionId:{},result:{},耗时:{}毫秒", params.getModel(), sessionId, JSON.toJSONString(result), totalTime);
                OpenAiChatCompletionResult handleResult = new OpenAiChatCompletionResult();
                handleResult.setId(result.getRequestId());
                handleResult.setModel(params.getMapping());
                GenerationUsage usage = result.getUsage();
                if (usage != null) {
                    OpenAiUsage aiUsage = new OpenAiUsage();
                    aiUsage.setPromptTokens(usage.getInputTokens() == null ? 0 : usage.getInputTokens());
                    aiUsage.setCompletionTokens(usage.getOutputTokens() == null ? 0 : usage.getOutputTokens());
                    aiUsage.setTotalTokens(usage.getTotalTokens() == null ? 0 : usage.getTotalTokens());
                    aiUsage.setCompletionTokensDetails(usage.getOutputTokensDetails() == null ? null : usage.getOutputTokensDetails());
                    handleResult.setUsage(aiUsage);
                }
                List<OpenAiChatCompletionChoice> choices2 = new ArrayList<>();
                GenerationOutput output = result.getOutput();
                if (output != null) {
                    List<GenerationOutput.Choice> choices = output.getChoices();
                    if (choices != null && !choices.isEmpty()) {
                        //处理结果信息
                        for (GenerationOutput.Choice choice : choices) {
                            OpenAiChatCompletionChoice model = new OpenAiChatCompletionChoice();
                            model.setIndex(choice.getIndex());
                            //SDK格式化问题，会存在"null"
                            if (StringUtils.isNotEmpty(choice.getFinishReason()) && !"null".equalsIgnoreCase(choice.getFinishReason())) {
                                model.setFinishReason(choice.getFinishReason());
                            }
                            model.setLogprobs(choice.getLogprobs());
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
                }
                handleResult.setChoices(choices2);
                handleResult.setSessionId(sessionId);
                callBack.callBackOpenAiStream(sessionId, params.getType(), handleResult);
            });
        } catch (ApiException e) {
            //异常信息
            logger.warn("streamCallOpenAi ApiException", e);
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            Status status = e.getStatus();
            if (status != null) {
                errorResult.setRequestId(status.getRequestId());
                handleResult.setRequestId(status.getRequestId());
                handleResult.setCode(status.getCode());
                handleResult.setStatusCode(status.getStatusCode());
                handleResult.setMessage(status.getMessage());
            }
            handleResult.setMessage(e.getMessage());
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            errorResult.setSessionId(sessionId);
            callBack.callBackOpenAiStreamError(sessionId, errorResult);

        } catch (Exception e1) {
            //异常信息
            logger.warn("streamCallOpenAi Exception", e1);
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            handleResult.setMessage(e1.getMessage());
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            errorResult.setSessionId(sessionId);
            callBack.callBackOpenAiStreamError(sessionId, errorResult);
        }
    }

    /**
     * openAi流式输出：执行模型调用(多模态模型)
     *
     * @param sessionId
     * @param params
     * @param callBack
     * @return
     */
    @Override
    public void streamCallOpenMultiModal(String sessionId, OpenAiModelHandleParams params, AbstractOpenAiStreamResultCallBack callBack) {
        logger.info("streamCallOpenMultiModal model:{},sessionId:{}", params.getModel(), sessionId);
        MultiModalConversation conv = new MultiModalConversation();

        List<MultiModalMessage> messages = new ArrayList<>();
        List<AiChatSseConnectMessageParams> messageList = params.getMessages();
        for (int kt = 0; kt < messageList.size(); kt++) {
            AiChatSseConnectMessageParams p = messageList.get(kt);
            if (p != null) {
                List<Map<String, Object>> content = new ArrayList<>();
                Map<String, Object> map = new HashMap<>();
                map.put("text", p.getContent());
                content.add(map);
                MultiModalMessage message = MultiModalMessage.builder()
                        .role(p.getRole())
                        .content(content).build();
                messages.add(message);
            }
        }

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                // 各地域的API Key不同。获取API Key：https://help.aliyun.com/zh/model-studio/get-api-key
                .apiKey(params.getApiKey())
                .model(params.getMapping())  // 此处以qwen3.5-plus为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/models
                .messages(messages)
                .build();

        if (params.getMax_tokens() != null && params.getMax_tokens() > 0) {
            param.setMaxTokens(params.getMax_tokens());
        }
        if (params.getTop_p() != null) {
            param.setTopP(params.getTop_p());
        }
        if (params.getTemperature() != null) {
            param.setTemperature(params.getTemperature().floatValue());
        }
        if (params.getPresence_penalty() != null) {
            param.setRepetitionPenalty(params.getPresence_penalty().floatValue());
        }
        if (params.getEnable_search() != null) {
            param.setEnableSearch(params.getEnable_search());
        }
        if (params.getEnable_thinking() != null) {
            param.setEnableThinking(params.getEnable_thinking());
        }
        try {
            logger.info("streamCallOpenMultiModal 获取QPS控制把柄,model:{},sessionId:{}", params.getModel(), sessionId);
            long startTime = System.currentTimeMillis();
            Flowable<MultiModalConversationResult> streamCallResult = conv.streamCall(param);
            streamCallResult.blockingForEach(result -> {
                long totalTime = (System.currentTimeMillis() - startTime);
                logger.info("streamCallOpenMultiModal model:{},sessionId:{},result:{},耗时:{}毫秒", params.getModel(), sessionId, JSON.toJSONString(result), totalTime);
                OpenAiChatCompletionResult handleResult = new OpenAiChatCompletionResult();
                handleResult.setId(result.getRequestId());
                handleResult.setModel(params.getMapping());
                MultiModalConversationUsage usage = result.getUsage();
                if (usage != null) {
                    OpenAiUsage aiUsage = new OpenAiUsage();
                    aiUsage.setPromptTokens(usage.getInputTokens() == null ? 0 : usage.getInputTokens());
                    aiUsage.setCompletionTokens(usage.getOutputTokens() == null ? 0 : usage.getOutputTokens());
                    aiUsage.setTotalTokens(usage.getTotalTokens() == null ? 0 : usage.getTotalTokens());
                    aiUsage.setCompletionTokensDetails(usage.getOutputTokensDetails() == null ? null : usage.getOutputTokensDetails());
                    handleResult.setUsage(aiUsage);
                }
                List<OpenAiChatCompletionChoice> choices2 = new ArrayList<>();
                MultiModalConversationOutput output = result.getOutput();
                if (output != null) {
                    List<MultiModalConversationOutput.Choice> choices = output.getChoices();
                    if (choices != null && !choices.isEmpty()) {
                        //处理结果信息
                        for (int kt = 0; kt < choices.size(); kt++) {
                            MultiModalConversationOutput.Choice choice = choices.get(kt);
                            OpenAiChatCompletionChoice model = new OpenAiChatCompletionChoice();
                            model.setIndex(kt);
                            //SDK格式化问题，会存在"null"
                            if (StringUtils.isNotEmpty(choice.getFinishReason()) && !"null".equalsIgnoreCase(choice.getFinishReason())) {
                                model.setFinishReason(choice.getFinishReason());
                            }
                            MultiModalMessage message = choice.getMessage();
                            if (message != null) {
                                OpenAiChatMessage chatMessage = new OpenAiChatMessage();
                                chatMessage.setRole(message.getRole());
                                List<Map<String, Object>> content = message.getContent();
                                if (content != null && !content.isEmpty()) {
                                    Map<String, Object> m0 = content.get(0);
                                    if (m0 != null) {
                                        Object text = m0.get("text");
                                        if (text instanceof String) {
                                            chatMessage.setContent((String) text);
                                        } else {
                                            String msg = JSON.toJSONString(text);
                                            chatMessage.setContent(msg);
                                        }
                                    }
                                }
                                chatMessage.setReasoningContent(message.getReasoningContent());
                                chatMessage.setToolCalls(message.getToolCalls());
                                chatMessage.setToolCallId(message.getToolCallId());
                                model.setDelta(chatMessage);
                            }
                            choices2.add(model);
                        }
                    }
                }
                handleResult.setChoices(choices2);
                handleResult.setSessionId(sessionId);
                callBack.callBackOpenAiStream(sessionId, params.getType(), handleResult);
            });
        } catch (ApiException e) {
            //异常信息
            logger.warn("streamCallOpenMultiModal ApiException", e);
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            Status status = e.getStatus();
            if (status != null) {
                errorResult.setRequestId(status.getRequestId());
                handleResult.setRequestId(status.getRequestId());
                handleResult.setCode(status.getCode());
                handleResult.setStatusCode(status.getStatusCode());
                handleResult.setMessage(status.getMessage());
            }
            handleResult.setMessage(e.getMessage());
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            errorResult.setSessionId(sessionId);
            callBack.callBackOpenAiStreamError(sessionId, errorResult);

        } catch (Exception e1) {
            //异常信息
            logger.warn("streamCallOpenMultiModal Exception", e1);
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            handleResult.setMessage(e1.getMessage());
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            errorResult.setSessionId(sessionId);
            callBack.callBackOpenAiStreamError(sessionId, errorResult);
        }
    }
}
