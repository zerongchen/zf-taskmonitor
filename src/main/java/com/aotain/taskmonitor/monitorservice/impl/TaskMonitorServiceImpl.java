package com.aotain.taskmonitor.monitorservice.impl;

import com.alibaba.fastjson.JSON;
import com.aotain.common.config.ContextUtil;
import com.aotain.common.config.redis.BaseRedisService;
import com.aotain.common.policyapi.model.base.BaseVO;
import com.aotain.common.utils.kafka.NoticeQueueUtils;
import com.aotain.common.utils.model.msg.EuSendStatus;
import com.aotain.common.utils.model.msg.NoticeQueueMessage;
import com.aotain.common.utils.model.msg.RedisPolicyAck;
import com.aotain.common.utils.model.msg.RedisTaskStatus;
import com.aotain.common.utils.redis.TaskMessageUtil;
import com.aotain.common.utils.tools.CommonConstant;
import com.aotain.common.utils.tools.MonitorStatisticsUtils;
import com.aotain.taskmonitor.monitorservice.ITaskMonitorService;
import com.aotain.taskmonitor.task.IRedoTask;
import com.aotain.taskmonitor.task.RedoTaskFactory;
import com.aotain.taskmonitor.utils.PolicyRedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/05
 */
public class TaskMonitorServiceImpl implements ITaskMonitorService{

    private BaseRedisService<String,String,String> baseRedisService =
            ContextUtil.getContext().getBean("baseRedisServiceImpl",BaseRedisService.class);

    private ExecutorService executor ;

    private Logger logger = LoggerFactory.getLogger(TaskMonitorServiceImpl.class);

    /**
     * job 任务状态监控扫描时间间隔（单位秒）
     */
    private int intervalTime = 10;

    /**
     * job 任务状态监控扫描处理过程发生异常时，扫描线程休眠时间(单位秒)
     */
    private int sleepTime = 10;

    /**
     *
     */
    private long lastExecTime = -1;

    @Override
    public void init(Map<String, Object> cfg) throws Exception {
        executor = Executors.newFixedThreadPool(3);
    }

