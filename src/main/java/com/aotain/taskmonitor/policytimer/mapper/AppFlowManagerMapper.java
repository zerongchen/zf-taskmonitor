package com.aotain.taskmonitor.policytimer.mapper;

import com.aotain.common.config.annotation.MyBatisDao;
import com.aotain.taskmonitor.policytimer.model.AppFlowManager;

import java.util.List;

@MyBatisDao
public interface AppFlowManagerMapper {

    int updateByPrimaryKeySelective( AppFlowManager record );

    List<AppFlowManager> getList();

}