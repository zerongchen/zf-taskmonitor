package com.aotain.taskmonitor.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/05/24
 */
@Configuration
public class MonitorConfigUtils {

    @Value("${monitor.type}")
    private Integer serviceType;

    @Value("${monitor.threadNum}")
    private Integer threadNum;

    @Value("${select.path}")
    private String selectPath;

    @Value("${select.name}")
    private String selectName;

    @Value("${select.sessionTimeout}")
    private Long selectSessionTimeout;

    @Value("${select.connectTimeout}")
    private Long selectConnectTimeout;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    public Integer getServiceType() {
        return serviceType;
    }

    public void setServiceType(Integer serviceType) {
        this.serviceType = serviceType;
    }

    public Integer getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(Integer threadNum) {
        this.threadNum = threadNum;
    }

    public String getSelectPath() {
        return selectPath;
    }

    public void setSelectPath(String selectPath) {
        this.selectPath = selectPath;
    }

    public String getSelectName() {
        return selectName;
    }

    public void setSelectName(String selectName) {
        this.selectName = selectName;
    }

    public Long getSelectSessionTimeout() {
        return selectSessionTimeout;
    }

    public void setSelectSessionTimeout(Long selectSessionTimeout) {
        this.selectSessionTimeout = selectSessionTimeout;
    }

    public Long getSelectConnectTimeout() {
        return selectConnectTimeout;
    }

    public void setSelectConnectTimeout(Long selectConnectTimeout) {
        this.selectConnectTimeout = selectConnectTimeout;
    }
}
