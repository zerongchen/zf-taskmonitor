package com.aotain.taskmonitor.dpidevicestatus.service.impl;

import com.aotain.taskmonitor.dpidevicestatus.mapper.DpiDynamicCpuMapper;
import com.aotain.taskmonitor.dpidevicestatus.mapper.DpiDynamicPortMapper;
import com.aotain.taskmonitor.dpidevicestatus.mapper.DpiStaticMapper;
import com.aotain.taskmonitor.dpidevicestatus.mapper.DpiStaticPortMapper;
import com.aotain.taskmonitor.dpidevicestatus.model.DpiDynamicCpu;
import com.aotain.taskmonitor.dpidevicestatus.model.DpiDynamicPort;
import com.aotain.taskmonitor.dpidevicestatus.model.DpiStatic;
import com.aotain.taskmonitor.dpidevicestatus.model.DpiStaticPort;
import com.aotain.taskmonitor.dpidevicestatus.service.IDpiDeviceStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/01
 */
@Service
public class DpiDeviceStatusServiceImpl implements IDpiDeviceStatusService{

    private static Logger logger = LoggerFactory.getLogger(DpiDeviceStatusServiceImpl.class);

    @Autowired
    private DpiDynamicCpuMapper dpiDynamicCpuMapper;

    @Autowired
    private DpiDynamicPortMapper dpiDynamicPortMapper;

    @Autowired
    private DpiStaticMapper dpiStaticMapper;

    @Autowired
    private DpiStaticPortMapper dpiStaticPortMapper;

    @Override
    public boolean saveDpiDynamicCpu(DpiDynamicCpu dpiDynamicCpu){
        int result = dpiDynamicCpuMapper.saveDpiDynamicCpu(dpiDynamicCpu);
        return result>0;
    }

    @Override
    public boolean saveDpiDynamicPort(DpiDynamicPort dpiDynamicPort){
        int result = dpiDynamicPortMapper.saveDpiDynamicPort(dpiDynamicPort);
        return result>0;
    }

    @Override
    public boolean saveDpiStatic(DpiStatic dpiStatic) {
        int result = dpiStaticMapper.saveDpiStatic(dpiStatic);
        return result>0;
    }

    @Override
    public void deleteDpiStatic(String deploySiteName) {
        dpiStaticMapper.deleteDpiStaticByDeploySiteName(deploySiteName);
    }

    @Override
    public boolean saveDpiStaticPort(DpiStaticPort dpiStaticPort){
        int result = dpiStaticPortMapper.saveDpiStaticPort(dpiStaticPort);
        return result>0;
    }

    @Override
    public void deleteDpiStaticPort(String deploySiteName){
        dpiStaticPortMapper.deleteDpiStaticPortByDeploySiteName(deploySiteName);
    }
}
