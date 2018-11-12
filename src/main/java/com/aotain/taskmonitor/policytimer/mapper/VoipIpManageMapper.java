package com.aotain.taskmonitor.policytimer.mapper;

import com.aotain.common.config.annotation.MyBatisDao;
import com.aotain.common.config.pagehelper.Page;
import com.aotain.common.policyapi.model.VoipFlowStrategy;

import java.util.List;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/03/16
 */
@MyBatisDao
public interface VoipIpManageMapper {
    /**
     * save
     * @param voipFlowStrategy
     * @return
     */
    int insertSelective(VoipFlowStrategy voipFlowStrategy);

    /**
     * 查询分页数据
     * @param page
     */
    List<VoipFlowStrategy> listData(Page<VoipFlowStrategy> page);

    /**
     * 根据主键查询数据
     * @param voipFlowStrategy
     * @return
     */
    VoipFlowStrategy selectByPrimaryKey(VoipFlowStrategy voipFlowStrategy);

    /**
     * 修改记录
     * @param voipFlowStrategy
     * @return
     */
    int updateByPrimaryKey(VoipFlowStrategy voipFlowStrategy);

    /**
     * 根据主键删除数据（逻辑删除）
     * @param voipFlowStrategy
     * @return
     */
    int deleteData(VoipFlowStrategy voipFlowStrategy);

    /**
     * 是否存在名称相同的记录
     * @param voipFlowStrategy
     * @return
     */
    int existSameNameRecord(VoipFlowStrategy voipFlowStrategy);

    /**
     * 获取所有在有效期的voip策略
     * @param rStartTime
     * @return
     */
    List<VoipFlowStrategy> listInValidPeriodData(Long rStartTime);
}
