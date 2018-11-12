package com.aotain.taskmonitor.policytimer;

import com.aotain.taskmonitor.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aotain.taskmonitor.policytimer.policy.GeneralFlowManagerService;
import com.aotain.taskmonitor.policytimer.policy.IVoipFlowManageService;
import com.aotain.taskmonitor.policytimer.policy.ShareManageService;
import com.aotain.taskmonitor.policytimer.policy.WebFlowManageService;
import com.aotain.taskmonitor.policytimer.policy.flowmirror.IFlowMirrorService;

/**
 * 
 * ClassName: PolicyTimer
 * Description:策略定时下发
 * date: 2018年3月9日 上午10:08:37
 * 
 * @author tanzj
 * @version  
 * @since JDK 1.8
 */
@Component
public class PolicyTimer {
	private Logger log = LoggerFactory.getLogger(PolicyTimer.class);
	
	@Autowired
	private WebFlowManageService webFlowManageService;

	@Autowired
	private GeneralFlowManagerService generalFlowManagerService;

	@Autowired
	private IVoipFlowManageService voipFlowManageService;

	@Autowired
	private IFlowMirrorService flowMirrorService;
	
	@Autowired
	private ShareManageService shareManageService;
	
	/**
	 * 
	* @Title: sendMessage
	* @Description: web流量管理策略定时下发
	* @param 
	* @return void
	* @throws
	 */
	@Scheduled(cron = "0 0 0 * * ?")
    public void webFlowManageMessage(){
		if(Main.LSELECTOR == null || !Main.LSELECTOR.getLeader()){
			log.debug("it is not the leader node...");
			return;
		}
		log.info("webFlowManage policy send start");
		webFlowManageService.timerPolicy();
		log.info("webFlowManage policy send end");
	}

	/**
	 *
	 * @Title: sendMessage
	 * @Description: 通用流量管理策略定时下发
	 * @param
	 * @return void
	 * @throws
	 */
	@Scheduled(cron = "0 0 0 * * ?")
	public void generalFlowManageMessage(){
		if(Main.LSELECTOR == null || !Main.LSELECTOR.getLeader()){
			log.debug("it is not the leader node...");
			return;
		}
		log.info("General Flow manager policy send start");
		generalFlowManagerService.timerPolicy();
		log.info("General Flow manager policy send end");
	}

	/**
	 *
	 * @Title: sendMessage
	 * @Description: voip流量管理策略定时下发
	 * @param
	 * @return void
	 * @throws
	 */
	@Scheduled(cron = "0 0 0 * * ?")
	public void voipFlowManageMessage(){
		if(Main.LSELECTOR == null || !Main.LSELECTOR.getLeader()){
			log.debug("it is not the leader node...");
			return;
		}
		log.info("General voipFlow manager policy send start");
		voipFlowManageService.timerPolicy();
		log.info("General voipFlow manager policy send end");
	}

	/**
	 *
	 * @Title: sendMessage
	 * @Description: 流量镜像策略定时下发
	 * @param
	 * @return void
	 * @throws
	 */
//	@Scheduled(cron = "0 0/2 * * * ?")
//	public void flowMirrorManagerMessage(){
//		log.info("General flowMirror manager policy send start");
//		flowMirrorService.timerPolicy();
//		log.info("General flowMirror manager policy send end");
//	}

	/**
	 * 
	* @Title: sendMessage
	* @Description: 1拖N用户管理策略定时下发
	* @param 
	* @return void
	* @throws
	 */
	@Scheduled(cron = "0 0 0 * * ?")
    public void shareManageMessage(){
		if(Main.LSELECTOR == null || !Main.LSELECTOR.getLeader()){
			log.debug("it is not the leader node...");
			return;
		}
		log.info("shareManage policy send start");
		shareManageService.timerPolicy();
		log.info("shareManage policy send end");
	}
	
}
