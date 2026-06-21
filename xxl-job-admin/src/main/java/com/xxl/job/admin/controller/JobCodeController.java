package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.interceptor.PermissionInterceptor;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLogGlue;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobLogGlueDao;
import com.xxl.job.core.biz.model.ReturnT;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * job code controller
 * @author xuxueli 2015-12-19 16:13:16
 */
@RestController
@RequestMapping("/admin-api/v1/jobcode")
public class JobCodeController {

    @Resource
    private XxlJobInfoDao xxlJobInfoDao;

    @Resource
    private XxlJobLogGlueDao xxlJobLogGlueDao;

    @GetMapping("/{id}/history")
    public ReturnT<List<XxlJobLogGlue>> history(@PathVariable int id) {
        XxlJobInfo jobInfo = xxlJobInfoDao.loadById(id);
        if (jobInfo == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        List<XxlJobLogGlue> jobLogGlues = xxlJobLogGlueDao.findByJobId(id);
        return new ReturnT<>(jobLogGlues);
    }

    @PutMapping("/{id}")
    public ReturnT<String> save(
            HttpServletRequest request, @PathVariable int id, @RequestBody Map<String, String> body) {
        String glueSource = body.get("glueSource");
        String glueRemark = body.get("glueRemark");
        // valid
        if (glueRemark == null) {
            return new ReturnT<>(
                    500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_glue_remark")));
        }
        if (glueRemark.length() < 4 || glueRemark.length() > 100) {
            return new ReturnT<>(500, I18nUtil.getString("jobinfo_glue_remark_limit"));
        }
        XxlJobInfo existsJobInfo = xxlJobInfoDao.loadById(id);
        if (existsJobInfo == null) {
            return new ReturnT<>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }

        // valid permission
        PermissionInterceptor.validJobGroupPermission(request, existsJobInfo.getJobGroup());

        // update new code
        existsJobInfo.setGlueSource(glueSource);
        existsJobInfo.setGlueRemark(glueRemark);
        existsJobInfo.setGlueUpdatetime(new Date());

        existsJobInfo.setUpdateTime(new Date());
        xxlJobInfoDao.update(existsJobInfo);

        // log old code
        XxlJobLogGlue xxlJobLogGlue = new XxlJobLogGlue();
        xxlJobLogGlue.setJobId(existsJobInfo.getId());
        xxlJobLogGlue.setGlueType(existsJobInfo.getGlueType());
        xxlJobLogGlue.setGlueSource(glueSource);
        xxlJobLogGlue.setGlueRemark(glueRemark);

        xxlJobLogGlue.setAddTime(new Date());
        xxlJobLogGlue.setUpdateTime(new Date());
        xxlJobLogGlueDao.save(xxlJobLogGlue);

        // remove code backup more than 30
        xxlJobLogGlueDao.removeOld(existsJobInfo.getId(), 30);

        return ReturnT.SUCCESS;
    }
}
