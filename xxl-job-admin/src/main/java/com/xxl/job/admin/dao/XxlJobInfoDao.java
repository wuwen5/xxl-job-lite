package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobInfo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Data Access Object for XxlJobInfo entities.
 * Provides methods to perform CRUD operations and queries on job information.
 * @author xuxueli 2016-1-12 18:03:45
 */
@Mapper
public interface XxlJobInfoDao {

    /**
     * Retrieves a paginated list of job infos, filtered by job group, trigger status, job description, executor handler, and author.
     *
     * @param offset the offset for pagination
     * @param pagesize the number of items per page
     * @param jobGroup the job group ID to filter by
     * @param triggerStatus the trigger status to filter by
     * @param jobDesc the job description to filter by (optional)
     * @param executorHandler the executor handler to filter by (optional)
     * @param author the author to filter by (optional)
     * @return a list of XxlJobInfo entities for the specified page
     */
    List<XxlJobInfo> pageList(
            @Param("offset") int offset,
            @Param("pagesize") int pagesize,
            @Param("jobGroup") int jobGroup,
            @Param("triggerStatus") int triggerStatus,
            @Param("jobDesc") String jobDesc,
            @Param("executorHandler") String executorHandler,
            @Param("author") String author);

    /**
     * Counts the total number of job infos matching the filter criteria for pagination.
     *
     * @param jobGroup the job group ID to filter by
     * @param triggerStatus the trigger status to filter by
     * @param jobDesc the job description to filter by (optional)
     * @param executorHandler the executor handler to filter by (optional)
     * @param author the author to filter by (optional)
     * @return the total count of matching job infos
     */
    int pageListCount(
            @Param("jobGroup") int jobGroup,
            @Param("triggerStatus") int triggerStatus,
            @Param("jobDesc") String jobDesc,
            @Param("executorHandler") String executorHandler,
            @Param("author") String author);

    /**
     * Saves a new job info.
     *
     * @param info the job info to save
     * @return the number of rows affected
     */
    int save(XxlJobInfo info);

    /**
     * Loads a job info by its ID.
     *
     * @param id the ID of the job info to load
     * @return the XxlJobInfo entity, or null if not found
     */
    XxlJobInfo loadById(@Param("id") int id);

    /**
     * Updates an existing job info.
     *
     * @param xxlJobInfo the job info to update
     * @return the number of rows affected
     */
    int update(XxlJobInfo xxlJobInfo);

    /**
     * Deletes a job info by its ID.
     *
     * @param id the ID of the job info to delete
     * @return the number of rows affected
     */
    int delete(@Param("id") long id);

    /**
     * Retrieves all job infos belonging to a specific job group.
     *
     * @param jobGroup the job group ID
     * @return a list of XxlJobInfo entities in the specified job group
     */
    List<XxlJobInfo> getJobsByGroup(@Param("jobGroup") int jobGroup);

    /**
     * Finds the total count of all job infos.
     *
     * @return the total number of job infos
     */
    int findAllCount();

    /**
     * find schedule job, limit "trigger_status = 1"
     *
     * @param maxNextTime the maximum next trigger time to filter by
     * @param pagesize the number of items to retrieve
     * @return a list of XxlJobInfo entities that are scheduled to be triggered before the specified maxNextTime
     */
    List<XxlJobInfo> scheduleJobQuery(@Param("maxNextTime") long maxNextTime, @Param("pagesize") int pagesize);

    /**
     * update schedule job
     * 	1、can only update "trigger_status = 1", Avoid stopping tasks from being opened
     * 	2、valid "triggerStatus gte 0", filter illegal state
     *
     * @param xxlJobInfo the job info to update, must include the ID and the new trigger status
     * @return the number of rows affected, should be 1 if the update was successful, or 0 if the job info was not in a valid state for updating
     */
    int scheduleUpdate(XxlJobInfo xxlJobInfo);
}
