package com.aotain.taskmonitor.task.impl;

import com.aotain.common.utils.kafka.AckQueueUtil;
import com.aotain.common.utils.model.msg.RedisTaskStatus;
import com.aotain.common.utils.tools.MonitorStatisticsUtils;
import com.aotain.taskmonitor.task.IRedoTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/07
 */
public class SmmsAckRedoTask implements IRedoTask{

    private Logger logger = LoggerFactory.getLogger(SmmsAckRedoTask.class);

    @Override
    public boolean redoTask(RedisTaskStatus taskStatus) {
        try {
            String ackTaskJsonStr = taskStatus.getContent();
            AckQueueUtil.sendMsgToKafkaAckQueue(ackTaskJsonStr);
            return true;
        } catch (Exception e) { // 抛出异常时，没用成功创建重试任务
            logger.error("SmmsAckRedoTask try to write ack queue exception", e);
            MonitorStatisticsUtils.addEvent(e);
            return false;
        }
    }
}
