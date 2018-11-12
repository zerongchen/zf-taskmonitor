package com.aotain.taskmonitor.policytimer.mapper;

import java.util.List;
import java.util.Map;

import com.aotain.common.config.annotation.MyBatisDao;
import com.aotain.taskmonitor.policytimer.model.WebFlowManage;

/**
 * 
 * ClassName: WebFlowManageMapper
 * Description: web流量管理
 * date: 2018年3月9日 上午8:48:57
 * 
 * @author HP 
 * @version  
 * @since JDK 1.8
 */
@MyBatisDao
public interface WebFlowManageMapper {
	
    int updateByPrimaryKeySelective(WebFlowManage record);

    List<WebFlowManage> getIndexList(Map<String,Object> query);
    
}