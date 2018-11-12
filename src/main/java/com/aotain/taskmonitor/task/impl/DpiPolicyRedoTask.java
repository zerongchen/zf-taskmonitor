package com.aotain.taskmonitor.task.impl;

import com.alibaba.fastjson.JSON;
import com.aotain.common.config.ContextUtil;
import com.aotain.common.config.redis.BaseRedisService;
import com.aotain.common.policyapi.constant.ProbeType;
import com.aotain.common.policyapi.model.base.BaseVO;
import com.aotain.common.policyapi.model.msg.ServerStatus;
import com.aotain.common.utils.kafka.NoticeQueueUtils;
import com.aotain.common.utils.model.msg.*;
import com.aotain.common.utils.redis.TaskMessageUtil;
import com.aotain.common.utils.tools.CommonConstant;
import com.aotain.common.utils.tools.MonitorStatisticsUtils;
import com.aotain.taskmonitor.task.IRedoTask;
import com.aotain.taskmonitor.utils.PolicyRedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DPI通用设备策略下发重做任务
 *
 * @author daiyh@aotain.com
 * @date 2018/02/07
 */
public class DpiPolicyRedoTask implements IRedoTask{
    private Logger logger = LoggerFactory.getLogger(DpiPolicyRedoTask.class);

    private BaseRedisService<String,String,String> baseRedisService =
            ContextUtil.getContext().getBean("baseRedisServiceImpl",BaseRedisService.class);

    @Override
    public boolean redoTask(RedisTaskStatus taskStatus) {
        try {
            // 需要重发的机房
            List<String> euipList = new ArrayList<String>();
            int ackCount = 0;

            // 基本参数准备
            String policyJsonStr = taskStatus.getContent();
            BaseVO policyObj = BaseVO.parseFromJson(policyJsonStr, BaseVO.class);
            int probeType = policyObj.getProbeType();
            int messageType = policyObj.getMessageType();
            Long messageNo = policyObj.getMessageNo();

            try {
                // 查询ack,并找出需要重发的机房
                Map<String, Object> acks = PolicyRedisUtils.queryAcks(ProbeType.DPI.getValue(), messageType, messageNo);
                for (String euIp : acks.keySet()) {
                    String value = (String) acks.get(euIp);
                    RedisPolicyAck ack = RedisPolicyAck.parseFrom(value, RedisPolicyAck.class);

                    if (ack.getStatus() == 1){
                        ++ackCount;
                    }

                    // 失败时，检查eu服务器状态,将正常euip加入列表
                    if (ack.getStatus() == 0 ) {
                        // status:0-初始化、1-收到ACK
//                        ServerStatus euStatus = PolicyRedisUtils.queryDPIStatus(ProbeType.DPI.getValue(), euIp);
                        Map<String,String> zfDeviceMaps = baseRedisService.getHashs("ZFGeneralInformation");
                        Set<String> keySet = zfDeviceMaps.keySet();
                        for (String key : keySet){
                            String zfDpiRelationHashKey = String.format("ZF_DPI_Relation_%s",key);
                            Map<String,String> zfDpiRelationMap = baseRedisService.getHashs(zfDpiRelationHashKey);
                            // 遍历map
                            Set<String> dpiIpAndPorts = zfDpiRelationMap.keySet();
                            for (String dpiIpAndPort:dpiIpAndPorts){
                                if (dpiIpAndPort.contains(euIp)){
                                    String status = baseRedisService.getHash(zfDpiRelationHashKey,dpiIpAndPort);
                                    if (status.equals("0")){
                                        euipList.add(euIp);
                                    }

                                }
                            }
                        }
//                        if (euStatus == null || euStatus.getStatus() == null) {
//                            logger.error("DPIPolicyRedoTask found empty or unknown DPI server status information: dpiIp="
//                                    + euIp + ",dpiStatus=" + euStatus);
//                        } else if (euStatus.getStatus() == 0) {
//                            // status : 0- 正常；1 - 异常
//                            // 将正常的eu ip写入重发列表中
//                            euipList.add(euIp);
//                        }
                    }

                    try {
                        // 构造EU状态信息
                        EuSendStatus status = new EuSendStatus();
                        status.setIp(euIp);
                        status.setMessageNo(messageNo.intValue());
                        status.setMessageType(messageType);
                        status.setPolicyIp(ack.getPolicyIp());
                        status.setProbeType(probeType);
                        status.setStatus(ack.getStatus());
                        // 构造消息通知对象
                        NoticeQueueMessage message = new NoticeQueueMessage();
                        message.setType(CommonConstant.NOTICE_MESSAGE_TYPE_DPI_POLICY_STATUS);
                        message.setCreateTime(System.currentTimeMillis() / 1000);
                        // 设置通知消息内容
                        message.setMessage(JSON.toJSONString(status));
                        NoticeQueueUtils.sendMessage(message);
                    } catch (Exception e) {
                        // 状态更新失败时，重做的流程不能中断
                        logger.error("DPIPolicyRedoTask update DPI status exception", e);
                        MonitorStatisticsUtils.addEvent(e);
                    }
                }
            } catch (Exception e) {
                logger.error("DPIPolicyRedoTask try to query ack fail dpi ip exception", e);
                MonitorStatisticsUtils.addEvent( e);
                // return false时，主线程会将任务状态改为失败
                return false;
            }

            // 无重发机房
            if (euipList.size() == 0) {
                // 且没有机房连接异常
                if (ackCount>0){
                    // 修改任务状态成功
                    setTaskStatus(taskStatus, true);
                } else {
                    setTaskStatus(taskStatus, false);
                }
                return true;

            } else {
                // 设置为失败
                setTaskStatus(taskStatus, false);
                // 构造策略对象
                StrategySendChannel channel = new StrategySendChannel();
                channel.setIp(euipList);
                channel.setProbeType(policyObj.getProbeType());
                channel.setMessageType(policyObj.getMessageType());
                channel.setMessageContent(policyJsonStr);

                // 重发策略
                try {
                    PolicyRedisUtils.publishPolicyToChannel(channel);
                    return true;
                } catch (Exception e) {
                    logger.error("EuPolicyRedoTask try to publish policy to channel exception", e);
                    MonitorStatisticsUtils.addEvent( e);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("EuPolicyRedoTask  was broken by unexpected exception", e);
            MonitorStatisticsUtils.addEvent( e);
            return false;
        }
    }

    private void setTaskStatus(RedisTaskStatus task, boolean success) {
        try {
            boolean flag = TaskMessageUtil.getInstance().setTaskStatus(task.getTaskId(), task.getTopTaskId(),
                    task.getTaskType(),
                    success ? CommonConstant.REDIS_TASK_STATUS_SUCCESS : CommonConstant.REDIS_TASK_STATUS_FAIL,
                    task.objectToJson());

            if(!flag) {
                logger.error("EuPolicyRedoTask  try to update task stutas to "+(success?"sucess":"fail")+" fail");
            }
        } catch (Exception e) {
            logger.error("EuPolicyRedoTask  try to update task stutas to success fail", e);
        }
    }
}
