package com.yzjyhp.ai.common.queue;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 构建监控上下文
 *
 * @since 0.1.0
 */

public class AiQueueInfo {


    /**
     * 存放采取后的上报数据队列
     */
    private final ArrayBlockingQueue<AbstractTask> nodeQueue;
    private final Logger logger = LoggerFactory.getLogger(AiQueueInfo.class);
    /**
     * 上传线程数
     */
    private int uploadThreads;

    public AiQueueInfo() {
        nodeQueue = new ArrayBlockingQueue<AbstractTask>(1000000);
        uploadThreads = 4;
        // 启动上传线程
        for (int i = 0; i < uploadThreads; i++) {
            PushAssistantQueueInfoTask task = new PushAssistantQueueInfoTask(this, "AiQueueInfo-Thread-" + i);
            task.setDaemon(true);
            task.start();
        }
    }

    /**
     * 保存到日志队列中,如果对列已满将直接放弃
     *
     * @param traceNode
     */
    public void storeNode(AbstractTask traceNode) {
        if (!nodeQueue.offer(traceNode)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error("storeNode异常 ",e);
                Thread.currentThread().interrupt();
            }catch (Exception e) {
                logger.error("storeNode异常 ",e);
            }
            if (!nodeQueue.offer(traceNode)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    logger.error("storeNode异常 ",e);
                    Thread.currentThread().interrupt();
                }catch (Exception e) {
                    logger.error("storeNode异常 ",e);
                }
                if (nodeQueue.offer(traceNode)) {
                    logger.warn("PushAssistantQueueInfo异常,对列已满:" + traceNode.toString());
                }
            }
        }
    }

    /**
     * 节点上传
     */
    public int uploadNode(int size) {
        List<AbstractTask> list = new ArrayList<>(size + 1);
        AbstractTask first = null;
        try {
            first = nodeQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }catch (Exception e) {
            return 0;
        }
        nodeQueue.drainTo(list, size);
        list.add(first);
        for (int ktt = 0; ktt < list.size(); ktt++) {
            try {
                AbstractTask task = list.get(ktt);
                if (task != null) {
                    task.execute();
                }
            } catch (Exception e) {
                logger.error("This pushMessageBatch TaskCallBack is error,error:{} ", e.getMessage());
            }
        }
        return list.size();
    }

    /**
     * 获取队列线程
     *
     * @since 0.1.0
     */
    public class PushAssistantQueueInfoTask extends Thread {
        private final Logger logger = LoggerFactory.getLogger(PushAssistantQueueInfoTask.class);
        private AiQueueInfo context;

        public PushAssistantQueueInfoTask(AiQueueInfo context, String name) {
            this.setName(name);
            this.context = context;
        }

        @Override
        public void run() {
            for (; ; ) {
                try {
                    context.uploadNode(100);
                } catch (Throwable e) {
                    logger.error("upload Task error", e);
                }
            }
        }
    }

}
