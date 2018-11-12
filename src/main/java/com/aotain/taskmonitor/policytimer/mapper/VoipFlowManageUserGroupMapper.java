package com.aotain.taskmonitor.policytimer.mapper;

import com.aotain.common.config.annotation.MyBatisDao;
import com.aotain.common.policyapi.model.VoipFlowManageUserGroup;
import com.github.abel533.mapper.Mapper;

import java.util.List;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/03/16
 */
@MyBatisDao
public interface VoipFlowManageUserGroupMapper extends Mapper<VoipFlowManageUserGroup> {
    /**
     * 根据voipflowId删除记录
     * @param voipflowId
     * @return
     */
    int deleteByVoipFlowId(int voipflowId);

    /**
     * 根据voipflowId获取绑定的用户组信息
     * @param voipflowId
     * @return
     */
    List<VoipFlowManageUserGroup> getBindUser(int voipflowId);
}
