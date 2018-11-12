package com.aotain.taskmonitor.policytimer.policy;

import com.aotain.common.policyapi.base.BaseService;
import com.aotain.common.policyapi.constant.BindAction;
import com.aotain.common.policyapi.constant.MessageTypeConstants;
import com.aotain.common.policyapi.constant.OperationConstants;
import com.aotain.common.policyapi.model.FlowMirrorStrategy;
import com.aotain.common.policyapi.model.UserPolicyBindStrategy;
import com.aotain.common.policyapi.model.VoipFlowManageUserGroup;
import com.aotain.common.policyapi.model.VoipFlowStrategy;
import com.aotain.common.policyapi.model.base.BaseVO;
import com.aotain.common.policyapi.model.msg.BindMessage;
import com.aotain.common.policyapi.model.msg.VoipFlowManageIp;
import com.aotain.common.utils.redis.MessageNoUtil;
import com.aotain.common.utils.redis.MessageSequenceNoUtil;
import com.aotain.taskmonitor.policytimer.log.LogConstant;
import com.aotain.taskmonitor.policytimer.log.OperationLog;
import com.aotain.taskmonitor.policytimer.log.OperationLogMapper;
import com.aotain.taskmonitor.policytimer.mapper.PolicyMapper;
import com.aotain.taskmonitor.policytimer.mapper.UserPolicyBindMapper;
import com.aotain.taskmonitor.policytimer.mapper.VoipFlowManageUserGroupMapper;
import com.aotain.taskmonitor.policytimer.mapper.VoipIpManageMapper;
import com.aotain.taskmonitor.policytimer.model.Policy;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.aotain.common.utils.monitorstatistics.ExceptionCollector.getLocalHost;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/03/16
 */
@Service
public class VoipFlowManageServiceImpl extends BaseService implements IVoipFlowManageService{

    private static final Logger logger = LoggerFactory.getLogger(VoipFlowManageServiceImpl.class);

    /** 网关 */
    private static Integer voipGwType = 0;
    /** 网守 */
    private static Integer voipGwKeeperType = 1;

    @Autowired
    private VoipIpManageMapper voipIpManageMapper;

    @Autowired
    private PolicyMapper policyMapper;

    @Autowired
    private UserPolicyBindService userPolicyBindService;

    @Autowired
    private UserPolicyBindMapper userPolicyBindMapper;

