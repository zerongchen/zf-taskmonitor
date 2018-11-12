package com.aotain.taskmonitor;

import com.aotain.taskmonitor.dpidevicestatus.mapper.DpiDynamicCpuMapper;
import com.aotain.taskmonitor.dpidevicestatus.mapper.DpiDynamicPortMapper;
import com.aotain.taskmonitor.dpidevicestatus.mapper.DpiStaticMapper;
import com.aotain.taskmonitor.dpidevicestatus.mapper.DpiStaticPortMapper;
import com.aotain.taskmonitor.dpidevicestatus.model.DpiDynamicCpu;
import com.aotain.taskmonitor.dpidevicestatus.model.DpiDynamicPort;
import com.aotain.taskmonitor.dpidevicestatus.model.DpiStatic;
import com.aotain.taskmonitor.dpidevicestatus.model.DpiStaticPort;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/01
 */
public class MapperTest extends BaseTest{

    @Autowired
    private DpiDynamicCpuMapper dpiDynamicCpuMapper;

    @Autowired
    private DpiDynamicPortMapper dpiDynamicPortMapper;

    @Autowired
    private DpiStaticMapper dpiStaticMapper;

    @Autowired
    private DpiStaticPortMapper dpiStaticPortMapper;

    @Test
    public void testSaveDpiDynamicCpu(){
        DpiDynamicCpu dpiDynamicCpu = new DpiDynamicCpu();
        dpiDynamicCpu.setCpuNo(50);
        dpiDynamicCpu.setCpuUsage(10);
        dpiDynamicCpu.setCreateTime(new Date());
        dpiDynamicCpuMapper.saveDpiDynamicCpu(dpiDynamicCpu);
    }

    @Test
    public void testSaveDpiDynamicPort(){
        DpiDynamicPort dpiDynamicPort = new DpiDynamicPort();
        dpiDynamicPort.setPortNo(10);
        dpiDynamicPort.setPortInfo("aaa");
        dpiDynamicPort.setPortUsage(50);
        dpiDynamicPort.setCreateTime(new Date());
        dpiDynamicPortMapper.saveDpiDynamicPort(dpiDynamicPort);
    }

    @Test
    public void testSaveDpiStatic(){
        DpiStatic dpiStatic = new DpiStatic();
        dpiStatic.setDeploySiteName("DPI-AOTAIN-TEST");
        dpiStatic.setAnalysisSlotNum(1);
//        dpiStatic.setCreateTime(new Date());
        dpiStatic.setGPSlotNum(1);
//        dpiStatic.setHouseId("bang");
        dpiStatic.setPreProcSlotNum(1);
        dpiStatic.setProbeType(0);
        dpiStatic.setSlotNum(1);
        dpiStatic.setSoftwareVersion(1);
        dpiStatic.setTotalCapability(50);
        dpiStatic.setDeployMode(1);
//        dpiStaticMapper.saveDpiStatic(dpiStatic);
    }

    @Test
    public void testSaveDpiStaticPort(){
        DpiStaticPort dpiStaticPort = new DpiStaticPort();
        dpiStaticPort.setDeploySiteName("DPI-AOTAIN-TEST");
        dpiStaticPort.setMLinkDesc("ba");
        dpiStaticPort.setMLinkId(1);
        dpiStaticPort.setPortDescription("bag");
        dpiStaticPort.setPortNo(1);
        dpiStaticPort.setPortType(1);
        dpiStaticPort.setCreateTime(new Date());
        dpiStaticPortMapper.saveDpiStaticPort(dpiStaticPort);
    }

    @Test
    public void delete(){
        dpiStaticPortMapper.deleteDpiStaticPortByDeploySiteName("DPI-AOTAIN-TEST");
    }

    @Test
    public void delete2(){
        dpiStaticMapper.deleteDpiStaticByDeploySiteName("DPI-AOTAIN-TEST");
    }
}
