package com.aotain.taskmonitor.policytimer.mapper;

import com.aotain.common.config.annotation.MyBatisDao;
import com.aotain.taskmonitor.policytimer.model.Policy;

/**
 * 
 * ClassName: PolicyMapper
 * Description: 定时发送policy更新数据库
 * date: 2018年3月9日 上午8:30:18
 * 
 * @author tanzj
 * @version  
 * @since JDK 1.8
 */
@MyBatisDao
public interface PolicyMapper {

    int insertSelective(Policy record);

    int updatePolicyByMessageNoAndType(Policy record);

    
}