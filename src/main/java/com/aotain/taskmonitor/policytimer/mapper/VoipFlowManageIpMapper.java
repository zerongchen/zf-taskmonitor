package com.aotain.taskmonitor.policytimer.mapper;

import com.aotain.common.config.annotation.MyBatisDao;
import com.aotain.common.policyapi.model.msg.VoipFlowManageIp;
import com.github.abel533.mapper.Mapper;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/03/16
 */
@MyBatisDao
public interface VoipFlowManageIpMapper extends Mapper<VoipFlowManageIp> {
    /**
     * 根据voipflowId删除记录
     * @param voipflowId
     * @return
     */
    int deleteByVoipFlowId(int voipflowId);
}
