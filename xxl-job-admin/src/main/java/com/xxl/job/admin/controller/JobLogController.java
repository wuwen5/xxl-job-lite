package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.interceptor.PermissionInterceptor;
import com.xxl.job.admin.core.complete.XxlJobCompleter;
import com.xxl.job.admin.core.exception.XxlJobException;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobLogDao;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.KillParam;
import com.xxl.job.core.biz.model.LogParam;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.DateUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

/**
 * index controller
 * @author xuxueli 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/joblog")
@Slf4j
public class JobLogController {

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;

    @Resource
    private XxlJobInfoDao xxlJobInfoDao;

    @Resource
    private XxlJobLogDao xxlJobLogDao;

    @GetMapping
    public String index(
            HttpServletRequest request,
            Model model,
            @RequestParam(required = false, defaultValue = "0") Integer jobId) {

        // 执行器列表
        List<XxlJobGroup> jobGroupListAll = xxlJobGroupDao.findAll();

        // filter group
        List<XxlJobGroup> jobGroupList = PermissionInterceptor.filterJobGroupByRole(request, jobGroupListAll);
        if (jobGroupList == null || jobGroupList.isEmpty()) {
            throw new XxlJobException(I18nUtil.getString("jobgroup_empty"));
        }

        model.addAttribute("JobGroupList", jobGroupList);

        // 任务
        if (jobId > 0) {
            XxlJobInfo jobInfo = xxlJobInfoDao.loadById(jobId);
            if (jobInfo == null) {
                throw new RuntimeException(
                        I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_unvalid"));
            }

            model.addAttribute("jobInfo", jobInfo);

            // valid permission
            PermissionInterceptor.validJobGroupPermission(request, jobInfo.getJobGroup());
        }

        return "joblog/joblog.index";
    }

    @GetMapping("/getJobsByGroup/{id}")
    @ResponseBody
    public ReturnT<List<XxlJobInfo>> getJobsByGroup(@PathVariable int id) {
        List<XxlJobInfo> list = xxlJobInfoDao.getJobsByGroup(id);
        return new ReturnT<>(list);
    }

    @PostMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "0") int start,
            @RequestParam(required = false, defaultValue = "10") int length,
            int jobGroup,
            int jobId,
            int logStatus,
            String filterTime) {

        // valid permission
        // 仅管理员支持查询全部；普通用户仅支持查询有权限的 jobGroup
        PermissionInterceptor.validJobGroupPermission(request, jobGroup);

        // parse param
        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if (filterTime != null && !filterTime.trim().isEmpty()) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                triggerTimeStart = DateUtil.parseDateTime(temp[0]);
                triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
            }
        }

        // page query
        List<XxlJobLog> list =
                xxlJobLogDao.pageList(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
        int listCount =
                xxlJobLogDao.pageListCount(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);

        // package result
        Map<String, Object> maps = new HashMap<>(2);
        // 总记录数
        maps.put("recordsTotal", listCount);
        // 过滤后的总记录数
        maps.put("recordsFiltered", listCount);
        // 分页列表
        maps.put("data", list);
        return maps;
    }

    @GetMapping("/logDetailPage/{id}")
    public String logDetailPage(@PathVariable int id, Model model) {

        // base check
        XxlJobLog jobLog = xxlJobLogDao.load(id);
        if (jobLog == null) {
            throw new RuntimeException(I18nUtil.getString("joblog_logid_unvalid"));
        }

        model.addAttribute("triggerCode", jobLog.getTriggerCode());
        model.addAttribute("handleCode", jobLog.getHandleCode());
        model.addAttribute("logId", jobLog.getId());
        return "joblog/joblog.detail";
    }

    @GetMapping("/logDetailCat/{logId}")
    @ResponseBody
    public ReturnT<LogResult> logDetailCat(@PathVariable long logId, int fromLineNum) {
        try {
            // valid
            // todo, need to improve performance
            XxlJobLog jobLog = xxlJobLogDao.load(logId);
            if (jobLog == null) {
                return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("joblog_logid_unvalid"));
            }

            // log cat
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(jobLog.getExecutorAddress());
            ReturnT<LogResult> logResult =
                    executorBiz.log(new LogParam(jobLog.getTriggerTime().getTime(), logId, fromLineNum));

            // is end
            if (logResult.getContent() != null
                    && logResult.getContent().getFromLineNum()
                            > logResult.getContent().getToLineNum()) {
                if (jobLog.getHandleCode() > 0) {
                    logResult.getContent().setEnd(true);
                }
            }

            // fix xss
            if (logResult.getContent() != null
                    && StringUtils.hasText(logResult.getContent().getLogContent())) {
                String newLogContent = logResult.getContent().getLogContent();
                newLogContent = HtmlUtils.htmlEscape(newLogContent, "UTF-8");
                logResult.getContent().setLogContent(newLogContent);
            }

            return logResult;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, e.getMessage());
        }
    }

    @PostMapping("/logKill")
    @ResponseBody
    public ReturnT<String> logKill(int id) {
        // base check
        XxlJobLog jobLog = xxlJobLogDao.load(id);
        XxlJobInfo jobInfo = xxlJobInfoDao.loadById(jobLog.getJobId());
        if (jobInfo == null) {
            return new ReturnT<>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        if (ReturnT.SUCCESS_CODE != jobLog.getTriggerCode()) {
            return new ReturnT<>(500, I18nUtil.getString("joblog_kill_log_limit"));
        }

        // request of kill
        ReturnT<String> runResult;
        try {
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(jobLog.getExecutorAddress());
            runResult = executorBiz.kill(new KillParam(jobInfo.getId()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            runResult = new ReturnT<>(500, e.getMessage());
        }

        if (ReturnT.SUCCESS_CODE == runResult.getCode()) {
            jobLog.setHandleCode(ReturnT.FAIL_CODE);
            jobLog.setHandleMsg(I18nUtil.getString("joblog_kill_log_byman") + ":"
                    + (runResult.getMsg() != null ? runResult.getMsg() : ""));
            jobLog.setHandleTime(new Date());
            XxlJobCompleter.updateHandleInfoAndFinish(jobLog);
            return new ReturnT<>(runResult.getMsg());
        } else {
            return new ReturnT<>(500, runResult.getMsg());
        }
    }

    @PostMapping("/clearLog")
    @ResponseBody
    public ReturnT<String> clearLog(HttpServletRequest request, int jobGroup, int jobId, int type) {
        // valid permission
        PermissionInterceptor.validJobGroupPermission(request, jobGroup);

        // opt
        Date clearBeforeTime = null;

        // 0 : 清理所有
        int clearBeforeNum = 0;

        if (type < 1 || type > 9) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("joblog_clean_type_unvalid"));
        }

        if (type == 1) {
            // 清理一个月之前日志数据
            clearBeforeTime = Date.from(LocalDateTime.now(DEFAULT_ZONE)
                    .minusMonths(1)
                    .atZone(DEFAULT_ZONE)
                    .toInstant());
        } else if (type == 2) {
            // 清理三个月之前日志数据
            clearBeforeTime = Date.from(LocalDateTime.now(DEFAULT_ZONE)
                    .minusMonths(3)
                    .atZone(DEFAULT_ZONE)
                    .toInstant());
        } else if (type == 3) {
            // 清理六个月之前日志数据
            clearBeforeTime = Date.from(LocalDateTime.now(DEFAULT_ZONE)
                    .minusMonths(6)
                    .atZone(DEFAULT_ZONE)
                    .toInstant());
        } else if (type == 4) {
            // 清理一年之前日志数据
            clearBeforeTime = Date.from(LocalDateTime.now(DEFAULT_ZONE)
                    .minusYears(1)
                    .atZone(DEFAULT_ZONE)
                    .toInstant());
        } else if (type == 5) {
            // 清理一千条以前日志数据
            clearBeforeNum = 1000;
        } else if (type == 6) {
            // 清理一万条以前日志数据
            clearBeforeNum = 10000;
        } else if (type == 7) {
            // 清理三万条以前日志数据
            clearBeforeNum = 30000;
        } else if (type == 8) {
            // 清理十万条以前日志数据
            clearBeforeNum = 100000;
        }

        List<Long> logIds;
        do {
            logIds = xxlJobLogDao.findClearLogIds(jobGroup, jobId, clearBeforeTime, clearBeforeNum, 1000);
            if (logIds != null && !logIds.isEmpty()) {
                xxlJobLogDao.clearLog(logIds);
            }
        } while (logIds != null && !logIds.isEmpty());

        return ReturnT.SUCCESS;
    }
}
