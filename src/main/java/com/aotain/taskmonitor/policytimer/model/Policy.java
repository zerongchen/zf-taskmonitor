package com.aotain.taskmonitor.policytimer.model;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class Policy implements Serializable {
	@NonNull private Long messageNo;

	@NonNull private String messageName;

	@NonNull private Integer operateType;
	
    @NonNull private Integer messageType;

    private static final long serialVersionUID = 1L;

//    //以前需要
//    private Date startTime;
//
//    private Date endTime;
//
//    private Long flag;
    
    //新表结构
    private Date createTime;

    private Date modifyTime;

    private String modifyOper;
    
    private String createOper;
    
    private Long messageSequenceno;
    
    

}