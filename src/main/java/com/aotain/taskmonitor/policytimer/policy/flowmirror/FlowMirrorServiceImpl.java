package com.aotain.taskmonitor.policytimer.policy.flowmirror;

import com.aotain.common.policyapi.base.BaseService;
import com.aotain.common.policyapi.constant.BindAction;
import com.aotain.common.policyapi.constant.MessageType;
import com.aotain.common.policyapi.constant.OperationConstants;
import com.aotain.common.policyapi.model.FlowMirrorStrategy;
import com.aotain.common.policyapi.model.UserPolicyBindStrategy;
import com.aotain.common.policyapi.model.VoipFlowStrategy;
import com.aotain.common.policyapi.model.base.BaseVO;
import com.aotain.common.policyapi.model.msg.BindMessage;
import com.aotain.taskmonitor.policytimer.log.LogConstant;
import com.aotain.taskmonitor.policytimer.log.OperationLog;
import com.aotain.taskmonitor.policytimer.log.OperationLogMapper;
import com.aotain.taskmonitor.policytimer.mapper.FlowMirrorMapper;
import com.aotain.taskmonitor.policytimer.mapper.UserPolicyBindMapper;
import com.aotain.taskmonitor.policytimer.policy.UserPolicyBindService;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.aotain.common.utils.monitorstatistics.ExceptionCollector.getLocalHost;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/04/10
 */
@Service
public class FlowMirrorServiceImpl extends BaseService implements IFlowMirrorService{

    private static final Logger logger = LoggerFactory.getLogger(FlowMirrorServiceImpl.class);

    @Autowired
    private FlowMirrorMapper flowMirrorMapper;

    @Autowired
    private UserPolicyBindMapper userPolicyBindMapper;

    @Autowired
    private UserPolicyBindService userPolicyBindService;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Override
    protected boolean addDb(BaseVO policy) {
        return false;
    }

    @Override
    protected boolean deleteDb(BaseVO policy) {
        return false;
    }

    @Override
    protected boolean modifyDb(BaseVO policy) {
        return false;
    }

    @Override
    protected boolean addCustomLogic(BaseVO policy) {
        return sendRedisMessage(policy);
    }

    @Override
    protected boolean modifyCustomLogic(BaseVO policy) {
        return sendRedisMessage(policy);
    }

