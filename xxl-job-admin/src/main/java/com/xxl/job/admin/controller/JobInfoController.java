package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.interceptor.PermissionInterceptor;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.thread.JobScheduleHelper;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.DateUtil;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * index controller
 * @author xuxueli 2015-12-19 16:13:16
 */
@RestController
@RequestMapping("/admin-api/v1/jobinfo")
@Slf4j
public class JobInfoController {

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;

    @Resource
    private XxlJobInfoDao xxlJobInfoDao;

    @Resource
    private XxlJobService xxlJobService;

    @GetMapping
    public Map<String, Object> pageList(
            @RequestParam(required = false, defaultValue = "0") int start,
            @RequestParam(required = false, defaultValue = "10") int length,
            int jobGroup,
            Integer triggerStatus,
            String jobDesc,
            String executorHandler,
            String author) {

        PermissionInterceptor.validJobGroupPermission(jobGroup);
        return xxlJobService.pageList(
                start, length, jobGroup, triggerStatus == null ? -1 : triggerStatus, jobDesc, executorHandler, author);
    }

    @PostMapping
    public ReturnT<String> add(@RequestBody XxlJobInfo jobInfo) {
        // valid permission
        PermissionInterceptor.validJobGroupPermission(jobInfo.getJobGroup());

        // opt
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser();
        return xxlJobService.add(jobInfo, loginUser);
    }

    @PutMapping("/{id}")
    public ReturnT<String> update(@PathVariable int id, @RequestBody XxlJobInfo jobInfo) {
        jobInfo.setId(id);
        // valid permission
        PermissionInterceptor.validJobGroupPermission(jobInfo.getJobGroup());

        // opt
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser();
        return xxlJobService.update(jobInfo, loginUser);
    }

    @DeleteMapping("/{id}")
    public ReturnT<String> remove(@PathVariable int id) {
        XxlJobInfo jobInfo = xxlJobInfoDao.loadById(id);
        if (jobInfo == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        PermissionInterceptor.validJobGroupPermission(jobInfo.getJobGroup());
        return xxlJobService.remove(id);
    }

    @PatchMapping("/{id}")
    public ReturnT<String> updateStatus(@PathVariable int id, @RequestParam String action) {
        XxlJobInfo jobInfo = xxlJobInfoDao.loadById(id);
        if (jobInfo == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        PermissionInterceptor.validJobGroupPermission(jobInfo.getJobGroup());
        if ("start".equalsIgnoreCase(action)) {
            return xxlJobService.start(id);
        } else if ("stop".equalsIgnoreCase(action)) {
            return xxlJobService.stop(id);
        }
        return new ReturnT<>(ReturnT.FAIL_CODE, "invalid action: " + action);
    }

    @PostMapping("/{id}/trigger")
    public ReturnT<String> triggerJob(@PathVariable int id, String executorParam, String addressList) {
        // login user
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser();
        // trigger
        return xxlJobService.trigger(loginUser, id, executorParam, addressList);
    }

    @GetMapping("/trigger-time/next")
    public ReturnT<List<String>> nextTriggerTime(String scheduleType, String scheduleConf) {

        XxlJobInfo paramXxlJobInfo = new XxlJobInfo();
        paramXxlJobInfo.setScheduleType(scheduleType);
        paramXxlJobInfo.setScheduleConf(scheduleConf);

        List<String> result = new ArrayList<>();
        try {
            Date lastTime = new Date();
            for (int i = 0; i < 5; i++) {
                lastTime = JobScheduleHelper.generateNextValidTime(paramXxlJobInfo, lastTime);
                if (lastTime != null) {
                    result.add(DateUtil.formatDateTime(lastTime));
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("nextTriggerTime error. scheduleType = {}, scheduleConf= {}", scheduleType, scheduleConf, e);
            return new ReturnT<>(
                    ReturnT.FAIL_CODE,
                    (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")) + e.getMessage());
        }
        return new ReturnT<>(result);
    }
}
