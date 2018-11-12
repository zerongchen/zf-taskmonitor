package com.aotain.taskmonitor.dpidevicestatus.mapper;

import com.aotain.common.config.annotation.MyBatisDao;
import com.aotain.taskmonitor.dpidevicestatus.model.DpiStaticPort;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/01
 */
@MyBatisDao
public interface DpiStaticPortMapper {
    /**
     * 保存DPI设备(静态)port信息
     * @param dpiStaticPort
     * @return
     */
    int saveDpiStaticPort(DpiStaticPort dpiStaticPort);

    /**
     * 根据deploySiteName删除已存在的信息
     * @param deploySiteName
     * @return
     */
    int deleteDpiStaticPortByDeploySiteName(String deploySiteName);
}
