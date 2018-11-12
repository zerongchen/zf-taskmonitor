package com.aotain.taskmonitor.task;

import com.aotain.common.utils.model.msg.RedisTaskStatus;

/**
 * 任务重做接口
 *
 * @author daiyh@aotain.com
 * @date 2018/02/05
 */
public interface IRedoTask {
    /**
     * 重新执行某个任务
     * @param taskStatus 任务hash信息
     * @return  成功创建重做任务,返回true，失败返回false
     */
    boolean redoTask(RedisTaskStatus taskStatus);
}