    @Autowired
    private VoipFlowManageUserGroupMapper voipFlowManageUserGroupMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Override
    public void timerPolicy() {
        // 获取所有在有效期的voip策略
        List<VoipFlowStrategy> voipFlowStrategyList = voipIpManageMapper.listInValidPeriodData(getTodayTimeStamp());
        for (int i=0;i<voipFlowStrategyList.size();i++){
            VoipFlowStrategy voipFlowStrategy = voipFlowStrategyList.get(i);

            // fill the content
            voipFlowStrategy.setMessageType(MessageTypeConstants.MESSAGE_TYPE_VOIP_FLOW_POLICY);
            voipFlowStrategy.setProbeType(0);
            setVoipFlowManageIp(voipFlowStrategy);

            Policy policyNo = createPolicyBeanByBaseVo(voipFlowStrategy);
            boolean flag = true;
            if (voipFlowStrategy.getMessageNo()!=null){
                policyNo.setMessageNo(voipFlowStrategy.getMessageNo());
                flag = false;
            } else {
                long messageNo = MessageNoUtil.getInstance().getMessageNo(MessageTypeConstants.MESSAGE_TYPE_VOIP_FLOW_POLICY);
                policyNo.setMessageNo(messageNo);
                voipFlowStrategy.setMessageNo(messageNo);
            }
            if (voipFlowStrategy.getMessageSequenceNo()!=null){
                policyNo.setMessageSequenceno(voipFlowStrategy.getMessageSequenceNo());
            } else {
                long messageSeqNo = MessageSequenceNoUtil.getInstance().getSequenceNo(MessageTypeConstants.MESSAGE_TYPE_VOIP_FLOW_POLICY);
                policyNo.setMessageSequenceno(messageSeqNo);
                voipFlowStrategy.setMessageSequenceNo(messageSeqNo);
            }
            if ( flag ){
                policyMapper.insertSelective(policyNo);
                voipIpManageMapper.updateByPrimaryKey(voipFlowStrategy);
            }

            // 增加到redis hash
            setRedisPolicyHash(voipFlowStrategy);
            // 发送其他redis信息
            if (voipFlowStrategy.getOperationType()==OperationConstants.OPERATION_SAVE){
                addCustomLogic(voipFlowStrategy);

            } else {
                modifyCustomLogic(voipFlowStrategy);
            }

//            try{
//                OperationLog operationLog = createOperationLogByStrategy(voipFlowStrategy);
//                operationLogMapper.insertSelective(operationLog);
//            } catch (Exception e){
//                e.printStackTrace();
//                logger.info(" add operationLog failed...");
//            }


            // 删除原有的绑定策略
            Map<String,Object> queryCondition = generateQueryConditionMap(voipFlowStrategy);
            List<UserPolicyBindStrategy> existUserPolicyBindStrategyList = userPolicyBindMapper.getByBindMessageNoAndType(queryCondition);
            deleteExistBindPolicy(existUserPolicyBindStrategyList,voipFlowStrategy);

            // 发送相关的用户绑定策略
            List<VoipFlowManageUserGroup> voipFlowManageUserGroups = voipFlowManageUserGroupMapper.getBindUser(voipFlowStrategy.getVoipFlowId());
            for(int j=0;j<voipFlowManageUserGroups.size();j++){
                UserPolicyBindStrategy userPolicyBindStrategy = new UserPolicyBindStrategy();

                List<BindMessage> bindMessages = new ArrayList<>();
                BindMessage bindMessage = new BindMessage();
                bindMessage.setBindMessageNo(voipFlowStrategy.getMessageNo());
                bindMessage.setBindMessageType(voipFlowStrategy.getMessageType());
                bindMessages.add(bindMessage);

                userPolicyBindStrategy.setUserBindMessageNo(voipFlowStrategy.getMessageNo());
                userPolicyBindStrategy.setUserBindMessageType(voipFlowStrategy.getMessageType());

                userPolicyBindStrategy.setBindAction(BindAction.BIND.getValue());
                if (voipFlowManageUserGroups.get(j).getUserName()!=null){
                    userPolicyBindStrategy.setUserName(voipFlowManageUserGroups.get(j).getUserName());
                } else {
                    userPolicyBindStrategy.setUserName("");
                }

                userPolicyBindStrategy.setUserGroupId(voipFlowManageUserGroups.get(j).getUserGroupId());

                userPolicyBindStrategy.setUserType(voipFlowManageUserGroups.get(j).getUserType());
                userPolicyBindStrategy.setBindInfo(bindMessages);
                userPolicyBindService.addPolicy(userPolicyBindStrategy);

            }
        }
    }


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

    /**
     * 根据参数生成对应的FlowManageIp实体
     * @param voipFlowStrategy
     */
    private void setVoipFlowManageIp(VoipFlowStrategy voipFlowStrategy){
        List<VoipFlowManageIp> voipGwList = new ArrayList<>();
        List<VoipFlowManageIp> voipGwKeeperList = new ArrayList<>();
        String gwIp = voipFlowStrategy.getGwIp();
        String gwKeeperIp = voipFlowStrategy.getGwKeeperIp();
        //网关集合
        List<String> gwIps = Arrays.asList(gwIp.split(","));
        for (int i=0;i<gwIps.size();i++){
            if ( !StringUtils.isEmpty(gwIps.get(i)) ){
                VoipFlowManageIp voipFlowManageIp = new VoipFlowManageIp();
                voipFlowManageIp.setVoipflowId(voipFlowStrategy.getVoipFlowId());
                voipFlowManageIp.setVoipIp(gwIps.get(i));
                voipFlowManageIp.setVoipType(voipGwType);
                voipGwList.add(voipFlowManageIp);
            }
        }

        //网守集合
        List<String> gwKeeperIps = Arrays.asList(gwKeeperIp.split(","));
        for (int i=0;i<gwKeeperIps.size();i++){
            if ( !StringUtils.isEmpty(gwKeeperIps.get(i)) ){
                VoipFlowManageIp voipFlowManageIp = new VoipFlowManageIp();
                voipFlowManageIp.setVoipflowId(voipFlowStrategy.getVoipFlowId());
                voipFlowManageIp.setVoipIp(gwKeeperIps.get(i));
                voipFlowManageIp.setVoipType(voipGwKeeperType);
                voipGwKeeperList.add(voipFlowManageIp);
            }
        }
        voipFlowStrategy.setVoipGwIpList(voipGwList);
        voipFlowStrategy.setVoipGwKeeperIpList(voipGwKeeperList);
    }