    @Override
    public void startup() {
        while (true) {

            lastExecTime = System.currentTimeMillis();
            try {
                // 获取并遍历所有任务
                List<RedisTaskStatus> tasks = TaskMessageUtil.getInstance().getTasks();
                logger.info("TaskMonitorService(JavaProcess) current task count is " + tasks.size());

                for (RedisTaskStatus task : tasks) {
                    // 过滤无效的数据
                    if ( !taskParamsCheck(task) ) {
                        logger.warn("TaskMonitorService(JavaProcess) skip invalid taskData :" + task.toString());

                        // 删除无效数据
                        if( task.getTaskId() != null ){
                            logger.error("remove invalid taskData from redis: "+task.objectToJson());
                            TaskMessageUtil.getInstance().removeTask(task.getTaskId());
                        }
                        continue;
                    }

                    // 1. 初始状态，且下次执行时间大于当前时间时，不必任务处理
                    boolean init = task.getStatus() == CommonConstant.REDIS_TASK_STATUS_START;
                    boolean timeNotReached = task.getNextTime() > (System.currentTimeMillis() / 1000);
                    if ( init && timeNotReached ){
                        continue;
                    }

                    // 2. 重试状态，且下次执行时间大于当前时间时，不必任务处理
                    boolean retry = task.getStatus() == CommonConstant.REDIS_TASK_STATUS_RERTY;
                    if ( retry && timeNotReached ) {
                        continue;
                    }

                    // 3. 处理成功，写入监控队列，并删除hash任务
                    if (task.getStatus() == CommonConstant.REDIS_TASK_STATUS_SUCCESS) {
                        // 移除任务
                        removeTask(task, 1);
                        logger.info(
                                "TaskMonitorService(JavaProcess) remove success task information to monitor queue, taskid="
                                        + task.getTaskId() + ",taskType=" + task.getTaskType() + ",status="
                                        + task.getStatus());
                        continue;
                    }

                    // 4. 处理失败

                    // DPI策略且策略状态为初始化或者重试
                    boolean dpiSpecial = (!task.getTaskType().equals(4))
                            && (  task.getStatus().equals(CommonConstant.REDIS_TASK_STATUS_START)
                                  || task.getStatus().equals(CommonConstant.REDIS_TASK_STATUS_RERTY) );

                    // 4.1. 处理失败，判断是否超出最大尝试次数

                    // 重试次数超过重试次数上限
                    if ( task.getMaxTimes() != 0 && task.getTimes() >= task.getMaxTimes() ) {
                        // 新增超时判断逻辑：若超时，不进行重试，防止多次重试造成紊乱(策略不用直接超时处理)
                        if( dpiSpecial ) {
                            // 按照超时状态移除任务
                            removeTask(task, 4);
                        }else{
                            // 按照超出最大尝试次数移除任务
                            removeTask(task, 2);
                        }
                        logger.info(
                                "TaskMonitorService(JavaProcess) remove fail(out of max try times) task information to monitor queue, taskid="
                                        + task.getTaskId() + ",taskType=" + task.getTaskType() + ",status="
                                        + task.getStatus());
                        continue;
                    }

                    // 4.2 处理失败，如果可无限重试，但已超过有效期
                    if (task.getMaxTimes() == 0 && ((System.currentTimeMillis() / 1000) >= task.getExpireTime())) {
                        // 新增超时判断逻辑：若超时，不进行重试，防止多次重试造成紊乱(策略不用直接超时处理)
                        if( dpiSpecial ) {
                            // 按照超时状态移除任务
                            removeTask(task, 4);
                        }else{
                            // 按照过期移除任务
                            removeTask(task, 3);
                        }
                        logger.info(
                                "TaskMonitorService(JavaProcess) remove fail(out of expire time) task information to monitor queue, taskid="
                                        + task.getTaskId() + ",taskType=" + task.getTaskType() + ",status="
                                        + task.getStatus());
                        continue;
                    }

                    // 5. 重试并更新下次处理时间,如果已经到了重试时间，则进行重试
                    if (task.getNextTime() <= (System.currentTimeMillis() / 1000)) {
                        // 新增超时判断逻辑：若超时，不进行重试，防止多次重试造成紊乱(策略不用直接超时处理)
                        if( dpiSpecial ) {
                            removeTask(task, 4);
                        }else{
                            redoTask(task);
                        }
                    }
                }

                Thread.sleep(intervalTime * 1000);
            } catch (InterruptedException e) { // CPU发来中断信号，任务结束
                logger.error("TaskMonitorService(JavaProcess) has been interrupted, service will be shutdown ...", e);
                MonitorStatisticsUtils.addEvent(e);
                break;
            } catch (Exception e) { // 其它异常，服务休眠1分钟，而后再次启动
                logger.error(
                        "TaskMonitorService(JavaProcess) service has broken by exception, service will sleep 1 minute ...",
                        e);
                MonitorStatisticsUtils.addEvent(e);
                try {
                    Thread.sleep(sleepTime * 1000);
                } catch (InterruptedException e1) { // 服务将被中断
                    logger.error("TaskMonitorService(JavaProcess) has been interrupted, service will be shutdown ...",
                            e1);
                    MonitorStatisticsUtils.addEvent(e);
                    break;
                }
            }
        }
        // 如果收到时钟中断异常后，while循环退出，线程池关闭，总服务关闭
        shutdown();
    }

    @Override
    public void shutdown() {
        if (executor == null) {
            logger.info("TaskMonitorService(JavaProcess) has been shutdown ...");
            return;
        }
        if (executor.isShutdown()) {
            logger.info("TaskMonitorService(JavaProcess) has been shutdown ...");
            executor = null;
            return;
        }
        try {
            logger.info("TaskMonitorService(JavaProcess) try to shutdown ...");
            // 发送关闭命令
            executor.shutdown();

            logger.info("TaskMonitorService(JavaProcess) waiting all task to be finished ...");
            // 等待未完成命令超时
            if (!executor.awaitTermination(60, TimeUnit.MILLISECONDS)) {
                // 等待60秒钟
                logger.info("TaskMonitorService(JavaProcess) try to kill timeout threads ...");
                // 超时的时候向线程池中所有的线程发出中断(interrupted)。
                List<Runnable> timeoutTasklist = executor.shutdownNow();
                logger.info("TaskMonitorService(JavaProcess) interrupted " + timeoutTasklist.size()
                        + " timeout threads  ...");
            }
        } catch (InterruptedException e) {
            // awaitTermination方法被中断的时候也中止线程池中全部的线程的执行。
            logger.info("TaskMonitorService(JavaProcess) awaitTermination interrupted , retry shutdownNow ...", e);
            executor.shutdownNow();
            MonitorStatisticsUtils.addEvent( e);
        }
        logger.info("TaskMonitorService(JavaProcess) has been shutdowned");
    }

