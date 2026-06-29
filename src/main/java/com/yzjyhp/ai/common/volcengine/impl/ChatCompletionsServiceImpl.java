package com.yzjyhp.ai.common.volcengine.impl;

import com.alibaba.fastjson.JSON;
import com.yzjyhp.ai.common.vo.OpenAiModelErrorResult;
import com.yzjyhp.ai.common.vo.OpenAiModelHandleErrorResult;
import com.yzjyhp.ai.common.vo.OpenAiModelHandleParams;
import com.yzjyhp.ai.common.volcengine.ChatCompletionsService;
import com.yzjyhp.ai.open.model.OpenAiChatCompletionChoice;
import com.yzjyhp.ai.open.model.OpenAiChatCompletionResult;
import com.yzjyhp.ai.open.model.OpenAiChatMessage;
import com.yzjyhp.ai.open.model.OpenAiUsage;
import com.yzjyhp.ai.open.service.AbstractOpenAiStreamResultCallBack;
import com.yzjyhp.ai.open.vo.AiChatSseConnectMessageParams;
import com.volcengine.ark.runtime.exception.ArkHttpException;
import com.volcengine.ark.runtime.model.Usage;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChunk;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionResult;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import io.reactivex.Flowable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Description:火山方舟大模型服务
 * Date: 2025/4/16 14:07
 *
 * @version 1.0.0
 * @author: yzjyhp
 */
