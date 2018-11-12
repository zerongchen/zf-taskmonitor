package com.aotain.taskmonitor.dpidevicestatus.mapper;

import com.aotain.common.config.annotation.MyBatisDao;
import com.aotain.taskmonitor.dpidevicestatus.model.DpiStatic;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/01
 */
@MyBatisDao
public interface DpiStaticMapper {

    /**
     * 保存DPI设备(静态)信息
     * @param dpiStatic
     * @return
     */
    int saveDpiStatic(DpiStatic dpiStatic);

    /**
     * 根据deploySiteName删除已存在的信息
     * @param deploySiteName
     * @return
     */
    int deleteDpiStaticByDeploySiteName(String deploySiteName);
}
