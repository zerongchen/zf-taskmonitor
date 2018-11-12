package com.aotain.taskmonitor.dpidevicestatus.service;

import com.aotain.taskmonitor.dpidevicestatus.model.DpiDynamicCpu;
import com.aotain.taskmonitor.dpidevicestatus.model.DpiDynamicPort;
import com.aotain.taskmonitor.dpidevicestatus.model.DpiStatic;
import com.aotain.taskmonitor.dpidevicestatus.model.DpiStaticPort;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/01
 */
public interface IDpiDeviceStatusService {

    boolean saveDpiDynamicCpu(DpiDynamicCpu dpiDynamicCpu);

    boolean saveDpiDynamicPort(DpiDynamicPort dpiDynamicPort);

    boolean saveDpiStatic(DpiStatic dpiStatic);

    void deleteDpiStatic(String deploySiteName);

    boolean saveDpiStaticPort(DpiStaticPort dpiStaticPort);

    void deleteDpiStaticPort(String deploySiteName);
}