    @Override
    public boolean isThreadError() {
        return false;
    }

    private boolean taskParamsCheck(RedisTaskStatus task) {
        if (task.getTaskId() == null) {
            logger.warn("TaskMonitorService(JavaProcess) params check - taskid is null");
            return false;
        }
        if (task.getTopTaskId() == null) {
            // 默认修改为0
            task.setTopTaskId(0L);
        }
        if (task.getTaskType() == null) {
            logger.warn("TaskMonitorService(JavaProcess) params check - tasktype is null");
            return false;
        }
        if (task.getTimes() == null) {
            task.setTimes(1);
        }
        if (task.getInterval() == null) {
            logger.warn("TaskMonitorService(JavaProcess) params check - interval is null");
            return false;
        }
        if (task.getCreateTime() == null) {
            task.setCreateTime(System.currentTimeMillis() / 1000);
            logger.warn("TaskMonitorService(JavaProcess) params check - createtime is null");
        }
        if (task.getMaxTimes() == null) {
            logger.warn("TaskMonitorService(JavaProcess) params check - maxtimes is null");
            return false;
        }
        // 无限重试时必填过期时间
        if (task.getMaxTimes() == 0 && task.getExpireTime() == null) {
            logger.warn("TaskMonitorService(JavaProcess) params check - expiretime is null");
            return false;
        }

        // 下次执行时间
        if (task.getNextTime() == null) {
            task.setNextTime(task.getInterval() + task.getCreateTime());
        }

        // 任务状态，默认为1-开始
        if (task.getStatus() == null) {
            task.setStatus(1);
        }
        return true;
    }

    /**
     * 移除已完成的redis消息：根据不同的hash消息类型做不同的移除任务操作
     *
     * @param task
     * @param type 1-成功的任务,2-超过最大重试次数，3-超过有效期，4-任务执行超时
     * @see com.aotain.common.utils.tools.CommonConstant  taskType定义见此常量
     */
    private void removeTask(RedisTaskStatus task, int type) {
        if(type == 4){
            // 设置为超时状态
            task.setStatus(5);
        }

        switch (task.getTaskType()) {

            // 1-job任务 2-管局ACK 3-文件上报 5-azkaban任务 统一处理 删除redis，写消息队列
            // 4-DPI策略 需要进行个性化处理
            case 1:
            case 2:
            case 3:
            case 5:
                logger.info("removeTask start,and the task content is "+JSON.toJSONString(task));
                NoticeQueueUtils.sendMessage(buildNoticeMessage(task));
                TaskMessageUtil.getInstance().removeTask(task.getTaskId());
                logger.info("remove task from redis success,and the taskId is "+task.getTaskId() );
                break;

            case 4 :
                try {
                    // 1 发送DPI下发状态通知
                    String policyJsonStr = task.getContent();
                    BaseVO policyObj = BaseVO.parseFromJson(policyJsonStr, BaseVO.class);
                    int probeType = policyObj.getProbeType();
                    int messageType = policyObj.getMessageType();
                    Long messageNo = policyObj.getMessageNo();
                    Map<String, Object> acks = PolicyRedisUtils.queryAcks(probeType, messageType, messageNo);
                    for (String dpiIp : acks.keySet()) {
                        String value = (String) acks.get(dpiIp);
                        RedisPolicyAck ack = RedisPolicyAck.parseFrom(value, RedisPolicyAck.class);
                        // 构造EU状态信息
                        EuSendStatus status = new EuSendStatus();
                        status.setIp(dpiIp);
                        status.setMessageNo(messageNo.intValue());
                        status.setMessageType(messageType);
                        status.setPolicyIp(ack.getPolicyIp());
                        status.setProbeType(probeType);
                        status.setStatus(ack.getStatus());

                        // 当ack返回status=0时判断dpi设备是否连接正常
                        modifyEuStatus(status);

                        NoticeQueueMessage message = new NoticeQueueMessage();
                        message.setType(CommonConstant.NOTICE_MESSAGE_TYPE_DPI_POLICY_STATUS);
                        message.setMessage(JSON.toJSONString(status));
                        message.setCreateTime(System.currentTimeMillis() / 1000);
                        // 1.1 发送消息通知
                        NoticeQueueUtils.sendMessage(message);
                    }
                    // 2 从ack队列中删除记录
                    PolicyRedisUtils.delAcks(probeType, messageType, messageNo);
                } catch (Exception e) { // 状态更新失败时，流程不能中断
                    logger.error("TaskMonitorService(JavaProcess) update DPI status exception",e);
                }
                // 删除redis，写消息队列
                logger.info("remove task from redis start,and the taskId is "+task.getTaskId());
                NoticeQueueUtils.sendMessage(buildNoticeMessage(task));
                TaskMessageUtil.getInstance().removeTask(task.getTaskId());
                logger.info("remove task from redis success,and the taskId is "+task.getTaskId() );
                break;

            // 默认删除redis，写消息队列
            default :
                NoticeQueueUtils.sendMessage(buildNoticeMessage(task));
                TaskMessageUtil.getInstance().removeTask(task.getTaskId());
                logger.info("remove task from redis success,and the taskId is "+task.getTaskId() );
                break;
        }

    }

