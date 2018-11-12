package com.aotain.taskmonitor.monitorservice;

import com.aotain.taskmonitor.monitorservice.impl.TaskMonitorServiceImpl;

/**
 * 任务监控服务构造工厂
 *
 * @author daiyh@aotain.com
 * @date 2018/02/05
 */
public class TaskMonitorServiceFactory {

    private final static int SERVICE_TYPE_JAVA_PROCESS = 1;

    /**
     * 创建Java进程版-任务监控服务
     *
     * @return
     */
    public static ITaskMonitorService createJavaProcessMonitor(int serviceType) {
        switch (serviceType) {
            case SERVICE_TYPE_JAVA_PROCESS:
                return new TaskMonitorServiceImpl();
        }
        return null;
    }
}
