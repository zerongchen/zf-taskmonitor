package com.aotain.taskmonitor.task.impl;

import com.aotain.common.utils.kafka.JobQueueUtil;
import com.aotain.common.utils.model.msg.JobQueue;
import com.aotain.common.utils.model.msg.RedisTaskStatus;
import com.aotain.common.utils.tools.CommonConstant;
import com.aotain.common.utils.tools.MonitorStatisticsUtils;
import com.aotain.taskmonitor.task.IRedoTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/05
 */
public class JobRedoTask implements IRedoTask{

    private Logger logger = LoggerFactory.getLogger(JobRedoTask.class);

    @Override
    public boolean redoTask(RedisTaskStatus taskStatus) {
        try {
            String jobTaskJsonStr = taskStatus.getContent();
            JobQueue jobQueue = JobQueue.parseFrom(jobTaskJsonStr, JobQueue.class);
            jobQueue.setIsretry(CommonConstant.JOB_QUEUE_ISRETRY_TRUE); // 重试标志
            JobQueueUtil.sendMsgToKafkaJobQueue(jobQueue);
            return true;
        } catch (Exception e) { // 抛出异常时，没用成功创建重试任务
            logger.error("JobRedoTask try to write job queue exception", e);
            MonitorStatisticsUtils.addEvent( e);
            return false;
        }
    }
}
