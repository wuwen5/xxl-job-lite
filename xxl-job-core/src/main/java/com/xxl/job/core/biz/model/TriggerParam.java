package com.xxl.job.core.biz.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Created by xuxueli on 16/7/22.
 */
@Setter
@Getter
@Accessors(chain = true)
public class TriggerParam implements Serializable {
    private static final long serialVersionUID = 42L;

    private int jobId;

    private String executorHandler;
    private String executorParams;
    private String executorBlockStrategy;
    private int executorTimeout;

    private long logId;
    private long logDateTime;

    private String glueType;
    private String glueSource;
    private long glueUpdatetime;

    private int broadcastIndex;
    private int broadcastTotal;

    @Override
    public String toString() {
        return "TriggerParam{" + "jobId="
                + jobId + ", executorHandler='"
                + executorHandler + '\'' + ", executorParams='"
                + executorParams + '\'' + ", executorBlockStrategy='"
                + executorBlockStrategy + '\'' + ", executorTimeout="
                + executorTimeout + ", logId="
                + logId + ", logDateTime="
                + logDateTime + ", glueType='"
                + glueType + '\'' + ", glueSource='"
                + glueSource + '\'' + ", glueUpdatetime="
                + glueUpdatetime + ", broadcastIndex="
                + broadcastIndex + ", broadcastTotal="
                + broadcastTotal + '}';
    }
}
