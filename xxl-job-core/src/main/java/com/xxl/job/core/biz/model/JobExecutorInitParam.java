package com.xxl.job.core.biz.model;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author wuwen
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class JobExecutorInitParam implements Serializable {

    private static final long serialVersionUID = 42L;

    private JobExecutorParam jobExecutorParam;

    private List<JobInfoParam> jobInfoParamList;
}
