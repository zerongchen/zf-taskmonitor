package com.aotain.taskmonitor;

import com.aotain.common.config.ContextUtil;
import com.aotain.common.config.LocalConfig;
import com.aotain.common.utils.monitorstatistics.ModuleConstant;
import com.aotain.common.utils.monitorstatistics.TypeConstant;
import com.aotain.common.utils.tools.MonitorStatisticsUtils;
import com.aotain.common.utils.tools.Tools;
import com.aotain.common.utils.zookeeper.LeaderLatchClient;
import com.aotain.taskmonitor.monitorservice.ITaskMonitorService;
import com.aotain.taskmonitor.monitorservice.MonitorServiceManager;
import com.aotain.taskmonitor.monitorservice.TaskMonitorServiceFactory;
import com.aotain.taskmonitor.utils.MonitorConfigUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/05
 */
public class Main {

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    private static int DELAY = 60 * 1000;

    private static int INTERVAL = 60 * 1000;

    public  static LeaderLatchClient LSELECTOR;

    private static String ZOOKEEPER_CONNECT_STR_NAME = "zookeeper.connect";
    private static String ZOOKEEPER_SELECT_NAMESPACE = "ZF";

    /**
     * 初始化LeaderSelect
     *
     * @param cfg
     * @return
     */
    private static LeaderLatchClient initLeaderSelect(MonitorConfigUtils cfg) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(LocalConfig.getInstance().getHashValueByHashKey(ZOOKEEPER_CONNECT_STR_NAME))
                .retryPolicy(retryPolicy).sessionTimeoutMs(cfg.getSelectSessionTimeout().intValue())
                .connectionTimeoutMs(cfg.getSelectConnectTimeout().intValue()).namespace(ZOOKEEPER_SELECT_NAMESPACE)
                .build();
        client.start();

        cfg.setSelectName(Tools.getHostName()+"-"+cfg.getSelectName());
        final LeaderLatchClient curatorClient = new LeaderLatchClient(client, cfg.getSelectPath(), cfg.getSelectName());
        try {
            curatorClient.start();
            LSELECTOR = curatorClient;
            return curatorClient;
        } catch (Exception e) {
            logger.error("zookeeper leader select init exception, service will be exit", e);
            MonitorStatisticsUtils.addEvent(e);
            return null;
        }
    }

    public static void main(String[] args) {

        try {
            // spring-task-monitor.xml
            new ClassPathXmlApplicationContext("classpath*:spring-task-monitor.xml");
            MonitorStatisticsUtils.initModuleALL(ModuleConstant.MODULE_TASK_MONITOR);

            // 加载项目配置
            MonitorConfigUtils cfg = ContextUtil.getContext().getBean(MonitorConfigUtils.class);
            final LeaderLatchClient lselector = initLeaderSelect(cfg);

            if (lselector == null) {
                logger.error("service shutdown with error");
                System.exit(-1); // 强制退出
            }

            // 创建服务管理器
            final MonitorServiceManager manager = new MonitorServiceManager();

            // 1. redis hash任务监控服务注册
            ITaskMonitorService monitorService = TaskMonitorServiceFactory
                    .createJavaProcessMonitor(1);
            if (monitorService == null) {
                logger.error(
                        "dpi task monitor service[redis hash task] server start error, nonsupport monitor config service type(${monitor.type}):"
                                + 1);
                return;
            }
            MonitorStatisticsUtils.addEvent(ModuleConstant.MODULE_TASK_MONITOR, TypeConstant.EXCEPTION_TYPE_THREAD, 0);
            // 注册异常处理
            try {
                manager.register(monitorService, null);
            } catch (Exception e) {
                logger.error("dpi task monitor service[redis hash task] server register exception", e);
                MonitorStatisticsUtils.addEvent(e);
            }

            // 2. 其它服务注册...

            // 服务启动
            manager.startup();
            logger.info("dpi task monitor service server start success");

            // 注册退出事件响应
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                manager.shutdown(); // 关闭服务
                }
            });

            threadMonitor(manager);
        } catch (Exception e) {
            logger.error("dpi task monitor service server start error", e);
            MonitorStatisticsUtils.addEvent(e);
        }

    }

    /**
     * 写线程异常监控
     *
     * @param manager
     */
    private static void threadMonitor(final MonitorServiceManager manager) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                int count = MonitorStatisticsUtils.getQuartErrorThreadCount();
                int mainCount = manager.getErrorThreadCount();
                if (count == -1) {
                    logger.error("thread monitor exception - quartz task monitor error(count is -1).");
                    count = 0;
                }
                if (mainCount == -1) {
                    logger.error("thread monitor exception - main task monitor error(count is -1).");
                    mainCount = 0;
                }
                int totalCount = mainCount + count;
                if (totalCount > 0) {
                    logger.error("thread monitor exception , error count is " + totalCount);
                } else {
                    logger.debug("thread monitor exception , error count is " + totalCount);
                }
                MonitorStatisticsUtils.addEvent(ModuleConstant.MODULE_TASK_MONITOR,
                        TypeConstant.EXCEPTION_TYPE_THREAD, totalCount);
            }

        }, DELAY, INTERVAL);
    }

}
