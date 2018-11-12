package com.aotain.taskmonitor.monitorservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 监控任务服务管理类
 *   主要用于实现监控任务的初始化 销毁 增加等操作
 *
 * @author daiyh@aotain.com
 * @date 2018/02/05
 */
public class MonitorServiceManager {

    private List<ITaskMonitorService> services = new ArrayList<ITaskMonitorService>();

    private Logger logger = LoggerFactory.getLogger(MonitorServiceManager.class);

    /***
     * 注册一个服务
     *
     * @param service 服务接口
     * @param cfg 配置信息
     * @throws Exception 注册时会进行服务初始化，可能抛出异常
     */
    public void register(ITaskMonitorService service, Map<String, Object> cfg) throws Exception {
        if (service == null) {
            return;
        }
        service.init(cfg);
        services.add(service);
    }

    /**
     * 启动服务
     */
    public void startup() {
        for (final ITaskMonitorService service : services) {
            // 子线程启动服务
            Thread th = new Thread() {
                @Override
                public void run() {
                    service.startup();
                }
            };
            th.start();
        }

    }

    /**
     * 获取异常线程数
     * @return
     */
    public int getErrorThreadCount() {
        if(services == null){
            return -1;
        }
        int count = 0;
        for (ITaskMonitorService service : services) {
            if(service.isThreadError()){
                count++;
            }
        }
        return count;
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        for (ITaskMonitorService service : services) {
            service.shutdown();
        }
    }
}
