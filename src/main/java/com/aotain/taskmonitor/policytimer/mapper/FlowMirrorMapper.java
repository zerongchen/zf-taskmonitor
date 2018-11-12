package com.aotain.taskmonitor.policytimer.mapper;

import com.aotain.common.config.annotation.MyBatisDao;
import com.aotain.common.policyapi.model.FlowMirrorStrategy;

import java.util.List;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/04/10
 */
@MyBatisDao
public interface FlowMirrorMapper {
    /**
     * 获取所有在有效期的voip策略
     * @param rStartTime
     * @return
     */
    List<FlowMirrorStrategy> listInValidPeriodData(Long rStartTime);

    /**
     * 修改记录
     * @param flowMirrorStrategy
     * @return
     */
    int updateSelective(FlowMirrorStrategy flowMirrorStrategy);
}
