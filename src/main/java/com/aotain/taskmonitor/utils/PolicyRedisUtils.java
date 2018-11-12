package com.aotain.taskmonitor.utils;

import com.alibaba.fastjson.JSON;
import com.aotain.common.config.ContextUtil;
import com.aotain.common.config.redis.BaseRedisService;
import com.aotain.common.policyapi.model.msg.ServerStatus;
import com.aotain.common.utils.constant.RedisKeyConstant;
import com.aotain.common.utils.model.msg.StrategySendChannel;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/05
 */
public class PolicyRedisUtils {

    @SuppressWarnings("unchecked")
    private static BaseRedisService<String, String, Object> rediscluster = ContextUtil.getContext()
            .getBean("baseRedisServiceImpl", BaseRedisService.class);


    /**
     * 策略发布接口
     *
     * @throws Exception
     */
    public static void publishPolicyToChannel(StrategySendChannel channelMessage) throws Exception {
        rediscluster.sendMessage(RedisKeyConstant.REDIS_KEY_POLICY_PUBLISH_CHANNEL, channelMessage.objectToJson());
    }

    /**
     * 查询策略的ack信息
     *
     * @throws Exception
     */
    public static Map<String, Object> queryAcks(int probeType, int messageType, Long messageNo) throws Exception {
        String key = String.format(RedisKeyConstant.REDIS_KEY_POLICY_ACK, probeType, messageType, messageNo);
        return rediscluster.getHashs(key);
    }

    /**
     * 删除ACK队列
     *
     * @param probeType
     * @param messageType
     * @param messageNo
     * @throws Exception
     */
    public static void delAcks(int probeType, int messageType, Long messageNo) throws Exception {
        String key = String.format(RedisKeyConstant.REDIS_KEY_POLICY_ACK, probeType, messageType, messageNo);
        rediscluster.remove(key);
    }

    /**
     * 查询EU设备状态
     *
     * @param dpiIp EU设备IP
     * @param probeType
     * @return 0 - 正常；1 - 异常
     * @throws Exception
     */
    public static ServerStatus queryDPIStatus(int probeType, String dpiIp) throws Exception {
        if (StringUtils.isBlank(dpiIp)) {
            return null;
        }
        String key = String.format(RedisKeyConstant.REDIS_KEY_DPI_SERVER_STATUS, probeType);
        String json = (String) rediscluster.getHashValueByHashKey(key, dpiIp);
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return JSON.parseObject(json, ServerStatus.class);
    }
}
