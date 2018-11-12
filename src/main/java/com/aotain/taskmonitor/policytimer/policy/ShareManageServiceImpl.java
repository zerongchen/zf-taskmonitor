package com.aotain.taskmonitor.policytimer.policy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.aotain.common.policyapi.base.BaseService;
import com.aotain.common.policyapi.constant.MessageType;
import com.aotain.common.policyapi.constant.OperationConstants;
import com.aotain.common.policyapi.constant.ProbeType;
import com.aotain.common.policyapi.model.ShareManageStrategy;
import com.aotain.common.policyapi.model.ShareManageUserGroup;
import com.aotain.common.policyapi.model.UserPolicyBindStrategy;
import com.aotain.common.policyapi.model.base.BaseVO;
import com.aotain.common.policyapi.model.msg.BindMessage;
import com.aotain.common.utils.date.DateFormatConstant;
import com.aotain.common.utils.date.DateUtils;
import com.aotain.common.utils.redis.MessageNoUtil;
import com.aotain.common.utils.redis.MessageSequenceNoUtil;
import com.aotain.taskmonitor.policytimer.mapper.PolicyMapper;
import com.aotain.taskmonitor.policytimer.mapper.ShareManageMapper;
import com.aotain.taskmonitor.policytimer.mapper.UserPolicyBindMapper;
import com.aotain.taskmonitor.policytimer.model.Policy;


@Service
@Transactional(propagation=Propagation.REQUIRED,rollbackFor={Exception.class})
public class ShareManageServiceImpl extends BaseService implements ShareManageService {
	
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ShareManageServiceImpl.class);

	@Autowired
	private ShareManageMapper policyShareMapper;
	
    @Autowired
    private PolicyMapper policyMapper;

    @Autowired
    private UserPolicyBindMapper userPolicyBindMapper;
    
    @Autowired
    private UserPolicyBindService userPolicyBindService;
    
    
	@Override
	protected boolean addDb(BaseVO baseVO) {
		if(baseVO instanceof ShareManageStrategy){
			Policy policy = new Policy();
            policy.setMessageName(baseVO.getMessageName());
            policy.setMessageType(MessageType.SHARE_MANAGE_POLICY.getId());
            policy.setMessageNo(baseVO.getMessageNo());
            policy.setMessageSequenceno(baseVO.getMessageSequenceNo());
            policy.setOperateType(OperationConstants.OPERATION_SAVE);
            policy.setCreateTime(new Date());
            policy.setModifyTime(new Date());
            policy.setModifyOper("admin");
            policy.setCreateOper("admin");
            policyMapper.insertSelective(policy);
		}
		return true;
	}

	@Override
	protected boolean deleteDb(BaseVO baseVO) {
		if(baseVO instanceof ShareManageStrategy){
			Policy policy = new Policy();
            policy.setMessageName(baseVO.getMessageName());
            policy.setMessageType(MessageType.SHARE_MANAGE_POLICY.getId());
            policy.setMessageNo(baseVO.getMessageNo());
            policy.setMessageSequenceno(baseVO.getMessageSequenceNo());
            policy.setOperateType(OperationConstants.OPERATION_DELETE);
            policy.setModifyTime(new Date());
            policy.setModifyOper("admin");
            policyMapper.updatePolicyByMessageNoAndType(policy);
		}
		return false;
	}

	@Override
	protected boolean modifyDb(BaseVO baseVO) {
		if(baseVO instanceof ShareManageStrategy){
			Policy policy = new Policy();
            policy.setMessageName(baseVO.getMessageName());
            policy.setMessageType(MessageType.SHARE_MANAGE_POLICY.getId());
            policy.setMessageNo(baseVO.getMessageNo());
            policy.setMessageSequenceno(baseVO.getMessageSequenceNo());
            policy.setOperateType(OperationConstants.OPERATION_SAVE);
            policy.setModifyTime(new Date());
            policy.setModifyOper("admin");
            policyMapper.updatePolicyByMessageNoAndType(policy);
		}
		return true;
	}

	@Override
	protected boolean addCustomLogic(BaseVO policy) {
		return setPolicyOperateSequenceToRedis(policy) && addTaskAndChannelToRedis(policy);
	}

	@Override
	protected boolean modifyCustomLogic(BaseVO policy) {
		return setPolicyOperateSequenceToRedis(policy) && addTaskAndChannelToRedis(policy);
	}

	@Override
	protected boolean deleteCustomLogic(BaseVO policy) {
		return setPolicyOperateSequenceToRedis(policy) ;
	}

    /**
     * 不同类型的绑定 -- 新增
     * @param record
     */
    protected void reUserBind(ShareManageStrategy share){
        //绑定信息
    	for(ShareManageUserGroup record:share.getUserGroups()) {
    		UserPolicyBindStrategy userPolicyBindStrategy = new UserPolicyBindStrategy();
    		List<BindMessage> bindInfo = new ArrayList<BindMessage>();
    		BindMessage bind = new BindMessage();
    		bind.setBindMessageNo(share.getMessageNo());
    		bind.setBindMessageType(MessageType.SHARE_MANAGE_POLICY.getId());
    		bindInfo.add(bind);
    		userPolicyBindStrategy.setBindInfo(bindInfo);
	        userPolicyBindStrategy.setUserType(record.getUserType());
	        userPolicyBindStrategy.setMessageType(MessageType.USER_POLICY_BIND.getId());
	        if(record.getUserType() == 1 || record.getUserType()==2){
	        	userPolicyBindStrategy.setUserName(record.getUserName());
	        }else if(record.getUserType() == 3){
                userPolicyBindStrategy.setUserGroupId(record.getUserGroupId());
                userPolicyBindStrategy.setUserName(userPolicyBindMapper.getUserGroupNameById(record.getUserGroupId()));
	        }else {
	            userPolicyBindStrategy.setUserName("");
	        }
	        userPolicyBindService.addPolicy(userPolicyBindStrategy);
    	}
    }
    
	@Override
	public void timerPolicy() {
		Map<String,Object> query = new HashMap<String, Object>();
		query.put("messageName", "");
		List<ShareManageStrategy> info = policyShareMapper.getIndexList(query);
		if(info.size()>0) {
			for(ShareManageStrategy share:info) {
				long now = DateUtils.parse2TimesTamp(DateUtils.formatDateyyyyMMdd(new Date()),DateFormatConstant.DATE_WITHOUT_SEPARATOR_SHORT);
				if(share.getStartTime()==0 || (now>= (share.getStartTime()-86400) &&
						(share.getEndTime()==0 || now <= (share.getEndTime()+86400)))) {
					share.setMessageType(MessageType.SHARE_MANAGE_POLICY.getId());
					share.setProbeType(ProbeType.DPI.getValue());
					share.setMessageSequenceNo(MessageSequenceNoUtil.getInstance().getSequenceNo(MessageType.SHARE_MANAGE_POLICY.getId()));
					if(share.getMessageNo()==null) {
						share.setMessageNo(MessageNoUtil.getInstance().getMessageNo(MessageType.SHARE_MANAGE_POLICY.getId()));
						share.setOperationType(OperationConstants.OPERATION_SAVE);
						//策略下发
						addPolicy(share);
						//绑定用户策略下发
						reUserBind(share);
						policyShareMapper.updateByPrimaryKeySelective(share);
					}else {
						share.setOperationType(OperationConstants.OPERATION_SAVE);
						//策略下发
						modifyPolicy(share);
					}
				}
			}
		}
		
	}
	
}