    /**
     * 生成对应绑定策略查询条件
     * @param voipFlowStrategy
     * @return
     */
    private Map<String,Object> generateQueryConditionMap(VoipFlowStrategy voipFlowStrategy){
        Map<String,Object> queryCondition = Maps.newHashMap();
        queryCondition.put("messageNo",voipFlowStrategy.getMessageNo());
        queryCondition.put("userBindMessageType",voipFlowStrategy.getMessageType());
        queryCondition.put("operateType",OperationConstants.OPERATION_DELETE);
        return queryCondition;
    }

    /**
     *
     * @param userPolicyBindStrategyList
     * @param voipFlowStrategy
     */
    private void deleteExistBindPolicy(List<UserPolicyBindStrategy> userPolicyBindStrategyList,VoipFlowStrategy voipFlowStrategy){

        voipFlowStrategy.setMessageType(MessageTypeConstants.MESSAGE_TYPE_VOIP_FLOW_POLICY);

        for (UserPolicyBindStrategy userPolicyBindStrategy : userPolicyBindStrategyList) {
            List<BindMessage> bindMessages = new ArrayList<>();
            BindMessage bindMessage = new BindMessage();
            bindMessage.setBindMessageNo(voipFlowStrategy.getMessageNo());
            bindMessage.setBindMessageType(voipFlowStrategy.getMessageType());
            bindMessages.add(bindMessage);
            userPolicyBindStrategy.setBindInfo(bindMessages);
            userPolicyBindService.deletePolicy(userPolicyBindStrategy);

        }
    }

    private OperationLog createOperationLogByStrategy(VoipFlowStrategy voipFlowStrategy){
        OperationLog operationLog = new OperationLog();
        if ( voipFlowStrategy.getModifyOper() != null ){
            operationLog.setOperUser(voipFlowStrategy.getModifyOper());
        } else {
            operationLog.setOperUser("admin");
        }

        operationLog.setOperModel(102004);
        // 定时发送时修改操作时间为当前
        operationLog.setOperTime(new Date());
        // 定时重发
        operationLog.setOperType(LogConstant.SCHEDULED_OPERATION_TYPE);

        operationLog.setClientIp(getLocalHost());
        operationLog.setClientPort(LogConstant.SERVER_PORT);
        operationLog.setServerName(getLocalHost());
        operationLog.setDataJson("messageNo="+voipFlowStrategy.getMessageNo());
        return operationLog;
    }

    /**
     * 根据voipFlowStrategy实体构造Policy实体
     * @param voipFlowStrategy
     * @return
     */
    private Policy createPolicyBeanByBaseVo(VoipFlowStrategy voipFlowStrategy){
        Policy policyNo = new Policy();
        policyNo.setMessageType(voipFlowStrategy.getMessageType());
        policyNo.setMessageName(voipFlowStrategy.getMessageName());
        policyNo.setOperateType(voipFlowStrategy.getOperationType());
        if ( !org.apache.commons.lang3.StringUtils.isEmpty(voipFlowStrategy.getCreateOper()) ){
            policyNo.setCreateOper(voipFlowStrategy.getCreateOper());
        }
        if ( !org.apache.commons.lang3.StringUtils.isEmpty(voipFlowStrategy.getModifyOper()) ){
            policyNo.setModifyOper(voipFlowStrategy.getModifyOper());
        }
        if ( voipFlowStrategy.getCreateTime() != null ){
            policyNo.setCreateTime(voipFlowStrategy.getCreateTime());
        }
        if ( voipFlowStrategy.getModifyTime() != null ){
            policyNo.setModifyTime(voipFlowStrategy.getModifyTime());
        }
        return policyNo;
    }

}
