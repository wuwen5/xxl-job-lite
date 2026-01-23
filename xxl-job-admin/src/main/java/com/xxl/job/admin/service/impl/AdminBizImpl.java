package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.scheduler.MisfireStrategyEnum;
import com.xxl.job.admin.core.scheduler.ScheduleTypeEnum;
import com.xxl.job.admin.core.thread.JobCompleteHelper;
import com.xxl.job.admin.core.thread.JobRegistryHelper;
import com.xxl.job.admin.core.util.JdbcDbLockUtils;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.JobExecutorInitParam;
import com.xxl.job.core.biz.model.JobExecutorParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author xuxueli 2017-07-27 21:54:20
 */
@Slf4j
@Service
@Getter
@Setter
public class AdminBizImpl implements AdminBiz {

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;

    @Resource
    private XxlJobInfoDao xxlJobInfoDao;

    @Resource
    private XxlJobServiceImpl xxlJobService;

    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return JobCompleteHelper.getInstance().callback(callbackParamList);
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registry(registryParam);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registryRemove(registryParam);
    }

    @Override
    public ReturnT<String> initJobInfo(JobExecutorInitParam jobExecutorParam) {
        int groupId = initJobGroups(jobExecutorParam.getJobExecutorParam());
        List<XxlJobInfo> jobsByGroup = xxlJobInfoDao.getJobsByGroup(groupId);

        JdbcDbLockUtils.executeWithDbLock(XxlJobAdminConfig.getAdminConfig().getDataSource(), jobExecutorParam.getJobExecutorParam().getAppName(),
                false, false, () -> jobExecutorParam.getJobInfoParamList()
                        .stream()
                        .filter(job -> jobsByGroup.stream()
                                .noneMatch(o -> job.getExecutorHandler().equals(o.getExecutorHandler())))
                        .map(o -> {
                            XxlJobInfo xxlJobInfo = new XxlJobInfo();
                            xxlJobInfo.setAuthor("SYSTEM");
                            xxlJobInfo.setJobDesc(o.getJobDesc());
                            xxlJobInfo.setJobGroup(groupId);
                            xxlJobInfo.setTriggerStatus(1);
                            xxlJobInfo.setExecutorHandler(o.getExecutorHandler());
                            xxlJobInfo.setExecutorParam(o.getExecutorParam());
                            xxlJobInfo.setGlueType(GlueTypeEnum.BEAN.name());
                            xxlJobInfo.setGlueRemark("GLUE代码初始化");
                            if (o.getCron() != null && !o.getCron().trim().isEmpty()) {
                                xxlJobInfo.setScheduleType(ScheduleTypeEnum.CRON.name());
                                xxlJobInfo.setScheduleConf(o.getCron());
                            } else if (o.getFixedRate() > 0) {
                                xxlJobInfo.setScheduleType(ScheduleTypeEnum.FIX_RATE.name());
                                xxlJobInfo.setScheduleConf(String.valueOf(o.getFixedRate()));
                            }
                            xxlJobInfo.setMisfireStrategy(MisfireStrategyEnum.DO_NOTHING.name());
                            xxlJobInfo.setExecutorRouteStrategy(ExecutorRouteStrategyEnum.FIRST.name());
                            xxlJobInfo.setExecutorBlockStrategy(ExecutorBlockStrategyEnum.SERIAL_EXECUTION.name());
                            return xxlJobInfo;
                        }).forEach(o -> xxlJobService.add(o, new XxlJobUser())));

        return ReturnT.SUCCESS;
    }

    private int initJobGroups(JobExecutorParam jobExecutorParam) {
        Optional<XxlJobGroup> any = xxlJobGroupDao.findAll().stream()
                .filter(o -> o.getAppname().equals(jobExecutorParam.getAppName()))
                .findAny();
        if (any.isPresent()) {
            return any.get().getId();
        }

        XxlJobGroup newGroup = new XxlJobGroup();
        newGroup.setAppname(jobExecutorParam.getAppName());
        newGroup.setTitle(jobExecutorParam.getTitle() == null || jobExecutorParam.getTitle().trim().isEmpty()
                ? jobExecutorParam.getAppName() : jobExecutorParam.getTitle());
        newGroup.setTitle(newGroup.getTitle().substring(0, Math.min(30, newGroup.getTitle().length())));
        newGroup.setUpdateTime(new Date());
        newGroup.setAddressType(0);

        try {
            JdbcDbLockUtils.executeWithDbLock(XxlJobAdminConfig.getAdminConfig().getDataSource(),
                    jobExecutorParam.getAppName(), true, false, () -> xxlJobGroupDao.save(newGroup));
        } catch (Exception e) {
            log.warn("初始化job_group失败, 可能已存在, appName={}", jobExecutorParam.getAppName(), e);
        }

        if (newGroup.getId() > 0) {
            return newGroup.getId();
        }


        // 重试3次查询已存在的分组
        for (int i = 0; i < 3; i++) {
            any = xxlJobGroupDao.findAll().stream()
                    .filter(o -> o.getAppname().equals(jobExecutorParam.getAppName()))
                    .findAny();
            if (any.isPresent()) {
                return any.get().getId();
            }
            // 如果没找到，等待一段时间后重试
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // 3次重试后仍然找不到，则抛出异常
        throw new RuntimeException("初始化job_group失败, 请检查数据库!");
    }
}
