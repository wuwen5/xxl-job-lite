package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobGroup;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Data Access Object for XxlJobGroup entities.
 * Provides methods to perform CRUD operations and queries on job groups.
 * Created by xuxueli on 16/9/30.
 */
@Mapper
public interface XxlJobGroupDao {

    /**
     * Finds all job groups.
     *
     * @return a list of all XxlJobGroup entities
     */
    List<XxlJobGroup> findAll();

    /**
     * Finds job groups by address type.
     *
     * @param addressType the address type to filter by
     * @return a list of XxlJobGroup entities matching the address type
     */
    List<XxlJobGroup> findByAddressType(@Param("addressType") int addressType);

    /**
     * Saves a new job group.
     *
     * @param xxlJobGroup the job group to save
     * @return the number of rows affected
     */
    int save(XxlJobGroup xxlJobGroup);

    /**
     * Updates an existing job group.
     *
     * @param xxlJobGroup the job group to update
     * @return the number of rows affected
     */
    int update(XxlJobGroup xxlJobGroup);

    /**
     * Removes a job group by its ID.
     *
     * @param id the ID of the job group to remove
     * @return the number of rows affected
     */
    int remove(@Param("id") int id);

    /**
     * Loads a job group by its ID.
     *
     * @param id the ID of the job group to load
     * @return the XxlJobGroup entity, or null if not found
     */
    XxlJobGroup load(@Param("id") int id);

    /**
     * Retrieves a paginated list of job groups, optionally filtered by appname and title.
     *
     * @param offset the offset for pagination
     * @param pagesize the number of items per page
     * @param appname the appname to filter by (optional)
     * @param title the title to filter by (optional)
     * @return a list of XxlJobGroup entities for the specified page
     */
    List<XxlJobGroup> pageList(
            @Param("offset") int offset,
            @Param("pagesize") int pagesize,
            @Param("appname") String appname,
            @Param("title") String title);

    /**
     * Counts the total number of job groups matching the filter criteria for pagination.
     *
     * @param offset the offset (not used in count, but included for consistency)
     * @param pagesize the pagesize (not used in count, but included for consistency)
     * @param appname the appname to filter by (optional)
     * @param title the title to filter by (optional)
     * @return the total count of matching job groups
     */
    int pageListCount(
            @Param("offset") int offset,
            @Param("pagesize") int pagesize,
            @Param("appname") String appname,
            @Param("title") String title);
}
