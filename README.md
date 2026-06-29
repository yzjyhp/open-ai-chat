
对接各大模型厂商公共项目
======================

## 三、核心对接模型接口解释
#### 接口地址:/open/ai/chat/message <br>
##### 接口描述:完全转发对接各个厂商模型，返回结构完全按照openai的结构进行返回 <br>

```java
        {
        "appCode": "调用方项目名称",
        "max_tokens": 0,
        "messages": [
            {
                "content": "",
                "role": "SYSTEM"
            }
        ],
        "model": "模型名称",
        "presence_penalty": 0,
        "stream": true,
        "temperature": 0,
        "top_p": 0
        }
```
##### 接口备注:stream=true的时候为流式返回，需要使用长连接SSE域名进行接收，和openai的不同点是解释的时候不会有[done]是正常的stop模式

<br>


