package com.xxl.job.admin.core.model;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * xxl-job log, used to track trigger process
 * @author xuxueli  2015-12-19 23:19:09
 */
@Setter
@Getter
@Accessors(chain = true)
public class XxlJobLog {

    private long id;

    private int jobGroup;
    private int jobId;
    private String jobDesc;

    private String executorAddress;
    private String executorHandler;
    private String executorParam;
    private String executorShardingParam;
    private int executorFailRetryCount;

    private Date triggerTime;
    private int triggerCode;
    private String triggerMsg;

    private Date handleTime;
    private int handleCode;
    private String handleMsg;

    private int alarmStatus;
}
