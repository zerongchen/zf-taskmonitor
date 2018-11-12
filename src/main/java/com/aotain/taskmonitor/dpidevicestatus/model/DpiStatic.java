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
public class DpiStatic {

    private Integer softwareVersion;

    private String deploySiteName;

    private String idcHouseId;

    private Integer probeType;

    private Integer deployMode;

    private Integer totalCapability;

    private Integer slotNum;

    private Integer preProcSlotNum;

    private Integer analysisSlotNum;

    private Integer gPSlotNum;

    private Date createTime;

    private List<DpiStaticPortJsonBean> portsType;

    public void setCreateTime(long createTime) {
        this.createTime = new Date(createTime*1000);
    }
}
