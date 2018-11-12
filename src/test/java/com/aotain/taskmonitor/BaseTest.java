package com.aotain.taskmonitor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/01
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:spring-task-monitor.xml"})
public class BaseTest {

    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        applicationContext = new ClassPathXmlApplicationContext("classpath*:spring-task-monitor.xml");
    }

    @PreDestroy
    public void destroy(){
        AbstractApplicationContext abstractApplicationContext = (AbstractApplicationContext)applicationContext;
        abstractApplicationContext.close();
    }

    @Test
    public void test(){

    }
}
