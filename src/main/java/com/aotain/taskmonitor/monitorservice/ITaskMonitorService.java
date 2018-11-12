package com.aotain.taskmonitor.monitorservice;

import java.util.Map;

/**
 * 任务监控服务接口定义
 *
 * @author daiyh@aotain.com
 * @date 2018/02/05
 */
public interface ITaskMonitorService {

    /**
     * 服务初始化
     * @param cfg
     * @throws Exception
     */
    void init(Map<String,Object> cfg) throws Exception;

    /**
     * 服务启动
     */
    void startup();

    /**
     * 服务关闭
     */
    void shutdown();

    /**
     * 任务线程是否正常
     * @return
     */
    boolean isThreadError();
}
