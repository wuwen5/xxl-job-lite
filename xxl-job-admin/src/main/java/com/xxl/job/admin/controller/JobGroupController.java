package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.controller.interceptor.PermissionInterceptor;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobRegistry;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobRegistryDao;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.RegistryConfig;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * job group controller
 * @author xuxueli 2016-10-02 20:52:56
 */
@RestController
@RequestMapping("/admin-api/v1/jobgroup")
public class JobGroupController {

    @Resource
    private XxlJobInfoDao xxlJobInfoDao;

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;

    @Resource
    private XxlJobRegistryDao xxlJobRegistryDao;

    @GetMapping
    @PermissionLimit(adminuser = true)
    public Map<String, Object> pageList(
            @RequestParam(required = false, defaultValue = "0") int start,
            @RequestParam(required = false, defaultValue = "10") int length,
            String appname,
            String title) {

        // page query
        appname = appname != null && !appname.trim().isEmpty() ? "%" + appname.trim() + "%" : null;
        title = title != null && !title.trim().isEmpty() ? "%" + title.trim() + "%" : null;
        List<XxlJobGroup> list = xxlJobGroupDao.pageList(start, length, appname, title);
        int listCount = xxlJobGroupDao.pageListCount(start, length, appname, title);

        // package result
        Map<String, Object> maps = new HashMap<>(3);
        // 总记录数
        maps.put("recordsTotal", listCount);
        // 过滤后的总记录数
        maps.put("recordsFiltered", listCount);
        // 分页列表
        maps.put("data", list);
        return maps;
    }

    @PostMapping
    @PermissionLimit(adminuser = true)
    public ReturnT<String> save(@RequestBody XxlJobGroup xxlJobGroup) {

        // valid
        Optional<String> failedMsg = validate(xxlJobGroup);
        if (failedMsg.isPresent()) {
            return new ReturnT<>(500, failedMsg.get());
        }

        // process
        xxlJobGroup.setUpdateTime(new Date());

        int ret = xxlJobGroupDao.save(xxlJobGroup);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @PutMapping("/{id}")
    @PermissionLimit(adminuser = true)
    public ReturnT<String> update(@PathVariable int id, @RequestBody XxlJobGroup xxlJobGroup) {
        // valid
        Optional<String> failedMsg = validate(xxlJobGroup);
        if (failedMsg.isPresent()) {
            return new ReturnT<>(500, failedMsg.get());
        }

        xxlJobGroup.setId(id);

        if (xxlJobGroup.getAddressType() == 0) {
            // 0=自动注册
            List<String> registryList = findRegistryByAppName(xxlJobGroup.getAppname());
            String addressListStr = null;
            if (registryList != null && !registryList.isEmpty()) {
                Collections.sort(registryList);
                addressListStr = String.join(",", registryList);
            }
            xxlJobGroup.setAddressList(addressListStr);
        }

        // process
        xxlJobGroup.setUpdateTime(new Date());

        int ret = xxlJobGroupDao.update(xxlJobGroup);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    private Optional<String> validate(XxlJobGroup xxlJobGroup) {

        if (!StringUtils.hasText(xxlJobGroup.getAppname())) {
            return Optional.of(I18nUtil.getString("system_please_input") + "AppName");
        }
        if (xxlJobGroup.getAppname().length() < 4 || xxlJobGroup.getAppname().length() > 64) {
            return Optional.of(I18nUtil.getString("jobgroup_field_appname_length"));
        }
        if (xxlJobGroup.getAppname().contains(">") || xxlJobGroup.getAppname().contains("<")) {
            return Optional.of("AppName" + I18nUtil.getString("system_unvalid"));
        }
        if (!StringUtils.hasText(xxlJobGroup.getTitle())) {
            return Optional.of(I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title"));
        }
        if (xxlJobGroup.getTitle().contains(">") || xxlJobGroup.getTitle().contains("<")) {
            return Optional.of(I18nUtil.getString("jobgroup_field_title") + I18nUtil.getString("system_unvalid"));
        }
        if (xxlJobGroup.getAddressType() != 0) {
            if (!StringUtils.hasText(xxlJobGroup.getAddressList())) {
                return Optional.of(I18nUtil.getString("jobgroup_field_addressType_limit"));
            }
            if (xxlJobGroup.getAddressList().contains(">")
                    || xxlJobGroup.getAddressList().contains("<")) {
                return Optional.of(
                        I18nUtil.getString("jobgroup_field_registryList") + I18nUtil.getString("system_unvalid"));
            }

            String[] addresses = xxlJobGroup.getAddressList().split(",");
            for (String item : addresses) {
                if (item == null || item.trim().isEmpty()) {
                    return Optional.of(I18nUtil.getString("jobgroup_field_registryList_unvalid"));
                }
            }
        }
        return Optional.empty();
    }

    private List<String> findRegistryByAppName(String appnameParam) {
        HashMap<String, List<String>> appAddressMap = new HashMap<>();
        List<XxlJobRegistry> list = xxlJobRegistryDao.findAll(RegistryConfig.DEAD_TIMEOUT, new Date());
        if (list != null) {
            for (XxlJobRegistry item : list) {
                if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                    String appname = item.getRegistryKey();
                    List<String> registryList = appAddressMap.get(appname);
                    if (registryList == null) {
                        registryList = new ArrayList<>();
                    }

                    if (!registryList.contains(item.getRegistryValue())) {
                        registryList.add(item.getRegistryValue());
                    }
                    appAddressMap.put(appname, registryList);
                }
            }
        }
        return appAddressMap.get(appnameParam);
    }

    @DeleteMapping("/{id}")
    @PermissionLimit(adminuser = true)
    public ReturnT<String> remove(@PathVariable int id) {

        // valid
        int count = xxlJobInfoDao.pageListCount(id, -1, null, null, null);
        if (count > 0) {
            return new ReturnT<>(500, I18nUtil.getString("jobgroup_del_limit_0"));
        }

        List<XxlJobGroup> allList = xxlJobGroupDao.findAll();
        if (allList.size() == 1) {
            return new ReturnT<>(500, I18nUtil.getString("jobgroup_del_limit_1"));
        }

        int ret = xxlJobGroupDao.remove(id);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @GetMapping("/{id}")
    @PermissionLimit(adminuser = true)
    public ReturnT<XxlJobGroup> loadById(@PathVariable int id) {
        XxlJobGroup jobGroup = xxlJobGroupDao.load(id);
        return jobGroup != null ? new ReturnT<>(jobGroup) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    @GetMapping("/all")
    public ReturnT<List<XxlJobGroup>> all() {
        // 执行器列表
        List<XxlJobGroup> list = xxlJobGroupDao.findAll();
        // filter group
        List<XxlJobGroup> jobGroupList = PermissionInterceptor.filterJobGroupByRole(list);
        return new ReturnT<>(jobGroupList);
    }
}
