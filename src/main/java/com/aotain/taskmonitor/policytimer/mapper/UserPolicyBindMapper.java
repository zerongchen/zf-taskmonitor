package com.aotain.taskmonitor.policytimer.mapper;

import java.util.List;
import java.util.Map;

import com.aotain.common.config.annotation.MyBatisDao;
import com.aotain.common.policyapi.model.UserPolicyBindStrategy;

/**
 * 
 * ClassName: UserPolicyBindMapper
 * Description: 用户绑定数据库更新
 * date: 2018年3月9日 上午8:48:34
 * 
 * @author tanzj 
 * @version  
 * @since JDK 1.8
 */
@MyBatisDao
public interface UserPolicyBindMapper {
    
    int insertSelective(UserPolicyBindStrategy record);
    
    /**
     * 
    * @Title: getByBindMessages
    * @Description: 获取多个绑定策略 
    * @param @param record
    * @param @return
    * @return List<UserPolicyBindStrategy>
    * @throws
     */
    List<UserPolicyBindStrategy> getByBindMessages(UserPolicyBindStrategy record);
    
    /**
     * 
    * @Title: updateOrDelete 
    * @Description: 修改或者删除策略，逻辑删除(这里用一句话描述这个方法的作用) 
    * @param @param record
    * @param @return    设定文件 
    * @return int    返回类型 
    * @throws
     */
    int updateOrDelete(UserPolicyBindStrategy record);
    
    
    String getUserGroupNameById(long id);

    List<UserPolicyBindStrategy> getByBindMessageNoAndType(Map<String,Object> bindQuery);
}