package com.aotain.taskmonitor;

import com.aotain.taskmonitor.scheduledtask.DpiServerStatusTask;
import com.aotain.taskmonitor.scheduledtask.DpiStatusTask;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/06
 */
public class ScheduleTaskTest extends BaseTest{

    @Autowired
    private DpiStatusTask dpiStatusTask;

    @Autowired
    private DpiServerStatusTask dpiServerStatusTask;

    @Test
    public void test(){
        dpiStatusTask.putDynamicDpiInfoToDB();
    }

    @Test
    public void test2(){
        dpiStatusTask.putStaticDpiInfoToDB();
    }

    @Test
    public void test3(){
        dpiServerStatusTask.writeServerStatusFile();
    }

}
