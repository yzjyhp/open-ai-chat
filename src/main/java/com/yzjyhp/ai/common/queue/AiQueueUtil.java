package com.yzjyhp.ai.common.queue;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 构建监控上下文
 *
 * @since 0.1.0
 */
public class AiQueueUtil {

    private final static Logger logger = LoggerFactory.getLogger(AiQueueUtil.class);
    /**
     * 存放采取后的上报数据队列
     */
    private static AiQueueInfo queueInfo;

    static {
        queueInfo = new AiQueueInfo();
    }

    /**
     * 获取Jedis实例
     *
     * @return
     */
    public synchronized static AiQueueInfo queueInfo() {
        try {
            if (queueInfo != null) {
                return queueInfo;
            } else {
                queueInfo = new AiQueueInfo();
                return queueInfo;
            }
        } catch (Exception e) {
            logger.error("获取PushAssistantQueueInfo实例 出错！", e);
        }
        return null;
    }

    public AiQueueUtil() {
        queueInfo();
    }

    public static void execute(AbstractTask traceNode) {
        queueInfo.storeNode(traceNode);
    }

}
