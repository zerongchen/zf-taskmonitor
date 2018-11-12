package com.aotain.taskmonitor.dpidevicestatus.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/01
 */
@Getter
@Setter
public class DpiStaticPortJsonBean {

    private Integer portType;

    private List<DpiStaticPort> ports;
}
