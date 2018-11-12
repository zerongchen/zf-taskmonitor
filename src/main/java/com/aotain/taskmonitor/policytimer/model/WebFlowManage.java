package com.aotain.taskmonitor.policytimer.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WebFlowManage implements Serializable {

    private Integer webflowId;

    private Integer ctype;

    private Long time;

    private String advUrl;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")  
    private Date modifyTime;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")  
    private Date createTime;

    private String createOper;

    private String modifyOper;

    private Long messageNo;

    private String messageName;
    
    private String startTime;
    
    private String endTime;
    
    private Integer operateType;
    
    private String RStartTime;
    
    private String REndTime;
    
    private static final long serialVersionUID = 1L;

    private List<WebFlowManageWebType> webTypes;

    private List<WebFlowUserGroup> userGroup;
}
