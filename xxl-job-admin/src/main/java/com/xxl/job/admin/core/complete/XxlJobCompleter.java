package com.xxl.job.admin.core.complete;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.thread.JobTriggerPoolHelper;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.context.XxlJobContext;
import java.text.MessageFormat;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * @author xuxueli 2020-10-30 20:43:10
 */
@UtilityClass
@Slf4j
public class XxlJobCompleter {

    /**
     * common fresh handle entrance (limit only once)
     *
     * @param xxlJobLog
     * @return
     */
    public static int updateHandleInfoAndFinish(XxlJobLog xxlJobLog) {

        // finish
        finishJob(xxlJobLog);

        // text最大64kb 避免长度过长
        if (xxlJobLog.getHandleMsg().length() > 15000) {
            xxlJobLog.setHandleMsg(xxlJobLog.getHandleMsg().substring(0, 15000));
        }

        // fresh handle
        return XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateHandleInfo(xxlJobLog);
    }

    /**
     * do somethind to finish job
     */
    private static void finishJob(XxlJobLog xxlJobLog) {

        // 1、handle success, to trigger child job
        StringBuilder triggerChildMsg = null;
        if (XxlJobContext.HANDLE_CODE_SUCCESS == xxlJobLog.getHandleCode()) {
            XxlJobInfo xxlJobInfo =
                    XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(xxlJobLog.getJobId());
            if (xxlJobInfo != null && StringUtils.hasText(xxlJobInfo.getChildJobId())) {
                triggerChildMsg = new StringBuilder("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>"
                        + I18nUtil.getString("jobconf_trigger_child_run") + "<<<<<<<<<<< </span><br>");

                String[] childJobIds = xxlJobInfo.getChildJobId().split(",");
                for (int i = 0; i < childJobIds.length; i++) {
                    int childJobId = isNumeric(childJobIds[i]) ? Integer.parseInt(childJobIds[i]) : -1;
                    if (childJobId > 0) {
                        // valid
                        if (childJobId == xxlJobLog.getJobId()) {
                            log.debug(
                                    ">>>>>>>>>>> xxl-job, XxlJobCompleter-finishJob ignore childJobId,  childJobId {} is self.",
                                    childJobId);
                            continue;
                        }

                        // trigger child job
                        JobTriggerPoolHelper.trigger(childJobId, TriggerTypeEnum.PARENT, -1, null, null, null);

                        // add msg
                        triggerChildMsg.append(MessageFormat.format(
                                I18nUtil.getString("jobconf_callback_child_msg1"),
                                (i + 1),
                                childJobIds.length,
                                childJobIds[i],
                                I18nUtil.getString("system_success"),
                                ""));
                    } else {
                        triggerChildMsg.append(MessageFormat.format(
                                I18nUtil.getString("jobconf_callback_child_msg2"),
                                (i + 1),
                                childJobIds.length,
                                childJobIds[i]));
                    }
                }
            }
        }

        if (triggerChildMsg != null) {
            xxlJobLog.setHandleMsg(xxlJobLog.getHandleMsg() + triggerChildMsg);
        }

        // 2、fix_delay trigger next
        // on the way

    }

    private static boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
