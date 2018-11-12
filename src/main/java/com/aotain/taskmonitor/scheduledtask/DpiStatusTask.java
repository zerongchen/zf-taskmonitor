package com.aotain.taskmonitor.scheduledtask;

import com.alibaba.fastjson.JSON;
import com.aotain.common.config.redis.BaseRedisService;
import com.aotain.common.policyapi.constant.ProbeType;
import com.aotain.taskmonitor.Main;
import com.aotain.taskmonitor.dpidevicestatus.model.*;
import com.aotain.taskmonitor.dpidevicestatus.service.IDpiDeviceStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 定时将dpi静态设备信息和dpi动态设备信息写入数据库
 *
 * @author daiyh@aotain.com
 * @date 2018/02/01
 */
@Component
public class DpiStatusTask {

    private static Logger logger = LoggerFactory.getLogger(DpiStatusTask.class);

    @Autowired
    private BaseRedisService<String,String,String> baseRedisService;

    @Autowired
    private IDpiDeviceStatusService dpiDeviceStatusService;

    private static String DPI_STATUS_STATIC_REDIS = "Strategy_0_197";

    private static String DPI_STATUS_DYNAMIC_REDIS = "Strategy_0_198";


    /**
     * 将redis中EU静态信息写入db
     */
    @Transactional(rollbackFor = Exception.class)
    @Scheduled(cron = "0 0/2 * * * ?")
    public void putStaticDpiInfoToDB(){
        if(Main.LSELECTOR == null || !Main.LSELECTOR.getLeader()){
            logger.debug("it is not the leader node...");
            return;
        }
        try{
            // 判断redis list中是否还存在记录
            long length = baseRedisService.listSize(DPI_STATUS_STATIC_REDIS);

            while ( length > 0 ){
                String json = baseRedisService.rightPop(DPI_STATUS_STATIC_REDIS);
                if (StringUtils.isEmpty(json)){
                    break;
                }

                DpiStatic dpiStatic = JSON.parseObject(json,DpiStatic.class);

                if (dpiStatic.getProbeType()==null) {
                    dpiStatic.setProbeType(ProbeType.DPI.getValue());
                }

                // 删除原有数据
                dpiDeviceStatusService.deleteDpiStatic(dpiStatic.getDeploySiteName());
                dpiDeviceStatusService.deleteDpiStaticPort(dpiStatic.getDeploySiteName());

                List<DpiStaticPortJsonBean> dpiStaticPortJsonBeanList = dpiStatic.getPortsType();

                dpiDeviceStatusService.saveDpiStatic(dpiStatic);
                if (dpiStaticPortJsonBeanList!=null){
                    for(DpiStaticPortJsonBean dpiStaticPortJsonBean:dpiStaticPortJsonBeanList){
                        int portType = dpiStaticPortJsonBean.getPortType();
                        List<DpiStaticPort> dpiStaticPortList = dpiStaticPortJsonBean.getPorts();
                        for(DpiStaticPort dpiStaticPort:dpiStaticPortList){
                            dpiStaticPort.setPortType(portType);
                            dpiStaticPort.setDeploySiteName(dpiStatic.getDeploySiteName());
                            dpiStaticPort.setCreateTime(dpiStatic.getCreateTime());
                            dpiDeviceStatusService.saveDpiStaticPort(dpiStaticPort);
                        }
                    }
                }

                length = baseRedisService.listSize(DPI_STATUS_STATIC_REDIS);
            }


        } catch (Exception e) {
            logger.error("putStaticInfoToDB failed...",e);

        }


    }

    /**
     * 将redis中EU动态信息写入db
     */
    @Transactional(rollbackFor = Exception.class)
    @Scheduled(cron = "0 1/2 * * * ?")
    public void putDynamicDpiInfoToDB(){
        if(Main.LSELECTOR == null || !Main.LSELECTOR.getLeader()){
            logger.debug("it is not the leader node...");
            return;
        }
        try{
            // 判断redis list中是否还存在记录
            long length = baseRedisService.listSize(DPI_STATUS_DYNAMIC_REDIS);
            while ( length > 0 ) {

                String json = baseRedisService.rightPop(DPI_STATUS_DYNAMIC_REDIS);
                if (StringUtils.isEmpty(json)) {
                    break;
                }

                DpiDynamicJsonBean dpiDynamicJsonBean = JSON.parseObject(json, DpiDynamicJsonBean.class);

                List<DpiDynamicCpu> deviceCpuDynamicList = dpiDynamicJsonBean.getTotalCPU();
                if (deviceCpuDynamicList!=null){
                    for (DpiDynamicCpu dpiDynamicCpu : deviceCpuDynamicList) {
                        dpiDynamicCpu.setCreateTime(dpiDynamicJsonBean.getCreateTime());
                        dpiDeviceStatusService.saveDpiDynamicCpu(dpiDynamicCpu);
                    }
                }


                List<DpiDynamicPort> dpiDynamicPorts = dpiDynamicJsonBean.getTotalPorts();
                if (dpiDynamicPorts!=null){
                    for (DpiDynamicPort dpiDynamicPort : dpiDynamicPorts) {
                        dpiDynamicPort.setCreateTime(dpiDynamicJsonBean.getCreateTime());
                        dpiDeviceStatusService.saveDpiDynamicPort(dpiDynamicPort);
                    }
                }

                length = baseRedisService.listSize(DPI_STATUS_DYNAMIC_REDIS);
            }
        } catch (Exception e) {
            logger.error("putDynamicInfoToDB failed...",e);

        }
    }
}
