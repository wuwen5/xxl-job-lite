package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobLog;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * job log
 * @author xuxueli 2016-1-12 18:03:06
 */
@Mapper
public interface XxlJobLogDao {

    /**
     * Retrieves a paginated list of job logs.
     * <p>
     * Filtering precedence: when {@code jobId > 0} the query filters by {@code jobId} alone and
     * {@code jobGroup} is ignored; when {@code jobId == 0} and {@code jobGroup > 0} the query
     * filters by {@code jobGroup} instead.
     * </p>
     * @param offset starting offset for pagination
     * @param pagesize page size for pagination
     * @param jobGroup job group ID (used only when {@code jobId == 0} and {@code jobGroup > 0})
     * @param jobId job ID; use {@code 0} as a sentinel to fall back to {@code jobGroup} filtering
     * @param triggerTimeStart start trigger time (inclusive), or {@code null} to skip
     * @param triggerTimeEnd end trigger time (inclusive), or {@code null} to skip
     * @param logStatus log status filter
     * @return list of XxlJobLog
     */
    List<XxlJobLog> pageList(
            @Param("offset") int offset,
            @Param("pagesize") int pagesize,
            @Param("jobGroup") int jobGroup,
            @Param("jobId") int jobId,
            @Param("triggerTimeStart") Date triggerTimeStart,
            @Param("triggerTimeEnd") Date triggerTimeEnd,
            @Param("logStatus") int logStatus);

    /**
     * Counts the total number of job logs matching criteria.
     * <p>
     * Note: {@code offset} and {@code pagesize} are not applied by the underlying SQL — they are
     * accepted only to keep this method's signature consistent with {@link #pageList}. The same
     * {@code jobId}/{@code jobGroup} precedence rules as {@link #pageList} apply here.
     * </p>
     * @param offset ignored — kept for signature consistency with {@link #pageList}
     * @param pagesize ignored — kept for signature consistency with {@link #pageList}
     * @param jobGroup job group ID (used only when {@code jobId == 0} and {@code jobGroup > 0})
     * @param jobId job ID; use {@code 0} as a sentinel to fall back to {@code jobGroup} filtering
     * @param triggerTimeStart start trigger time (inclusive), or {@code null} to skip
     * @param triggerTimeEnd end trigger time (inclusive), or {@code null} to skip
     * @param logStatus log status filter
     * @return count of logs
     */
    int pageListCount(
            @Param("offset") int offset,
            @Param("pagesize") int pagesize,
            @Param("jobGroup") int jobGroup,
            @Param("jobId") int jobId,
            @Param("triggerTimeStart") Date triggerTimeStart,
            @Param("triggerTimeEnd") Date triggerTimeEnd,
            @Param("logStatus") int logStatus);

    /**
     * Loads a job log by ID.
     * @param id log ID
     * @return XxlJobLog object
     */
    XxlJobLog load(@Param("id") long id);

    /**
     * Saves a job log.
     * @param xxlJobLog job log to save
     * @return generated ID
     */
    long save(XxlJobLog xxlJobLog);

    /**
     * Updates trigger info of a job log.
     * @param xxlJobLog job log with updated trigger info
     * @return number of affected rows
     */
    int updateTriggerInfo(XxlJobLog xxlJobLog);

    /**
     * Updates handle info of a job log.
     * @param xxlJobLog job log with updated handle info
     * @return number of affected rows
     */
    int updateHandleInfo(XxlJobLog xxlJobLog);

    /**
     * Deletes job logs by job ID.
     * @param jobId job ID
     * @return number of deleted logs
     */
    int delete(@Param("jobId") int jobId);

    /**
     * Finds log report data between dates.
     * @param from start date
     * @param to end date
     * @return map of report data
     */
    Map<String, Object> findLogReport(@Param("from") Date from, @Param("to") Date to);

    /**
     * Finds IDs of logs to clear.
     * @param jobGroup job group ID
     * @param jobId job ID
     * @param clearBeforeTime clear before this time
     * @param clearBeforeNum clear before this number
     * @param pagesize page size
     * @return list of log IDs
     */
    List<Long> findClearLogIds(
            @Param("jobGroup") int jobGroup,
            @Param("jobId") int jobId,
            @Param("clearBeforeTime") Date clearBeforeTime,
            @Param("clearBeforeNum") int clearBeforeNum,
            @Param("pagesize") int pagesize);

    /**
     * Clears logs by IDs.
     * @param logIds list of log IDs to clear
     * @return number of cleared logs
     */
    int clearLog(@Param("logIds") List<Long> logIds);

    /**
     * Finds IDs of failed job logs.
     * @param pagesize page size
     * @return list of failed log IDs
     */
    List<Long> findFailJobLogIds(@Param("pagesize") int pagesize);

    /**
     * Updates alarm status of a log.
     * @param logId log ID
     * @param oldAlarmStatus old alarm status
     * @param newAlarmStatus new alarm status
     * @return number of affected rows
     */
    int updateAlarmStatus(
            @Param("logId") long logId,
            @Param("oldAlarmStatus") int oldAlarmStatus,
            @Param("newAlarmStatus") int newAlarmStatus);

    /**
     * Finds lost job IDs before a time.
     * @param losedTime lost time threshold
     * @return list of lost job IDs
     */
    List<Long> findLostJobIds(@Param("losedTime") Date losedTime);
}
