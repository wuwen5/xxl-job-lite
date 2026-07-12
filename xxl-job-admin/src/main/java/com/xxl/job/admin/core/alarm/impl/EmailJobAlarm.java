package com.xxl.job.admin.core.alarm.impl;

import com.xxl.job.admin.core.alarm.JobAlarm;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.model.ReturnT;
import jakarta.mail.internet.MimeMessage;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * job alarm by email
 *
 * @author xuxueli 2020-01-19
 */
@Slf4j
@Component
public class EmailJobAlarm implements JobAlarm {

    /**
     * fail alarm
     *
     * @param jobLog
     */
    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {
        boolean alarmResult = true;

        // send monitor email
        if (info != null && StringUtils.hasText(info.getAlarmEmail())) {

            // alarmContent
            String alarmContent = "Alarm Job LogId=" + jobLog.getId();
            if (jobLog.getTriggerCode() != ReturnT.SUCCESS_CODE) {
                alarmContent += "<br>TriggerMsg=<br>" + jobLog.getTriggerMsg();
            }
            if (jobLog.getHandleCode() > 0 && jobLog.getHandleCode() != ReturnT.SUCCESS_CODE) {
                alarmContent += "<br>HandleCode=" + jobLog.getHandleMsg();
            }

            // email info
            XxlJobGroup group =
                    XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().load(info.getJobGroup());
            String personal = I18nUtil.getString("admin_name_full");
            String title = I18nUtil.getString("jobconf_monitor");
            String content = MessageFormat.format(
                    loadEmailJobAlarmTemplate(),
                    group != null ? group.getTitle() : "null",
                    info.getId(),
                    info.getJobDesc(),
                    alarmContent);

            Set<String> emailSet =
                    new HashSet<>(Arrays.asList(info.getAlarmEmail().split(",")));
            for (String email : emailSet) {

                // make mail
                try {
                    MimeMessage mimeMessage =
                            XxlJobAdminConfig.getAdminConfig().getMailSender().createMimeMessage();

                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                    helper.setFrom(XxlJobAdminConfig.getAdminConfig().getEmailFrom(), personal);
                    helper.setTo(email);
                    helper.setSubject(title);
                    helper.setText(content, true);

                    XxlJobAdminConfig.getAdminConfig().getMailSender().send(mimeMessage);
                } catch (Exception e) {
                    log.error(">>>>>>>>>>> xxl-job, job fail alarm email send error, JobLogId:{}", jobLog.getId(), e);

                    alarmResult = false;
                }
            }
        }

        return alarmResult;
    }

    /**
     * load email job alarm template
     *
     * @return
     */
    private static String loadEmailJobAlarmTemplate() {
        String jobgroup = I18nUtil.getString("jobinfo_field_jobgroup");
        String id = I18nUtil.getString("jobinfo_field_id");
        String jobdesc = I18nUtil.getString("jobinfo_field_jobdesc");
        String alarmTitle = I18nUtil.getString("jobconf_monitor_alarm_title");
        String alarmContent = I18nUtil.getString("jobconf_monitor_alarm_content");
        String alarmType = I18nUtil.getString("jobconf_monitor_alarm_type");
        String monitorDetail = I18nUtil.getString("jobconf_monitor_detail");

        return String.format(
                """
        <h5>%s：</h5>
        <table border="1" cellpadding="3" style="border-collapse:collapse; width:80%%;">
           <thead style="font-weight:bold; color:#ffffff; background-color:#ff8c00;">
              <tr>
                 <td width="20%%">%s</td>
                 <td width="10%%">%s</td>
                 <td width="20%%">%s</td>
                 <td width="10%%">%s</td>
                 <td width="40%%">%s</td>
              </tr>
           </thead>
           <tbody>
              <tr>
                 <td>{0}</td>
                 <td>{1}</td>
                 <td>{2}</td>
                 <td>%s</td>
                 <td>{3}</td>
              </tr>
           </tbody>
        </table>
        """,
                monitorDetail, jobgroup, id, jobdesc, alarmTitle, alarmContent, alarmType);
    }
}
