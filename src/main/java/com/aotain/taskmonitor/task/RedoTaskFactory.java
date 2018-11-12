package com.aotain.taskmonitor.task;

import com.aotain.common.utils.tools.CommonConstant;
import com.aotain.taskmonitor.task.impl.DpiPolicyRedoTask;
import com.aotain.taskmonitor.task.impl.JobRedoTask;
import com.aotain.taskmonitor.task.impl.SmmsAckRedoTask;

/**
 * Demo class
 *
 * @author daiyh@aotain.com
 * @date 2018/02/05
 */
public class RedoTaskFactory {

    /**
     * 根据任务类型构造对应的重做实现接口
     * @param type
     * @return 如果type不支持，则返回null
     */
    public static IRedoTask createRedoTask(int type) {
        IRedoTask iRedoTask = null;
        switch (type) {
            case CommonConstant.REDIS_TASK_TYPE_JOBTASK:
                iRedoTask = new JobRedoTask();
                break;
            case CommonConstant.REDIS_TASK_TYPE_SMMSACK:
                iRedoTask = new SmmsAckRedoTask();
                break;
            case CommonConstant.REDIS_TASK_TYPE_FILEUPLOAD:
//                iRedoTask = new FileUploadRedoTask();
                break;
            case CommonConstant.REDIS_TASK_TYPE_DPI_POLICY:
                iRedoTask = new DpiPolicyRedoTask();
                break;
            case CommonConstant.REDIS_TASK_TYPE_AZKABAN:
//                iRedoTask = new AzkabanRedoTask();
                break;
        }
        return iRedoTask;
    }
}
