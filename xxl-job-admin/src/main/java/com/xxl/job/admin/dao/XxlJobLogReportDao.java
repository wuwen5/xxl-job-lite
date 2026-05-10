package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobLogReport;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * job log
 * @author xuxueli 2019-11-22
 */
@Mapper
public interface XxlJobLogReportDao {

    /**
     * Saves a job log report.
     * @param xxlJobLogReport report to save
     * @return number of affected rows
     */
    int save(XxlJobLogReport xxlJobLogReport);

    /**
     * Updates a job log report.
     * @param xxlJobLogReport report to update
     * @return number of affected rows
     */
    int update(XxlJobLogReport xxlJobLogReport);

    /**
     * Queries log reports within date range.
     * @param triggerDayFrom start date
     * @param triggerDayTo end date
     * @return list of XxlJobLogReport
     */
    List<XxlJobLogReport> queryLogReport(
            @Param("triggerDayFrom") Date triggerDayFrom, @Param("triggerDayTo") Date triggerDayTo);

    /**
     * Queries total log report.
     * @return XxlJobLogReport total
     */
    XxlJobLogReport queryLogReportTotal();
}
