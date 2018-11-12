package com.aotain.taskmonitor.policytimer.policy;

import com.aotain.common.policyapi.base.BaseService;
import com.aotain.common.policyapi.constant.MessageType;
import com.aotain.common.policyapi.constant.ProbeType;
import com.aotain.common.policyapi.model.GeneralFlowStrategy;
import com.aotain.common.policyapi.model.UserPolicyBindStrategy;
import com.aotain.common.policyapi.model.base.BaseVO;
import com.aotain.common.policyapi.model.msg.BindMessage;
import com.aotain.common.utils.redis.MessageNoUtil;
import com.aotain.common.utils.redis.MessageSequenceNoUtil;
import com.aotain.taskmonitor.policytimer.mapper.AppFlowManagerMapper;
import com.aotain.taskmonitor.policytimer.mapper.AppFlowManagerUserGroupMapper;
import com.aotain.taskmonitor.policytimer.mapper.PolicyMapper;
import com.aotain.taskmonitor.policytimer.model.AppFlowManager;
import com.aotain.taskmonitor.policytimer.model.AppFlowManagerUserGroup;
import com.aotain.taskmonitor.policytimer.model.Policy;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@Transactional(propagation= Propagation.REQUIRED,rollbackFor={Exception.class})
public class GeneralFlowManagerService extends BaseService{

    @Autowired
    private AppFlowManagerMapper appFlowManagerMapper;

    @Autowired
    private PolicyMapper policyMapper;

    @Autowired
    private UserPolicyBindService userPolicyBindService;

    @Autowired
    private AppFlowManagerUserGroupMapper appFlowManagerUserGroupMapper;

    public void timerPolicy() {
        List<AppFlowManager> list =  appFlowManagerMapper.getList();
        if(list==null || list.size()<1) return;

        for(AppFlowManager appFlowManager:list){
           if(sendPolicy(appFlowManager)){
               long messageSeqNo = MessageSequenceNoUtil.getInstance().getSequenceNo(MessageType.GENERAL_FLOW_POLICY.getId());
               GeneralFlowStrategy strategy = new GeneralFlowStrategy();
               strategy.setMessageType(MessageType.GENERAL_FLOW_POLICY.getId());
               strategy.setMessageSequenceNo(messageSeqNo);
               strategy.setMessageName(appFlowManager.getMessageName());
               strategy.setCTime(appFlowManager.getCTime());
               strategy.setAppThresholdUp(appFlowManager.getAppThresholdUpAbs());
               strategy.setAppThresholdDown(appFlowManager.getAppThresholdDnAbs());
               strategy.setAppType(appFlowManager.getApptype());
               strategy.setAppId(appFlowManager.getAppid());
               strategy.setAppName(appFlowManager.getAppid()==0?appFlowManager.getAppname():"");
               strategy.setAppflowId(appFlowManager.getAppFlowId());
               if(appFlowManager.getMessageNo()!=null){
                   strategy.setMessageNo(appFlowManager.getMessageNo());
                   modifyPolicy(strategy);
               }else {
                   Long messageNo = MessageNoUtil.getInstance().getMessageNo(MessageType.GENERAL_FLOW_POLICY.getId());
                   strategy.setMessageNo(messageNo);
                   Policy policy = new Policy();
                   policy.setMessageName(strategy.getMessageName());
                   policy.setMessageNo(messageNo);
                   policy.setMessageType(MessageType.GENERAL_FLOW_POLICY.getId());
                   policy.setMessageSequenceno(messageSeqNo);
                   policy.setCreateOper(appFlowManager.getCreateOper());
                   policy.setModifyOper(appFlowManager.getModifyOper());
                   Date date = new Date();
                   policy.setCreateTime(date);
                   policy.setModifyTime(date);
                   policy.setOperateType(1);
                   policyMapper.insertSelective(policy);
                   addPolicy(strategy);
                   userBind(strategy,appFlowManager.getAppFlowId());
               }
           }
        }
    }

    /**
     * 判断当前操作是否是在策略有效期
     * @param flowManager
     * @return
     */
    private boolean sendPolicy(AppFlowManager flowManager) {


        Calendar nowD = Calendar.getInstance();
        Long now = nowD.getTimeInMillis();
        if (flowManager.getREndtime() == (long) 0) {
            if(flowManager.getRStarttime() == (long) 0){
                return true;
            }else {
                Calendar startD = Calendar.getInstance();
                startD.setTimeInMillis(flowManager.getRStarttime()*1000);
                if(now >= startD.getTimeInMillis()){
                    return true;
                }else {
                    return false;
                }
            }
        }else{
            Calendar endD = Calendar.getInstance();
            endD.setTimeInMillis(flowManager.getREndtime()*1000);
            endD.set(Calendar.DATE, endD.get(Calendar.DATE) + 1);
            if(now < endD.getTimeInMillis()){
                if(flowManager.getRStarttime() == (long) 0){
                    return true;
                }else {
                    Calendar startD = Calendar.getInstance();
                    startD.setTimeInMillis(flowManager.getRStarttime()*1000);
                    if(now >= startD.getTimeInMillis()){
                        return true;
                    }else {
                        return false;
                    }
                }
            }else {
                return false;
            }
        }
    }

