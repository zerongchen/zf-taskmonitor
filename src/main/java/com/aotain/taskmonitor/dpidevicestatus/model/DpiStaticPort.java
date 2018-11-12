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
public class DpiStaticPort {

    private String deploySiteName;

    private Integer portType;

    private Integer portNo;

    private String portDescription;

    private Integer mLinkId;

    private String mLinkDesc;

    private Date createTime;

}
