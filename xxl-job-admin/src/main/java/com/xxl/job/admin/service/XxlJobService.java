package com.xxl.job.admin.service;

import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.core.biz.model.ReturnT;
import java.util.Date;
import java.util.Map;

/**
 * core job action for xxl-job
 *
 * @author xuxueli 2016-5-28 15:30:33
 */
public interface XxlJobService {

    /**
     * page list
     *
     * @param start
     * @param length
     * @param jobGroup
     * @param jobDesc
     * @param executorHandler
     * @param author
     * @return
     */
    Map<String, Object> pageList(
            int start,
            int length,
            int jobGroup,
            int triggerStatus,
            String jobDesc,
            String executorHandler,
            String author);

    /**
     * add job
     *
     * @param jobInfo
     * @return
     */
    ReturnT<String> add(XxlJobInfo jobInfo, XxlJobUser loginUser);

    /**
     * update job
     *
     * @param jobInfo
     * @return
     */
    ReturnT<String> update(XxlJobInfo jobInfo, XxlJobUser loginUser);

    /**
     * remove job
     * 	 *
     * @param id
     * @return
     */
    ReturnT<String> remove(int id);

    /**
     * start job
     *
     * @param id
     * @return
     */
    ReturnT<String> start(int id);

    /**
     * stop job
     *
     * @param id
     * @return
     */
    ReturnT<String> stop(int id);

    /**
     * trigger
     *
     * @param loginUser
     * @param jobId
     * @param executorParam
     * @param addressList
     * @return
     */
    ReturnT<String> trigger(XxlJobUser loginUser, int jobId, String executorParam, String addressList);

    /**
     * dashboard info
     *
     * @return
     */
    Map<String, Object> dashboardInfo();

    /**
     * chart info
     *
     * @param startDate
     * @param endDate
     * @return
     */
    ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate);
}
