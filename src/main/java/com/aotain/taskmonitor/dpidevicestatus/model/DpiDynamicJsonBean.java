package com.aotain.taskmonitor.dpidevicestatus.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/01
 */
@Getter
@Setter
public class DpiDynamicJsonBean {

    private Date createTime;

    private List<DpiDynamicPort> totalPorts;

    private List<DpiDynamicCpu> totalCPU;

    public void setCreateTime(long createTime) {
        this.createTime = new Date(createTime*1000);
    }

}
