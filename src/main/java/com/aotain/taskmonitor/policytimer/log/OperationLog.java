package com.aotain.taskmonitor.policytimer.log;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/04/13
 */
@Getter
@Setter
@Table(name="zf_v2_operation_log")
public class OperationLog {

    @Id
    @Column(name = "id")
    private Long id;

    private Date operTime;

    private String operUser;

    private Integer operModel;

    private Integer operType;

    private String clientIp;

    private Integer clientPort;

    private String serverName;

    private String dataJson;

    private String inputParam;

    private String outputParam;
}
