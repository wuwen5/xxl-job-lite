package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobLogGlue;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * job log for glue
 * @author xuxueli 2016-5-19 18:04:56
 */
@Mapper
public interface XxlJobLogGlueDao {

    /**
     * Saves a job log glue.
     * @param xxlJobLogGlue job log glue to save
     * @return number of affected rows
     */
    int save(XxlJobLogGlue xxlJobLogGlue);

    /**
     * Finds job log glues by job ID.
     * @param jobId job ID
     * @return list of XxlJobLogGlue
     */
    List<XxlJobLogGlue> findByJobId(@Param("jobId") int jobId);

    /**
     * Removes old job log glues.
     * @param jobId job ID
     * @param limit limit number to keep
     * @return number of removed rows
     */
    int removeOld(@Param("jobId") int jobId, @Param("limit") int limit);

    /**
     * Deletes job log glues by job ID.
     * @param jobId job ID
     * @return number of deleted rows
     */
    int deleteByJobId(@Param("jobId") int jobId);
}
