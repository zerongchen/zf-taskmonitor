package com.aotain.taskmonitor.scheduledtask;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.aotain.common.config.redis.BaseRedisService;
import com.aotain.common.policyapi.constant.EuDeviceConstant;
import com.aotain.common.utils.constant.RedisKeyConstant;
import com.aotain.common.utils.file.MonitorContentToFileUtil;
import com.aotain.common.utils.monitorstatistics.ExceptionCollector;
import com.aotain.taskmonitor.Main;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * DPI服务器状态监控类
 *
 * @author daiyh@aotain.com
 * @date 2018/02/06
 */
@Component
public class DpiServerStatusTask {

    private static final Logger logger = LoggerFactory.getLogger(DpiServerStatusTask.class);

    @Autowired
    private BaseRedisService<String, String, String> redisCluster;

    public static String SERVER_STATUS_KEY = "serverstatus_server_%s";

    /**
     * DPI模块心跳策略key
     */
    private static final String dpiStrategyKey = "Strategy_0_193";

    /**
     * 时间间隔 5min
     */
    private static final int timeInterval = 6 * 60;

    @Scheduled(cron = "0 0/1 * * * ?")
    public void updateDPIServerStatus(){
        if(Main.LSELECTOR == null || !Main.LSELECTOR.getLeader()){
            logger.debug("it is not the leader node...");
            return;
        }
        updateServerStatus();
    }

    private void updateServerStatus(){

        String strategyKey = dpiStrategyKey;
        String serverKey = RedisKeyConstant.REDIS_KEY_DPI_SERVER_STATUS;
        try{
            Map<String,String> hashes = redisCluster.getHashs(serverKey);
            // 遍历策略hash
            for( Map.Entry<String,String> entry:hashes.entrySet() ){
                String ip = entry.getKey();
                String mapStr = entry.getValue();

                if ( !StringUtils.isEmpty(mapStr) ) {
                    Map<String,Long> serverInfo = JSON.parseObject(mapStr,new TypeReference<Map<String, Long>>(){});

                    long status = serverInfo.get("status");
                    long serverTimestamp = serverInfo.get("lastUpdateTime");

                    String strategyTimestamp = redisCluster.getHash(strategyKey,ip);
                    if (!StringUtils.isEmpty(strategyTimestamp)){
                        //更新时间
                        serverInfo.put("lastUpdateTime",Long.valueOf(strategyTimestamp));
                        serverTimestamp = Long.valueOf(strategyTimestamp);
                    }

                    // 判断状态
                    if (System.currentTimeMillis()/1000-serverTimestamp>timeInterval) {
                        status = 1;
                        serverInfo.put("status", Long.valueOf(EuDeviceConstant.EuDeviceStatus.FATAL.getValue()));
                    } else {
                        status = 0;
                        serverInfo.put("status", Long.valueOf(EuDeviceConstant.EuDeviceStatus.NORMAL.getValue()));
                    }

                    redisCluster.putHash(serverKey,ip,JSON.toJSONString(serverInfo));
                    // 将信息写入到redis中 并定时保存到文件中
                    String field = String.format(SERVER_STATUS_KEY,ip)+"_dpi";
                    redisCluster.putHash(ExceptionCollector.getExceptionKeyByLocalHost(),field,status+"");

                }
            }
        } catch (Exception e){
            logger.error("update server status error...",e);
        }

    }

    @Scheduled(cron = "0 0/2 * * * ?")
    public void writeServerStatusFile(){
        if(Main.LSELECTOR == null || !Main.LSELECTOR.getLeader()){
            logger.debug("it is not the leader node...");
            return;
        }
        logger.info("generate server status begin...");
        Map<String,String> serverStatusMap = redisCluster.getHashs(ExceptionCollector.getExceptionKeyByLocalHost());
        MonitorContentToFileUtil.writeMessageToFile("serverstatus",serverStatusMap);
        redisCluster.remove(ExceptionCollector.getExceptionKeyByLocalHost());
        logger.info("generate server status end...");
    }
}