    /**
     * 用户组绑定
     * @param appFlowId
     */
    protected void userBind(GeneralFlowStrategy generalFlowStrategy,Long appFlowId){
        AppFlowManagerUserGroup appFlowManagerUserGroup = new AppFlowManagerUserGroup();
        appFlowManagerUserGroup.setAppflowId(appFlowId);
        List<AppFlowManagerUserGroup> list =  appFlowManagerUserGroupMapper.getAppGroup(appFlowManagerUserGroup);

        UserPolicyBindStrategy strategy = new UserPolicyBindStrategy();
        if(list!=null && list.size()>0){
            for (AppFlowManagerUserGroup userGroup:list){
                UserPolicyBindStrategy userPolicyBindStrategy = new UserPolicyBindStrategy();
                List<BindMessage> bindInfo = new ArrayList<BindMessage>();
                BindMessage bind = new BindMessage();
                bind.setBindMessageNo(generalFlowStrategy.getMessageNo());
                bind.setBindMessageType(MessageType.GENERAL_FLOW_POLICY.getId());
                bindInfo.add(bind);
                userPolicyBindStrategy.setBindInfo(bindInfo);
                userPolicyBindStrategy.setMessageType(MessageType.USER_POLICY_BIND.getId());
                userPolicyBindStrategy.setUserType(userGroup.getUserType());
                if(userGroup.getUserType() == 1 || userGroup.getUserType()==2){
                    userPolicyBindStrategy.setUserName(userGroup.getUserName());
                    userPolicyBindStrategy.setUserGroupId((long)0);
                }else if(userGroup.getUserType() == 3){
                    userPolicyBindStrategy.setUserGroupId(userGroup.getUserGroupId());
                    userPolicyBindStrategy.setUserName(userGroup.getUserName());
                }else {
                    userPolicyBindStrategy.setUserGroupId((long)0);
                    userPolicyBindStrategy.setUserName("");
                }
                userPolicyBindService.addPolicy(userPolicyBindStrategy);
            }
        }
    }

    @Override
    protected boolean addDb( BaseVO baseVO ) {

        if(baseVO instanceof GeneralFlowStrategy){
            GeneralFlowStrategy strategy = (GeneralFlowStrategy)baseVO;
            AppFlowManager appFlowManager = new AppFlowManager();
            appFlowManager.setAppFlowId(strategy.getAppflowId());
            appFlowManager.setOperateType(1);
            appFlowManager.setMessageNo(strategy.getMessageNo());
            appFlowManagerMapper.updateByPrimaryKeySelective(appFlowManager);
            strategy.setOperationType(1);
            strategy.setProbeType(ProbeType.DPI.getValue());
            return true;

        }
        return false;
    }

    @Override
    protected boolean deleteDb( BaseVO baseVO ) {
        return true;
    }

    @Override
    protected boolean modifyDb( BaseVO baseVO ) {
        if(baseVO instanceof GeneralFlowStrategy){
            GeneralFlowStrategy strategy = (GeneralFlowStrategy)baseVO;
//            AppFlowManager appFlowManager = new AppFlowManager();
//            appFlowManager.setAppFlowId(strategy.getAppflowId());
//            appFlowManager.setOperateType(2);
//            appFlowManagerMapper.updateByPrimaryKeySelective(appFlowManager);
            Policy policy = new Policy();
            policy.setMessageNo(strategy.getMessageNo());
            policy.setMessageType(MessageType.GENERAL_FLOW_POLICY.getId());
            policy.setMessageSequenceno(strategy.getMessageSequenceNo());
            Date date = new Date();
            policy.setModifyTime(date);
            policy.setOperateType(2);
            policyMapper.updatePolicyByMessageNoAndType(policy);
            strategy.setOperationType(1);
            strategy.setProbeType(ProbeType.DPI.getValue());
            return true;
        }
        return false;
    }

    @Override
    protected boolean addCustomLogic( BaseVO baseVO ) {
        return setPolicyOperateSequenceToRedis(baseVO) && addTaskAndChannelToRedis(baseVO);
    }

    @Override
    protected boolean modifyCustomLogic( BaseVO baseVO ) {
        return setPolicyOperateSequenceToRedis(baseVO) && addTaskAndChannelToRedis(baseVO);
    }

    @Override
    protected boolean deleteCustomLogic( BaseVO baseVO ) {
        return false;
    }
}