@Service
public class ChatCompletionsServiceImpl implements ChatCompletionsService {

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
        String apiKey = params.getApiKey();
        ArkService service = ArkService.builder().apiKey(apiKey).build();
        final List<ChatMessage> messages = new ArrayList<>();
        List<AiChatSseConnectMessageParams> messageList = params.getMessages();
        for (int kt = 0; kt < messageList.size(); kt++) {
            AiChatSseConnectMessageParams p = messageList.get(kt);
            if (p != null) {
                final ChatMessage userMessage = ChatMessage.builder().role(getRole(p.getRole())).content(p.getContent()).build();
                messages.add(userMessage);
            }
        }
        ChatCompletionRequest.Builder builder = ChatCompletionRequest.builder().model(params.getMapping()).messages(messages);
        if (params.getMax_tokens() != null && params.getMax_tokens() > 0) {
            builder.maxTokens(params.getMax_tokens());
        }
        if (params.getTop_p() != null) {
            builder.topP(params.getTop_p());
        }
        if (params.getTemperature() != null) {
            builder.temperature(params.getTemperature());
        }
        if (params.getPresence_penalty() != null) {
            builder.presencePenalty(params.getPresence_penalty());
        }
        if (params.getEnable_thinking() != null) {
            //enabled：开启思考模式，模型一定先思考后回答。
            //disabled：关闭思考模式，模型直接回答问题，不会进行思考。
            //auto：自动思考模式，模型根据问题自主判断是否需要思考，简单题目直接回答。
            if (params.getEnable_thinking()) {
                builder.thinking(new ChatCompletionRequest.ChatCompletionRequestThinking("enabled"));
            }
        }
        ChatCompletionRequest chatCompletionRequest = builder.build();
        try {
            long totalTime = (System.currentTimeMillis() - startTime);
            logger.info("callOpenAi 获取QPS控制把柄,model:{},sessionId:{},耗时:{}毫秒", params.getModel(), sessionId, totalTime);
            long startTime2 = System.currentTimeMillis();
            OpenAiChatCompletionResult handleResult = new OpenAiChatCompletionResult();
            ChatCompletionResult result = service.createChatCompletion(chatCompletionRequest);
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
            handleResult.setServiceTier(result.getServiceTier());
            handleResult.setObject(result.getObject());
            Usage usage = result.getUsage();
            if (usage != null) {
                OpenAiUsage aiUsage = new OpenAiUsage();
                aiUsage.setPromptTokens(usage.getPromptTokens());
                aiUsage.setCompletionTokens(usage.getCompletionTokens());
                aiUsage.setTotalTokens(usage.getTotalTokens());
                aiUsage.setPromptTokensDetails(usage.getPromptTokensDetails());
                aiUsage.setCompletionTokensDetails(usage.getCompletionTokensDetails());
                handleResult.setUsage(aiUsage);
            }
            //处理结果信息
            List<OpenAiChatCompletionChoice> choices2 = new ArrayList<>();
            List<ChatCompletionChoice> choices = result.getChoices();
            if (choices != null && !choices.isEmpty()) {
                //处理结果信息
                for (ChatCompletionChoice choice : choices) {
                    OpenAiChatCompletionChoice model = new OpenAiChatCompletionChoice();
                    model.setIndex(choice.getIndex());
                    if (StringUtils.isNotEmpty(choice.getFinishReason())) {
                        model.setFinishReason(choice.getFinishReason());
                    }
                    model.setModerationHitType(choice.getModerationHitType());
                    model.setLogprobs(choice.getLogprobs());
                    ChatMessage message = choice.getMessage();
                    if (message != null) {
                        OpenAiChatMessage chatMessage = new OpenAiChatMessage();
                        chatMessage.setRole(message.getRole().value());
                        chatMessage.setContent(message.getContent());
                        chatMessage.setReasoningContent(message.getReasoningContent());
                        chatMessage.setFunctionCall(message.getFunctionCall());
                        chatMessage.setToolCalls(message.getToolCalls());
                        chatMessage.setToolCallId(message.getToolCallId());
                        model.setDelta(chatMessage);
                    }
                    choices2.add(model);
                }
            }
            handleResult.setChoices(choices2);
            // shutdown service
            service.shutdownExecutor();
            totalTime = (System.currentTimeMillis() - startTime);
            logger.info("callOpenAi model:{},sessionId:{},返回耗时:{}毫秒", params.getModel(), sessionId, totalTime);
            return handleResult;
        } catch (ArkHttpException e) {
            logger.warn("callOpenAi ArkHttpException", e);
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            errorResult.setRequestId(e.requestId);
            handleResult.setRequestId(e.requestId);
            handleResult.setMessage(e.getMessage());
            handleResult.setCode(e.code);
            handleResult.setParam(e.param);
            handleResult.setType(e.type);
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            // shutdown service
            service.shutdownExecutor();
            return errorResult;
        } catch (Exception e) {
            logger.warn("callOpenAi Exception", e);
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            handleResult.setMessage(e.getMessage());
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            // shutdown service
            service.shutdownExecutor();
            return errorResult;
        }
    }

    /**
     * openAI流式输出：执行模型调用
     *
     * @param sessionId
     * @param params
     * @return
     */
    @Override
    public void streamCallOpenAi(String sessionId, OpenAiModelHandleParams params, AbstractOpenAiStreamResultCallBack callBack) {
        logger.info("streamCallOpenAi model:{},sessionId:{}", params.getModel(), sessionId);
        String apiKey = params.getApiKey();
        //stream_options
        ArkService service = ArkService.builder().apiKey(apiKey).build();
        final List<ChatMessage> messages = new ArrayList<>();
        List<AiChatSseConnectMessageParams> messageList = params.getMessages();
        for (int kt = 0; kt < messageList.size(); kt++) {
            AiChatSseConnectMessageParams p = messageList.get(kt);
            if (p != null) {
                final ChatMessage userMessage = ChatMessage.builder().role(getRole(p.getRole())).content(p.getContent()).build();
                messages.add(userMessage);
            }
        }
        ChatCompletionRequest.Builder builder =
                ChatCompletionRequest.builder().streamOptions(new ChatCompletionRequest.ChatCompletionRequestStreamOptions(true)).model(params.getMapping()).messages(messages);
        if (params.getMax_tokens() != null && params.getMax_tokens() > 0) {
            builder.maxTokens(params.getMax_tokens());
        }
        if (params.getTop_p() != null) {
            builder.topP(params.getTop_p());
        }
        if (params.getTemperature() != null) {
            builder.temperature(params.getTemperature());
        }
        if (params.getPresence_penalty() != null) {
            builder.presencePenalty(params.getPresence_penalty());
        }
        if (params.getEnable_thinking() != null) {
            //enabled：开启思考模式，模型一定先思考后回答。
            //disabled：关闭思考模式，模型直接回答问题，不会进行思考。
            //auto：自动思考模式，模型根据问题自主判断是否需要思考，简单题目直接回答。
            if (params.getEnable_thinking()) {
                builder.thinking(new ChatCompletionRequest.ChatCompletionRequestThinking("enabled"));
            }
        }
        ChatCompletionRequest chatCompletionRequest = builder.build();
        try {
            logger.info("streamCallOpenAi 获取QPS控制把柄,model:{},sessionId:{}", params.getModel(), sessionId);
            long startTime = System.currentTimeMillis();
            Flowable<ChatCompletionChunk> streamCallResult = service.streamChatCompletion(chatCompletionRequest);
            streamCallResult.blockingForEach(result -> {
                long totalTime = (System.currentTimeMillis() - startTime);
                logger.info("streamCallOpenAi model:{},sessionId:{},result:{},耗时:{}毫秒", params.getModel(), sessionId, JSON.toJSONString(result), totalTime);
                OpenAiChatCompletionResult handleResult = new OpenAiChatCompletionResult();
                handleResult.setId(result.getId());
                handleResult.setCreated(result.getCreated());
                handleResult.setModel(result.getModel());
                handleResult.setServiceTier(result.getServiceTier());
                handleResult.setObject(result.getObject());
                Usage usage = result.getUsage();
                if (usage != null) {
                    OpenAiUsage aiUsage = new OpenAiUsage();
                    aiUsage.setPromptTokens(usage.getPromptTokens());
                    aiUsage.setCompletionTokens(usage.getCompletionTokens());
                    aiUsage.setTotalTokens(usage.getTotalTokens());
                    aiUsage.setPromptTokensDetails(usage.getPromptTokensDetails());
                    aiUsage.setCompletionTokensDetails(usage.getCompletionTokensDetails());
                    handleResult.setUsage(aiUsage);
                }
                List<OpenAiChatCompletionChoice> choices2 = new ArrayList<>();
                List<ChatCompletionChoice> choices = result.getChoices();
                if (choices != null && !choices.isEmpty()) {
                    //处理结果信息
                    for (ChatCompletionChoice choice : choices) {
                        OpenAiChatCompletionChoice model = new OpenAiChatCompletionChoice();
                        model.setIndex(choice.getIndex());
                        if (StringUtils.isNotEmpty(choice.getFinishReason())) {
                            model.setFinishReason(choice.getFinishReason());
                        }
                        model.setModerationHitType(choice.getModerationHitType());
                        model.setLogprobs(choice.getLogprobs());
                        ChatMessage message = choice.getMessage();
                        if (message != null) {
                            OpenAiChatMessage chatMessage = new OpenAiChatMessage();
                            chatMessage.setRole(message.getRole().value());
                            chatMessage.setContent(message.getContent());
                            chatMessage.setReasoningContent(message.getReasoningContent());
                            chatMessage.setFunctionCall(message.getFunctionCall());
                            chatMessage.setToolCalls(message.getToolCalls());
                            chatMessage.setToolCallId(message.getToolCallId());
                            model.setDelta(chatMessage);
                        }
                        choices2.add(model);
                    }
                }
                handleResult.setChoices(choices2);
                handleResult.setSessionId(sessionId);
                callBack.callBackOpenAiStream(sessionId, params.getType(), handleResult);
            });
            // shutdown service
            service.shutdownExecutor();

        } catch (ArkHttpException e) {
            logger.warn("streamCallOpenAi ArkHttpException", e);
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            errorResult.setRequestId(e.requestId);
            handleResult.setRequestId(e.requestId);
            handleResult.setMessage(e.getMessage());
            handleResult.setCode(e.code);
            handleResult.setParam(e.param);
            handleResult.setType(e.type);
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            // shutdown service
            service.shutdownExecutor();
            errorResult.setSessionId(sessionId);
            callBack.callBackOpenAiStreamError(sessionId, errorResult);
        } catch (Exception e) {
            logger.warn("streamCallOpenAi Exception", e);
            OpenAiModelErrorResult errorResult = new OpenAiModelErrorResult();
            OpenAiModelHandleErrorResult handleResult = new OpenAiModelHandleErrorResult();
            //异常信息
            handleResult.setMessage(e.getMessage());
            handleResult.setStatusCode(-1);
            errorResult.setError(handleResult);
            // shutdown service
            service.shutdownExecutor();
            errorResult.setSessionId(sessionId);
            callBack.callBackOpenAiStreamError(sessionId, errorResult);
        }

    }

    private ChatMessageRole getRole(String role) {
        if (StringUtils.isEmpty(role)) {
            return ChatMessageRole.USER;
        }
        String value = role;
        if ("system".equals(value)) {
            return ChatMessageRole.SYSTEM;
        } else if ("user".equals(value)) {
            return ChatMessageRole.USER;
        } else if ("assistant".equals(value)) {
            return ChatMessageRole.ASSISTANT;
        } else if ("function".equals(value)) {
            return ChatMessageRole.FUNCTION;
        } else if ("tool".equals(value)) {
            return ChatMessageRole.TOOL;
        }
        return ChatMessageRole.USER;
    }
}
