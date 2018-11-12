package com.aotain.taskmonitor.policytimer.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@NoArgsConstructor
public class WebFlowUserGroup implements Serializable {

    private Long webflowId;

    private Integer userType;

    private Long userGroupId;

    private String userName;

    private static final long serialVersionUID = 1L;


}