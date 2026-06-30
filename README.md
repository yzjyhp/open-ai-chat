
对接各大模型厂商公共项目
======================

## 一、核心对接模型接口解释
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

## 二、多轮对话实现模式
### 工作原理
实现多轮对话的核心是维护一个 messages 数组。每一轮对话都需要将用户的最新提问和模型的回复追加到此数组中，并将其作为下一次请求的输入。<br>

以下示例为多轮对话时 messages 的状态变化：<br>
#### 第一轮对话
向messages 数组添加用户问题。<br>
```java
// 使用文本模型
    [
        {"role": "user", "content": "推荐一部关于太空探索的科幻电影。"}
    ]

// 使用多模态模型，以 Qwen-VL 为例
// {"role": "user",
//       "content": [{"type": "image_url","image_url": {"url": "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20251031/ownrof/f26d201b1e3f4e62ab4a1fc82dd5c9bb.png"}},
//                   {"type": "text", "text": "请问图片展现了有哪些商品？"}]
// }
```


#### 第二轮对话
向messages数组添加大模型回复内容与用户的最新提问。<br>
```java
// 使用文本模型
[
        {"role": "user", "content": "推荐一部关于太空探索的科幻电影。"},
        {"role": "assistant", "content": "我推荐《xxx》，这是一部经典的科幻作品。"},
        {"role": "user", "content": "这部电影的导演是谁？"}
        ]

// 使用多模态模型，以 Qwen-VL 为例
//[
//    {"role": "user", "content": [
//                    {"type": "image_url","image_url": {"url": "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20251031/ownrof/f26d201b1e3f4e62ab4a1fc82dd5c9bb.png"}},
//                   {"type": "text", "text": "请问图片展现了有哪些商品？"}]},
//    {"role": "assistant", "content": "图片展示了三件商品：一件浅蓝色背带裤、一件蓝白条纹短袖衬衫和一双白色运动鞋。"},
//    {"role": "user", "content": "它们属于什么风格？"}
//]
```

