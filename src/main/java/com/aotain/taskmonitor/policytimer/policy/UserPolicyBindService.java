package com.aotain.taskmonitor.policytimer.policy;

import java.util.Date;
import java.util.List;

import com.aotain.taskmonitor.policytimer.log.OperationLog;
import com.aotain.taskmonitor.policytimer.log.OperationLogMapper;
import org.apache.curator.framework.api.transaction.OperationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aotain.common.policyapi.base.BaseService;
import com.aotain.common.policyapi.constant.BindAction;
import com.aotain.common.policyapi.constant.MessageType;
import com.aotain.common.policyapi.constant.OperationConstants;
import com.aotain.common.policyapi.constant.ProbeType;
import com.aotain.common.policyapi.model.UserPolicyBindStrategy;
import com.aotain.common.policyapi.model.base.BaseVO;
import com.aotain.common.policyapi.model.msg.BindMessage;
import com.aotain.common.utils.redis.MessageNoUtil;
import com.aotain.common.utils.redis.MessageSequenceNoUtil;
import com.aotain.taskmonitor.policytimer.mapper.PolicyMapper;
import com.aotain.taskmonitor.policytimer.mapper.UserPolicyBindMapper;
import com.aotain.taskmonitor.policytimer.model.Policy;

import static com.aotain.common.utils.monitorstatistics.ExceptionCollector.getLocalHost;


/**
 * 
* @ClassName: UserPolicyBindService 
* @Description: 用户应用绑定策略，该策略只针对表
*  zf_v2_policy_userpolicy_bind 和 zf_v2_policy_messageno 操作(这里用一句话描述这个类的作用) 
* @author DongBoye
* @date 2018年2月6日 下午3:06:41 
*
 */
@Service
public class UserPolicyBindService extends BaseService{
	
	private static Logger logger = LoggerFactory.getLogger(UserPolicyBindService.class);

	private static int serverPort = 8895;
	
	@Autowired
	private PolicyMapper policyMapper;

	@Autowired
	private OperationLogMapper operationLogMapper;
	
