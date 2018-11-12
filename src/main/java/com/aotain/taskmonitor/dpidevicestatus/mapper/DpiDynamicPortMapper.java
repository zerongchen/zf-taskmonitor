package com.aotain.taskmonitor.dpidevicestatus.mapper;

import com.aotain.common.config.annotation.MyBatisDao;
import com.aotain.taskmonitor.dpidevicestatus.model.DpiDynamicPort;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/01
 */
@MyBatisDao
public interface DpiDynamicPortMapper {

    /**
     * 保存DPI设备(动态)port信息
     * @param dpiDynamicPort
     * @return
     */
    int saveDpiDynamicPort(DpiDynamicPort dpiDynamicPort);
}
