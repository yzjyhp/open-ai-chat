package com.yzjyhp.ai.open.util;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @author huaxin.guo
 */
@Slf4j
public class SseEmitterUtil {
    public static final long KEEP_ALIVE_TIME = 300000L;
    public static final long RECONNECT_TIME = 5 * 1000L;

    /**
     * 使用 map 对象缓存sessionId
     */
    private static final Map<String, SseEmitter> SSE_EMITTER_MAP = new ConcurrentHashMap<>();

    private SseEmitterUtil() {

    }

    // 全局单例定时线程池,核心线程数按需调整,不要无限制创建ScheduledExecutor
    private static final ScheduledExecutorService GLOBAL_SCHEDULER = Executors.newScheduledThreadPool(20);
    // 存储emitter与定时任务映射,用于连接关闭时快速取消定时
    private static final Map<SseEmitter, ScheduledFuture<?>> EMITTER_TASK_MAP = new ConcurrentHashMap<>();
    // 心跳间隔 25秒（小于SLB默认60s空闲超时）
    private static final long HEARTBEAT_INTERVAL_SEC = 25;
    // SSE 连接最大存活时长兜底 5分钟,避免永久挂连接
    private static final long MAX_LIVE_MILLIS = 300_000L;

    public static Map<String, SseEmitter> getSseEmitterMap() {
        return SSE_EMITTER_MAP;
    }

    /**
     * 获取长连接
     *
     * @param sessionId 用户ID
     * @return SseEmitter
     */
    public static SseEmitter getSseEmitter(String sessionId) {
        Map<String, SseEmitter> sseEmitterMap = SseEmitterUtil.getSseEmitterMap();
        if (sseEmitterMap == null || sseEmitterMap.isEmpty()) {
            return null;
        }
        SseEmitter connect = sseEmitterMap.get(sessionId);
        if (connect == null) {
            return null;
        }
        return connect;
    }

    /**
     * 创建连接
     *
     * @param sessionId 用户ID
     * @return SseEmitter
     */
    public static SseEmitter connect(String sessionId) {
        return customConnect(sessionId, KEEP_ALIVE_TIME);
    }

    /**
     * 创建连接
     *
     * @param sessionId 用户ID
     * @return SseEmitter
     */
    public static SseEmitter customConnect(String sessionId, Long aliveTime) {
        // 设置超时时间，0表示不过期。超过aliveTime时间未完成会抛出异常：AsyncRequestTimeoutException
        SseEmitter sseEmitter = new SseEmitter(aliveTime);
        // 注册回调
        sseEmitter.onCompletion(completionCallBack(sessionId));
        sseEmitter.onError(errorCallBack(sessionId));
        sseEmitter.onTimeout(timeoutCallBack(sessionId));
        // 缓存
        SSE_EMITTER_MAP.put(sessionId, sseEmitter);
        log.info("创建新的sse连接,当前客户端用户会话Id:{}", sessionId);
        return sseEmitter;
    }

    /**
     * 创建连接
     *
     * @param sessionId 用户ID
     * @return SseEmitter
     */
    public static SseEmitter customConnectHeartbeat(String sessionId, Long aliveTime) {
        // 设置超时时间,0表示不过期。超过aliveTime时间未完成会抛出异常:AsyncRequestTimeoutException
        SseEmitter sseEmitter = new SseEmitter(aliveTime);
        // 注册回调和心跳推送
        bindHeartbeat(sseEmitter, sessionId);

        // 缓存
        SSE_EMITTER_MAP.put(sessionId, sseEmitter);
        log.info("创建带心跳机制的新的sse连接,当前客户端用户会话Id:{}", sessionId);
        return sseEmitter;
    }

