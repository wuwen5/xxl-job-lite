package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobUser;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * job user
 * @author xuxueli 2019-05-04 16:44:59
 */
@Mapper
public interface XxlJobUserDao {

    /**
     * Retrieves a paginated list of job users.
     * @param offset starting offset
     * @param pagesize page size
     * @param username username filter
     * @param role role filter
     * @return list of XxlJobUser
     */
    List<XxlJobUser> pageList(
            @Param("offset") int offset,
            @Param("pagesize") int pagesize,
            @Param("username") String username,
            @Param("role") int role);

    /**
     * Counts the total number of job users matching criteria.
     * <p>
     * Note: {@code offset} and {@code pagesize} are not applied by the underlying SQL — they are
     * accepted only to keep this method's signature consistent with {@link #pageList}.
     * </p>
     * @param offset ignored — kept for signature consistency with {@link #pageList}
     * @param pagesize ignored — kept for signature consistency with {@link #pageList}
     * @param username username filter
     * @param role role filter
     * @return count of users
     */
    int pageListCount(
            @Param("offset") int offset,
            @Param("pagesize") int pagesize,
            @Param("username") String username,
            @Param("role") int role);

    /**
     * Loads a job user by username.
     * @param username username
     * @return XxlJobUser object
     */
    XxlJobUser loadByUserName(@Param("username") String username);

    /**
     * Saves a job user.
     * @param xxlJobUser user to save
     * @return number of affected rows
     */
    int save(XxlJobUser xxlJobUser);

    /**
     * Updates a job user.
     * @param xxlJobUser user to update
     * @return number of affected rows
     */
    int update(XxlJobUser xxlJobUser);

    /**
     * Deletes a job user by ID.
     * @param id user ID
     * @return number of deleted rows
     */
    int delete(@Param("id") int id);
}
