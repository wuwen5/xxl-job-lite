package com.xxl.job.core.biz.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by xuxueli on 17/3/23.
 */
@Setter
@Getter
public class LogResult implements Serializable {
    private static final long serialVersionUID = 42L;

    public LogResult() {}

    public LogResult(int fromLineNum, int toLineNum, String logContent, boolean isEnd) {
        this.fromLineNum = fromLineNum;
        this.toLineNum = toLineNum;
        this.logContent = logContent;
        this.isEnd = isEnd;
    }

    private int fromLineNum;
    private int toLineNum;
    private String logContent;
    private boolean isEnd;
}
