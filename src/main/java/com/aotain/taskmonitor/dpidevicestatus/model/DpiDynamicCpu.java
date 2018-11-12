package com.aotain.taskmonitor.dpidevicestatus.model;


import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/01
 */
@Getter
@Setter
public class DpiDynamicCpu {

    private Integer cpuNo;

    private Integer cpuUsage;

    private Date createTime;

}