    /**
     * 构造通知消息队列信息
     *
     * @param task
     * @return
     */
    private NoticeQueueMessage buildNoticeMessage(RedisTaskStatus task) {
        NoticeQueueMessage message = new NoticeQueueMessage();
        message.setType(CommonConstant.NOTICE_MESSAGE_TYPE_TASKSTATUS);
        message.setCreateTime(System.currentTimeMillis() / 1000);
        message.setMessage(task.objectToJson());
        return message;
    }

    /**
     * 重做任务
     *
     * @param task
     */
    private void redoTask(final RedisTaskStatus task) {
        final IRedoTask iRedoTask = RedoTaskFactory.createRedoTask(task.getTaskType());

        // 跳过不支持的任务类型
        if (iRedoTask == null) {
            logger.error("TaskMonitorService(JavaProcess) skip nonsupport task type , taskid=" + task.getTaskId()
                    + ",taskType=" + task.getTaskType() + ",status=" + task.getStatus());
            return;
        }

        // 主线程修改任务相关基本参数（子线程中还需要修改每次重做的结果）
        task.setTimes(task.getTimes() + 1);
        task.setNextTime(System.currentTimeMillis() / 1000 + task.getInterval());
        task.setStatus(CommonConstant.REDIS_TASK_STATUS_RERTY);
        // 回写到任务状态中
        TaskMessageUtil.getInstance().setTask(task.getTaskId(), task);

        // 创建重做任务线程
        Thread execTask = new Thread() {
            @Override
            public void run() {
                boolean createSuccess = iRedoTask.redoTask(task);
                if (!createSuccess) {
                    logger.error("TaskMonitorService(JavaProcess) create redo task fail, taskid=" + task.getTaskId());
                    // 重新获取任务信息（防止同时有两个重试任务时，任务参数被第一个重试任务还原）
                    RedisTaskStatus ctask = TaskMessageUtil.getInstance().getTask(task.getTaskId());
                    ctask.setStatus(CommonConstant.REDIS_TASK_STATUS_FAIL);
                    // 回写到任务状态中
                    TaskMessageUtil.getInstance().setTask(task.getTaskId(), ctask);
                    return;
                }
            }
        };
        // 提交线程池执行
        executor.submit(execTask);
    }

    /**
     * 修改dpi发送状态
     *     此处status来源ack
     * @param euSendStatus
     */
    private void modifyEuStatus(EuSendStatus euSendStatus){
        switch (euSendStatus.getStatus()){
            case 0 : {
                Map<String,String> zfDeviceMaps = baseRedisService.getHashs("ZFGeneralInformation");
                Set<String> keySet = zfDeviceMaps.keySet();
                for (String key : keySet){
                    String zfDpiRelationHashKey = String.format("ZF_DPI_Relation_%s",key);
                    Map<String,String> zfDpiRelationMap = baseRedisService.getHashs(zfDpiRelationHashKey);
                    // 遍历map
                    Set<String> dpiIpAndPorts = zfDpiRelationMap.keySet();
                    for (String dpiIpAndPort:dpiIpAndPorts){
                        String ip = dpiIpAndPort.split("\\|")[0];
                        if (ip.equals(euSendStatus.getIp())){
                            String value = baseRedisService.getHash(zfDpiRelationHashKey,dpiIpAndPort);
                            euSendStatus.setStatus("1".equals(value) ? 2 : 0 );
                            return;
                        }
                    }
                }

            }
            break;
            case 1 : break;
        }
    }

    public static void main(String[] args) {
        String s = "192.168.0.1|500".split("\\|")[0];
        System.out.println("192.168.0.1".equals(s));
    }

}
