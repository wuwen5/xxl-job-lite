package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobRegistry;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Created by xuxueli on 16/9/30.
 */
@Mapper
public interface XxlJobRegistryDao {

    /**
     * find dead registry id list
     * @param timeout timeout seconds, registry update time before now minus timeout is dead
     * @param nowTime current time
     * @return dead registry id list
     */
    List<Integer> findDead(@Param("timeout") int timeout, @Param("nowTime") Date nowTime);

    /**
     * delete dead registry
     * @param ids dead registry id list
     * @return delete count
     */
    int removeDead(@Param("ids") List<Integer> ids);

    /**
     * find alive registry list
     * @param timeout timeout seconds, registry update time before now minus timeout is dead
     * @param nowTime current time
     * @return alive registry list
     */
    List<XxlJobRegistry> findAll(@Param("timeout") int timeout, @Param("nowTime") Date nowTime);

    /**
     * registry update
     * @param registryGroup registry group
     * @param registryKey registry key
     * @param registryValue registry value
     * @param updateTime update time
     * @return update count
     */
    int registryUpdate(
            @Param("registryGroup") String registryGroup,
            @Param("registryKey") String registryKey,
            @Param("registryValue") String registryValue,
            @Param("updateTime") Date updateTime);

    /**
     * registry save
     * @param registryGroup registry group
     * @param registryKey registry key
     * @param registryValue registry value
     * @param updateTime update time
     * @return insert count
     */
    int registrySave(
            @Param("registryGroup") String registryGroup,
            @Param("registryKey") String registryKey,
            @Param("registryValue") String registryValue,
            @Param("updateTime") Date updateTime);

    /**
     * registry delete
     * @param registryGroup registry group
     * @param registryKey registry key
     * @param registryValue registry value
     * @return delete count
     */
    int registryDelete(
            @Param("registryGroup") String registryGroup,
            @Param("registryKey") String registryKey,
            @Param("registryValue") String registryValue);
}
