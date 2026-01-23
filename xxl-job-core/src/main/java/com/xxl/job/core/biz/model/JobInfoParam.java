package com.xxl.job.core.biz.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 *
 * @author wuwen
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class JobInfoParam implements Serializable {
    
    private static final long serialVersionUID = 42L;

    /**
     * 执行器AppName
     */
    private String appName;

    /**
     * 任务描述
     */
    private String jobDesc;
    
    /**
     * 任务JobHandler名称
     */
    private String executorHandler;
    
    /**
     * 执行器，任务参数
     */
    private String executorParam;
    
    private String cron;
    
    private long fixedRate;
}
