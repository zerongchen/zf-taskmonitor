package com.aotain.taskmonitor.policytimer.mapper;

import java.util.List;
import java.util.Map;

import com.aotain.common.config.annotation.MyBatisDao;
import com.aotain.common.policyapi.model.ShareManageStrategy;

@MyBatisDao
public interface ShareManageMapper {
    int updateByPrimaryKeySelective(ShareManageStrategy record);

    List<ShareManageStrategy> getIndexList(Map<String,Object> query);
}