    @Override
    protected boolean deleteCustomLogic(BaseVO policy) {
        return setPolicyOperateSequenceToRedis(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void timerPolicy() {
        // 获取所有在有效期的voip策略
        List<FlowMirrorStrategy> flowMirrorStrategies = flowMirrorMapper.listInValidPeriodData(getTodayTimeStamp());
        for (int i=0;i<flowMirrorStrategies.size();i++){
            FlowMirrorStrategy flowMirrorStrategy = flowMirrorStrategies.get(i);

            // fill the content
            flowMirrorStrategy.setMessageType(MessageType.APP_FLOW_MIRROR.getId());
            flowMirrorStrategy.setProbeType(0);

            // 增加到redis hash
            setRedisPolicyHash(flowMirrorStrategy);
            addCustomLogic(flowMirrorStrategy);

//            try{
//                OperationLog operationLog = createOperationLogByStrategy(flowMirrorStrategy);
//                operationLogMapper.insertSelective(operationLog);
//            } catch (Exception e){
//                e.printStackTrace();
//                logger.info(" add operationLog failed...");
//            }

            // 删除原有的绑定策略
            Map<String,Object> queryCondition = generateQueryConditionMap(flowMirrorStrategy);
            List<UserPolicyBindStrategy> existUserPolicyBindStrategyList = userPolicyBindMapper.getByBindMessageNoAndType(queryCondition);
            deleteExistBindPolicy(existUserPolicyBindStrategyList,flowMirrorStrategy);

            // 发送相关的用户绑定策略
            List<UserPolicyBindStrategy> userPolicyBindStrategies = flowMirrorStrategy.getBindUser();
            for(int j=0;j<userPolicyBindStrategies.size();j++){
                UserPolicyBindStrategy userPolicyBindStrategy = userPolicyBindStrategies.get(j);

                List<BindMessage> bindMessages = new ArrayList<>();
                BindMessage bindMessage = new BindMessage();
                bindMessage.setBindMessageNo(flowMirrorStrategy.getMessageNo());
                bindMessage.setBindMessageType(flowMirrorStrategy.getMessageType());
                bindMessages.add(bindMessage);

                userPolicyBindStrategy.setUserBindMessageNo(flowMirrorStrategy.getMessageNo());
                userPolicyBindStrategy.setUserBindMessageType(flowMirrorStrategy.getMessageType());

                userPolicyBindStrategy.setBindAction(BindAction.BIND.getValue());
                if (userPolicyBindStrategies.get(j).getUserName()==null){
                    userPolicyBindStrategy.setUserName("");
                }

                userPolicyBindStrategy.setBindInfo(bindMessages);
                userPolicyBindService.addPolicy(userPolicyBindStrategy);

            }
        }
    }

    /**
     * 生成对应绑定策略查询条件
     * @param flowMirrorStrategy
     * @return
     */
    private Map<String,Object> generateQueryConditionMap(FlowMirrorStrategy flowMirrorStrategy){
        Map<String,Object> queryCondition = Maps.newHashMap();
        queryCondition.put("messageNo",flowMirrorStrategy.getMessageNo());
        queryCondition.put("userBindMessageType",flowMirrorStrategy.getMessageType());
        queryCondition.put("operateType", OperationConstants.OPERATION_DELETE);
        return queryCondition;
    }

    /**
     * 删除原有的用户组绑定策略
     * @param userPolicyBindStrategyList
     * @param flowMirrorStrategy
     */
    private void deleteExistBindPolicy(List<UserPolicyBindStrategy> userPolicyBindStrategyList,FlowMirrorStrategy flowMirrorStrategy){
        flowMirrorStrategy.setMessageType(MessageType.APP_FLOW_MIRROR.getId());
        for (UserPolicyBindStrategy userPolicyBindStrategy : userPolicyBindStrategyList) {
            List<BindMessage> bindMessages = new ArrayList<>();
            BindMessage bindMessage = new BindMessage();
            bindMessage.setBindMessageNo(flowMirrorStrategy.getMessageNo());
            bindMessage.setBindMessageType(flowMirrorStrategy.getMessageType());
            bindMessages.add(bindMessage);
            userPolicyBindStrategy.setBindInfo(bindMessages);
            userPolicyBindService.deletePolicy(userPolicyBindStrategy);
        }
    }

    /**
     * 获取当前日期的时间戳 精确到s
     * @return
     */
    private static Long getTodayTimeStamp(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        //设置日期格式
        String date = simpleDateFormat.format(new Date());
        System.out.println(date);
        long timestamp = 0;
        try {
            timestamp = simpleDateFormat.parse(date).getTime()/1000;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return timestamp;
    }

    private OperationLog createOperationLogByStrategy(FlowMirrorStrategy flowMirrorStrategy){
        OperationLog operationLog = new OperationLog();
        if ( flowMirrorStrategy.getModifyOper() != null ){
            operationLog.setOperUser(flowMirrorStrategy.getModifyOper());
        } else {
            operationLog.setOperUser("admin");
        }
        // 定时发送时修改操作时间为当前
        operationLog.setOperTime(new Date());
        // 定时重发
        operationLog.setOperType(LogConstant.SCHEDULED_OPERATION_TYPE);
        operationLog.setOperModel(102010);

        operationLog.setClientIp(getLocalHost());
        operationLog.setClientPort(LogConstant.SERVER_PORT);
        operationLog.setServerName(getLocalHost());
        operationLog.setDataJson("messageNo="+flowMirrorStrategy.getMessageNo());
        return operationLog;
    }

}