	@Autowired
	private UserPolicyBindMapper userPolicyBindMapper;
	/**
	 * 注意要传入BindMessage的值
	 */
	@Override
	protected boolean addDb(BaseVO vo) {
		if(vo instanceof UserPolicyBindStrategy) {
			//转换
			UserPolicyBindStrategy record = (UserPolicyBindStrategy) vo ;
			
			Policy policy = new Policy();
			policy.setMessageType(MessageType.USER_POLICY_BIND.getId());
			Date time = new Date();
			long messageNo = MessageNoUtil.getInstance().getMessageNo(MessageType.USER_POLICY_BIND.getId());
			long messageSqNo = MessageSequenceNoUtil.getInstance().getSequenceNo(MessageType.USER_POLICY_BIND.getId());
			
			policy.setMessageNo(messageNo);
			policy.setMessageType(MessageType.USER_POLICY_BIND.getId());
	        policy.setMessageName(MessageType.USER_POLICY_BIND.getMessageType());
	        policy.setMessageSequenceno(messageSqNo);
	        policy.setCreateTime(time);
	        policy.setModifyTime(time);
	        //后续实现
	        policy.setCreateOper("admin");
	        policy.setModifyOper("admin");

	        policy.setOperateType(OperationConstants.OPERATION_SAVE);
	        
	        record.setMessageType(MessageType.USER_POLICY_BIND.getId());
	        record.setMessageNo(messageNo);
	        record.setMessageSequenceNo(messageSqNo);
	        record.setOperationType(OperationConstants.OPERATION_SAVE);
	        record.setCreateTime(time);
	        record.setModifyTime(time);
	        record.setCreateOper("admin");
	        record.setModifyOper("admin");
	        record.setBindAction(BindAction.BIND.getValue());
			record.setProbeType(ProbeType.DPI.getValue());
	        /**
	         * 需要补充createOper和modifyOper
	         */
	        policyMapper.insertSelective(policy);
	        List<BindMessage> bindInfoList = record.getBindInfo();
	        if(bindInfoList != null && bindInfoList.size() > 0) {
	        	for(BindMessage bindMessage: bindInfoList) {
	        		record.setUserBindMessageNo(bindMessage.getBindMessageNo());
	        		record.setUserBindMessageType(bindMessage.getBindMessageType());
	        		userPolicyBindMapper.insertSelective(record);

	        		// 添加日志
//					try{
//						OperationLog operationLog = createOperationLogByStrategy(record);
//						operationLogMapper.insertSelective(operationLog);
//					}catch (Exception e){
//						e.printStackTrace();
//						logger.info("add operation Log failed...");
//					}
	        	}
	        }else {
	        	logger.error("you have no bind message, error add database!");
	        }
	        return true;
		}
		return false;
	}
	/**
	 * 注意要传入BindMessage的值
	 */
	@Override
	protected boolean deleteDb(BaseVO vo) {
		if(vo instanceof UserPolicyBindStrategy) {
			//转换
			UserPolicyBindStrategy record = (UserPolicyBindStrategy) vo ;
			long messageSqNo = MessageSequenceNoUtil.getInstance().getSequenceNo(MessageType.USER_POLICY_BIND.getId());

			Policy policy = new Policy();
			policy.setMessageType(MessageType.USER_POLICY_BIND.getId());
			policy.setMessageNo(record.getMessageNo());
			policy.setOperateType(3);
			policy.setMessageSequenceno(messageSqNo);
			policy.setModifyOper("admin");
			policy.setCreateOper("admin");


	        Date time = new Date();
	        record.setMessageSequenceNo(messageSqNo);
	        record.setOperationType(OperationConstants.OPERATION_DELETE);
	        record.setCreateTime(time);
	        record.setModifyTime(time);
	        record.setBindAction(BindAction.UNBIND.getValue());
			record.setProbeType(ProbeType.DPI.getValue());
			record.setCreateOper("admin");
			record.setModifyOper("admin");
			record.setOperationType(3);
			record.setMessageType(MessageType.USER_POLICY_BIND.getId());

	        policyMapper.updatePolicyByMessageNoAndType(policy);
	        
	        List<BindMessage> bindInfoList = record.getBindInfo();
	        if(bindInfoList != null && bindInfoList.size() > 0) {
	        	for(BindMessage bindMessage: bindInfoList) {
	        		record.setUserBindMessageNo(bindMessage.getBindMessageNo());
	        		record.setUserBindMessageType(bindMessage.getBindMessageType());
	        		userPolicyBindMapper.updateOrDelete(record);

//					try{
//						OperationLog operationLog = createOperationLogByStrategy(record);
//						operationLogMapper.insertSelective(operationLog);
//					}catch (Exception e){
//						e.printStackTrace();
//						logger.info("add operation Log failed...");
//					}
	        	}
	        }else {
	        	logger.error("you have no bind message, error delete database!");
	        }
	        
	        return true;
		}
		return false;
	}

	@Override
	protected boolean modifyDb(BaseVO vo) {
		return true;
	}

	@Override
	protected boolean addCustomLogic(BaseVO vo) {
		return setPolicyOperateSequenceToRedis(vo) && addTaskAndChannelToRedis(vo);
	}

	@Override
	protected boolean modifyCustomLogic(BaseVO vo) {
		return true;
	}

	@Override
	protected boolean deleteCustomLogic(BaseVO vo) {
		return setPolicyOperateSequenceToRedis(vo) && addTaskAndChannelToRedis(vo);
	}

	private OperationLog createOperationLogByStrategy(UserPolicyBindStrategy userPolicyBindStrategy){
		OperationLog operationLog = new OperationLog();
		if ( userPolicyBindStrategy.getModifyOper() != null ){
			operationLog.setOperUser(userPolicyBindStrategy.getModifyOper());
		} else {
			operationLog.setOperUser("admin");
		}
		if ( userPolicyBindStrategy.getModifyTime() != null ){
			operationLog.setOperTime(userPolicyBindStrategy.getModifyTime());
		} else {
			operationLog.setOperTime(new Date());
		}
		operationLog.setOperModel(103003);
		if (userPolicyBindStrategy.getOperationType()==1){
			operationLog.setOperType(1);
		} else if (userPolicyBindStrategy.getOperationType()==2){
			operationLog.setOperType(2);
		} else if (userPolicyBindStrategy.getOperationType()==3){
			operationLog.setOperType(3);
		}

		operationLog.setClientIp(getLocalHost());
		operationLog.setClientPort(serverPort);
		operationLog.setServerName(getLocalHost());
		operationLog.setDataJson("messageNo="+userPolicyBindStrategy.getMessageNo());
		return operationLog;
	}

}
