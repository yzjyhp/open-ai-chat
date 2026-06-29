package com.yzjyhp.ai.common.queue;

/**
 * @Description
 * @Author yzjyhp
 * @Date 2020/7/31 9:30
 * @Version v1.2.0
 **/
public interface AbstractTask {

    /**
     * 线程池执行回调
     */
    void execute();
}
