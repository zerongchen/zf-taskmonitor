package com.aotain.taskmonitor.dpidevicestatus.mapper;

import com.aotain.common.config.annotation.MyBatisDao;
import com.aotain.taskmonitor.dpidevicestatus.model.DpiDynamicCpu;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/01
 */
@MyBatisDao
public interface DpiDynamicCpuMapper {

    /**
     * 保存DPI设备(动态)CPU信息
     * @param dpiDynamicCpu
     * @return
     */
    int saveDpiDynamicCpu(DpiDynamicCpu dpiDynamicCpu);
}
