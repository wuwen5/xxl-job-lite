package com.xxl.job.core.executor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

/**
 * 执行器配置类
 * @author wuwen
 */
@Getter
@With
@AllArgsConstructor
@Builder
public class XxlJobExecutorConfig {

    /**
     * 调度中心；多个，逗号分隔
     */
    private String adminAddresses;

    /**
     * 执行器通讯TOKEN；非空字符串
     */
    private String accessToken;

    /**
     * 接口超时时间，单位秒；默认3秒
     */
    private int timeout;

    /**
     * 执行器AppName；非空字符串
     */
    private String appname;

    /**
     * 执行器显示名称
     */
    private String title;

    /**
     * 执行器地址；非空字符串
     */
    private String address;

    /**
     * 执行器IP；非空字符串
     */
    private String ip;

    /**
     * 执行器端口；默认9999
     */
    private int port;

    /**
     * 执行器日志存储路径；非空字符串
     */
    private String logPath;

    /**
     * 执行器日志保留天数
     */
    private int logRetentionDays;

    /**
     * 是否开启GLUE模式；默认true
     */
    private boolean glueEnabled;
}
