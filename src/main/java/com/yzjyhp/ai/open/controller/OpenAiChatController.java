package com.yzjyhp.ai.open.controller;


import com.alibaba.fastjson.JSON;
import com.yzjyhp.ai.open.service.OpenAiChatService;
import com.yzjyhp.ai.open.vo.AiChatSseConnectParams;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@RestController
@RequestMapping("/open/ai/chat")
@Api(value = "/open/ai/chat", description = "OpenAi模式:AI实时对话", tags = {"OpenAi模式:AI Chat"})
public class OpenAiChatController {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private OpenAiChatService openAiChatService;

    @ApiOperation("AI会话-支持流式和非流式输出")
    @PostMapping(value = "/message", produces = {MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public Object message(@RequestBody AiChatSseConnectParams params, HttpServletRequest request) {
        logger.info("/open/ai/chat/message params:{}", JSON.toJSONString(params));
        String sessionId = params.getSessionId();
        if (StringUtils.isEmpty(params.getSessionId())) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
            params.setSessionId(sessionId);
        }
        if (params.isStream()) {
            return openAiChatService.streamCallOpenAi(sessionId, params);
        } else {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(openAiChatService.callNonStream(sessionId, params));
        }
    }

}
