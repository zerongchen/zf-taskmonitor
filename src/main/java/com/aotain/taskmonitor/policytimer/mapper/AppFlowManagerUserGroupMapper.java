package com.aotain.taskmonitor.policytimer.mapper;

import com.aotain.common.config.annotation.MyBatisDao;
import com.aotain.taskmonitor.policytimer.model.AppFlowManagerUserGroup;

import java.util.List;


@MyBatisDao
public interface AppFlowManagerUserGroupMapper {
    List<AppFlowManagerUserGroup> getAppGroup( AppFlowManagerUserGroup record );
}