package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.interceptor.PermissionInterceptor;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.thread.JobScheduleHelper;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.DateUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
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
    private XxlJobService xxlJobService;

    @PostMapping("/pageList")
    public Map<String, Object> pageList(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "0") int start,
            @RequestParam(required = false, defaultValue = "10") int length,
            Integer jobGroup,
            Integer triggerStatus,
            String jobDesc,
            String executorHandler,
            String author) {

        PermissionInterceptor.validJobGroupPermission(request, jobGroup);
        return xxlJobService.pageList(
                start,
                length,
                jobGroup,
                triggerStatus == null ? -1 : triggerStatus,
                jobDesc,
                executorHandler,
                author);
    }

    @PostMapping
    public ReturnT<String> add(HttpServletRequest request, @RequestBody XxlJobInfo jobInfo) {
        // valid permission
        PermissionInterceptor.validJobGroupPermission(request, jobInfo.getJobGroup());

        // opt
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
        return xxlJobService.add(jobInfo, loginUser);
    }

    @PutMapping("/{id}")
    public ReturnT<String> update(@PathVariable int id, HttpServletRequest request, @RequestBody XxlJobInfo jobInfo) {
        jobInfo.setId(id);
        // valid permission
        PermissionInterceptor.validJobGroupPermission(request, jobInfo.getJobGroup());

        // opt
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
        return xxlJobService.update(jobInfo, loginUser);
    }

    @DeleteMapping("/{id}")
    public ReturnT<String> remove(@PathVariable int id) {
        return xxlJobService.remove(id);
    }

    @PutMapping("/stop/{id}")
    public ReturnT<String> pause(@PathVariable int id) {
        return xxlJobService.stop(id);
    }

    @PutMapping("/start/{id}")
    public ReturnT<String> start(@PathVariable int id) {
        return xxlJobService.start(id);
    }

    @PostMapping("/trigger")
    public ReturnT<String> triggerJob(HttpServletRequest request, int id, String executorParam, String addressList) {
        // login user
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
        // trigger
        return xxlJobService.trigger(loginUser, id, executorParam, addressList);
    }

    @GetMapping("/nextTriggerTime")
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
