package com.xxl.job.core.biz.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author wuwen
 */
@Getter
@Setter
public class JobExecutorParam implements Serializable {
    
    private static final long serialVersionUID = 42L;

    public JobExecutorParam() {
    }
    
    public JobExecutorParam(String appName, String title) {
        this.appName = appName;
        this.title = title;
    }
    
    /**
     * 执行器appname
     */
    private String appName;

    /**
     * 显示名称
     */
    private String title;
}
