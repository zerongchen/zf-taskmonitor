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
import com.aotain.common.policyapi.model.UserPolicyBindStrategy;
import com.aotain.common.policyapi.model.WebFlowStrategy;
import com.aotain.common.policyapi.model.base.BaseVO;
import com.aotain.common.policyapi.model.msg.BindMessage;
import com.aotain.common.utils.date.DateFormatConstant;
import com.aotain.common.utils.date.DateUtils;
import com.aotain.common.utils.redis.MessageNoUtil;
import com.aotain.common.utils.redis.MessageSequenceNoUtil;
import com.aotain.taskmonitor.policytimer.mapper.PolicyMapper;
import com.aotain.taskmonitor.policytimer.mapper.UserPolicyBindMapper;
import com.aotain.taskmonitor.policytimer.mapper.WebFlowManageMapper;
import com.aotain.taskmonitor.policytimer.model.Policy;
import com.aotain.taskmonitor.policytimer.model.WebFlowManage;
import com.aotain.taskmonitor.policytimer.model.WebFlowManageWebType;
import com.aotain.taskmonitor.policytimer.model.WebFlowUserGroup;


@Service
@Transactional(propagation=Propagation.REQUIRED,rollbackFor={Exception.class})
public class WebFlowManageServiceImpl extends BaseService implements WebFlowManageService {
	
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebFlowManageServiceImpl.class);

	@Autowired
	private WebFlowManageMapper policyWebflowMapper;
	
    @Autowired
    private PolicyMapper policyMapper;

    @Autowired
    private UserPolicyBindMapper userPolicyBindMapper;
    
    @Autowired
    private UserPolicyBindService userPolicyBindService;
    
    
	@Override
	protected boolean addDb(BaseVO baseVO) {
		if(baseVO instanceof WebFlowStrategy){
			Policy policy = new Policy();
            policy.setMessageName(baseVO.getMessageName());
            policy.setMessageType(MessageType.WEB_FLOW_POLICY.getId());
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
		if(baseVO instanceof WebFlowStrategy){
			Policy policy = new Policy();
            policy.setMessageName(baseVO.getMessageName());
            policy.setMessageType(MessageType.WEB_FLOW_POLICY.getId());
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
		if(baseVO instanceof WebFlowStrategy){
			Policy policy = new Policy();
            policy.setMessageName(baseVO.getMessageName());
            policy.setMessageType(MessageType.WEB_FLOW_POLICY.getId());
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
    protected void reUserBind(WebFlowManage webFlow){
        //绑定信息
    	for(WebFlowUserGroup record:webFlow.getUserGroup()) {
    		UserPolicyBindStrategy userPolicyBindStrategy = new UserPolicyBindStrategy();
    		List<BindMessage> bindInfo = new ArrayList<BindMessage>();
    		BindMessage bind = new BindMessage();
    		bind.setBindMessageNo(webFlow.getMessageNo());
    		bind.setBindMessageType(MessageType.WEB_FLOW_POLICY.getId());
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
		List<WebFlowManage> info = policyWebflowMapper.getIndexList(query);
		if(info.size()>0) {
			for(WebFlowManage webFlow:info) {
				WebFlowStrategy webFlowStrategy =new WebFlowStrategy();
				long now = DateUtils.parse2TimesTamp(DateUtils.formatDateyyyyMMdd(new Date()),DateFormatConstant.DATE_WITHOUT_SEPARATOR_SHORT);
				if(webFlow.getStartTime().equals("0") || (now>= (Long.valueOf(webFlow.getStartTime())-86400) &&
						(webFlow.getEndTime().equals("0") || now <= (Long.valueOf(webFlow.getEndTime())+86400)))) {
					List<Integer>  webtypes = new ArrayList<Integer>();
					for(WebFlowManageWebType webType:webFlow.getWebTypes()) {
						webtypes.add(webType.getWebType());
					}
					webFlowStrategy.setMessageSequenceNo(MessageSequenceNoUtil.getInstance().getSequenceNo(MessageType.WEB_FLOW_POLICY.getId()));
					webFlowStrategy.setCTime(webFlow.getTime());
					webFlowStrategy.setAdvUrl(webFlow.getAdvUrl());
					webFlowStrategy.setCType(webFlow.getCtype());
					webFlowStrategy.setMessageType(MessageType.WEB_FLOW_POLICY.getId());
					webFlowStrategy.setProbeType(ProbeType.DPI.getValue());
					webFlowStrategy.setMessageName(webFlow.getMessageName());
					webFlowStrategy.setCWebType(webtypes);
					webFlowStrategy.setWebFlowId(webFlow.getWebflowId());
					if(webFlow.getMessageNo()==null) {
						webFlowStrategy.setMessageNo(MessageNoUtil.getInstance().getMessageNo(MessageType.WEB_FLOW_POLICY.getId()));
						webFlowStrategy.setOperationType(OperationConstants.OPERATION_SAVE);
						webFlow.setMessageNo(webFlowStrategy.getMessageNo());
						//策略下发
						addPolicy(webFlowStrategy);
						//绑定用户策略下发
						reUserBind(webFlow);
						policyWebflowMapper.updateByPrimaryKeySelective(webFlow);
					}else {
						webFlowStrategy.setOperationType(OperationConstants.OPERATION_SAVE);
						webFlowStrategy.setMessageNo(webFlow.getMessageNo());
						//策略下发
						modifyPolicy(webFlowStrategy);
					}
				}
			}
		}
		
	}
	
}