    /**
     * 绑定心跳定时任务到SseEmitter
     * @param emitter 当前会话sse发射器
     * @param sessionId 当前会话
     */
    public static void bindHeartbeat(SseEmitter emitter, String sessionId) {
        AtomicBoolean taskCanceled = new AtomicBoolean(false);
        long createTime = System.currentTimeMillis();

        // 注册连接完成回调:客户端断开/服务端关闭时销毁定时任务
        emitter.onCompletion(() -> {
            taskCanceled.set(true);
            cancelHeartbeatTask(emitter);
            log.info("SSE连接正常关闭,客户端 SessionId:{},完成发送信息!销毁心跳定时任务", sessionId);
        });

        // 注册连接异常回调（网络中断、SLB断开等）
        emitter.onError((ex) -> {
            taskCanceled.set(true);
            cancelHeartbeatTask(emitter);
            removeSseEmitter(sessionId, ex);
            log.warn("SSE连接异常断开,销毁心跳定时任务,客户端SessionId:{},异常信息:{}", sessionId, ex.getMessage());
        });

        // 注册连接超时回调
        emitter.onTimeout(() -> {
            taskCanceled.set(true);
            cancelHeartbeatTask(emitter);
            //抛出异常后,解析问题,可以不抛出直接关闭即可
            removeSseEmitter(sessionId, null);
            log.warn("SSE本地服务超时,销毁心跳定时任务,客户端SessionId:{}", sessionId);
        });

        // 提交心跳定时任务
        ScheduledFuture<?> heartbeatTask = GLOBAL_SCHEDULER.scheduleAtFixedRate(() -> {
            // 任务已取消直接跳过
            if (taskCanceled.get()) {
                return;
            }
            // 超过最大存活时间主动关闭连接
            if (System.currentTimeMillis() - createTime > MAX_LIVE_MILLIS) {
                log.info("SSE连接达到最大存活时长,主动关闭会话,sessionId:{}", sessionId);
                emitter.complete();
                return;
            }
            try {
                // SSE comment 发送心跳,格式 : ping\n\n,前端不会收到data业务数据
                String id = UUID.randomUUID().toString().replace("-", "");
                SseEmitter.SseEventBuilder pingEvent = SseEmitter.event().id(id)
                        .comment("ping");
                JSONObject result = new JSONObject();
                result.put("sessionId", sessionId);
                result.put("time", System.currentTimeMillis());
                ObjectMapper mapper = new ObjectMapper();
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                pingEvent.data(mapper.writeValueAsString(result), org.springframework.http.MediaType.APPLICATION_JSON);
                emitter.send(pingEvent);
                log.info("发送SSE心跳ping成功 sessionId:{}", sessionId);
            } catch (IOException e) {
                // IO异常=客户端连接已断开,关闭连接并销毁定时
                log.warn("发送SSE心跳失败,客户端连接已断开,关闭emitter,sessionId:{},异常:{}", sessionId, e.getMessage());
                taskCanceled.set(true);
                emitter.complete();
            } catch (Exception e) {
                // 兜底捕获所有未知异常,防止定时线程崩溃
                log.warn("SSE心跳发送未知异常,sessionId:{},异常:{}", sessionId, e.getMessage());
                log.error("SSE心跳发送未知异常", e);
                taskCanceled.set(true);
                emitter.complete();
            }
        }, 0, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);

        // 缓存定时任务句柄,用于后续取消
        EMITTER_TASK_MAP.put(emitter, heartbeatTask);
    }

    /**
     * 移除用户连接
     */
    public static void removeSession(String sessionId, Throwable throwable) {
        removeSseEmitter(sessionId, throwable);
        log.info("移除用户客户端Session:{}", sessionId);
    }

    /**
     * 移除用户连接-前端主动断开连接的时候使用
     */
    public static void removeSseEmitter(String sessionId, Throwable throwable) {
        try {
            SseEmitter emitter = SSE_EMITTER_MAP.get(sessionId);
            if (Objects.nonNull(emitter)) {
                if (Objects.nonNull(throwable)) {
                    emitter.completeWithError(throwable);
                } else {
                    emitter.complete();
                }
            }
        } catch (Exception e) {
        }
        SSE_EMITTER_MAP.remove(sessionId);
        log.info("断开连接的时候 移除用户客户端Session:{}", sessionId);
    }

    private static Runnable completionCallBack(String sessionId) {
        return () ->
                log.info("客户端SessionId:{},完成发送信息!", sessionId);

    }

    private static Runnable timeoutCallBack(String sessionId) {
        return () -> {
            log.info("客户端SessionId:{},连接超时!", sessionId);
            //抛出异常后,解析问题,可以不抛出直接关闭即可
            removeSseEmitter(sessionId, null);
        };
    }

    private static Consumer<Throwable> errorCallBack(String sessionId) {
        return throwable -> {
            log.info("客户端SessionId:{},发生异常!,异常:{}", sessionId, throwable.getMessage());
            removeSseEmitter(sessionId, throwable);
        };
    }

    /**
     * 取消指定emitter对应的心跳定时任务
     */
    private static void cancelHeartbeatTask(SseEmitter emitter) {
        ScheduledFuture<?> task = EMITTER_TASK_MAP.remove(emitter);
        if (task != null && !task.isDone()) {
            // 立即中断正在执行的心跳任务
            task.cancel(true);
        }
    }

    /**
     * 程序关闭时销毁全局线程池（项目销毁钩子调用）
     */
    public static void shutdownScheduler() {
        log.info("关闭SSE全局心跳定时线程池");
        GLOBAL_SCHEDULER.shutdown();
        try {
            if (!GLOBAL_SCHEDULER.awaitTermination(10, TimeUnit.SECONDS)) {
                GLOBAL_SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            GLOBAL_SCHEDULER.shutdownNow();
        }
    }
}
