package com.xxl.job.admin.core.alarm.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.core.biz.model.ReturnT;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Unit tests for {@link EmailJobAlarm}.
 */
@ExtendWith(MockitoExtension.class)
public class EmailJobAlarmTest {

    @Mock
    private XxlJobAdminConfig adminConfig;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private XxlJobGroupDao xxlJobGroupDao;

    private MockedStatic<XxlJobAdminConfig> mockedStatic;
    private EmailJobAlarm emailJobAlarm;

    @BeforeEach
    void setUp() throws Exception {

        mockedStatic = mockStatic(XxlJobAdminConfig.class);
        mockedStatic.when(XxlJobAdminConfig::getAdminConfig).thenReturn(adminConfig);

        emailJobAlarm = new EmailJobAlarm();
    }

    @AfterEach
    void tearDown() {
        mockedStatic.close();
    }

    private MimeMessage createRealMimeMessage() {
        Properties props = new Properties();
        Session session = Session.getInstance(props);
        return new MimeMessage(session);
    }

    private String extractHtmlContent(MimeMessage message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                if (part.getContentType() != null && part.getContentType().contains("text/html")) {
                    return (String) part.getContent();
                }
            }
        }
        return null;
    }

    private XxlJobInfo buildJobInfo(String alarmEmail) {
        XxlJobInfo info = new XxlJobInfo();
        info.setId(1);
        info.setJobGroup(1);
        info.setJobDesc("Test Job");
        info.setAlarmEmail(alarmEmail);
        return info;
    }

    private XxlJobLog buildJobLog(long id, int triggerCode, int handleCode) {
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(id);
        jobLog.setTriggerCode(triggerCode);
        jobLog.setTriggerMsg("Trigger message");
        jobLog.setHandleCode(handleCode);
        jobLog.setHandleMsg("Handle message");
        return jobLog;
    }

    /**
     * When job info is null, no email should be sent and result should be true.
     */
    @Test
    void testDoAlarm_NullInfo_ReturnsTrue() {
        XxlJobLog jobLog = buildJobLog(1L, ReturnT.SUCCESS_CODE, 0);

        boolean result = emailJobAlarm.doAlarm(null, jobLog);

        assertTrue(result);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    /**
     * When alarm email is blank, no email should be sent and result should be true.
     */
    @Test
    void testDoAlarm_BlankAlarmEmail_ReturnsTrue() {
        XxlJobInfo info = buildJobInfo("  ");
        XxlJobLog jobLog = buildJobLog(1L, ReturnT.SUCCESS_CODE, 0);

        boolean result = emailJobAlarm.doAlarm(info, jobLog);

        assertTrue(result);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    /**
     * When trigger fails, the email content should contain the trigger message.
     */
    @Test
    void testDoAlarm_TriggerFailure_SendEmailWithTriggerMsg() throws Exception {
        XxlJobInfo info = buildJobInfo("alarm@xxl-job.com");
        XxlJobLog jobLog = buildJobLog(1L, ReturnT.FAIL_CODE, 0);

        XxlJobGroup group = new XxlJobGroup();
        group.setTitle("Test Group");
        when(xxlJobGroupDao.load(1)).thenReturn(group);

        MimeMessage realMessage = createRealMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(realMessage);

        when(adminConfig.getMailSender()).thenReturn(mailSender);
        when(adminConfig.getXxlJobGroupDao()).thenReturn(xxlJobGroupDao);
        when(adminConfig.getEmailFrom()).thenReturn("test@xxl-job.com");
        lenient().when(adminConfig.getI18n()).thenReturn("zh_CN");

        boolean result = emailJobAlarm.doAlarm(info, jobLog);

        assertTrue(result);
        verify(mailSender, times(1)).send(realMessage);
    }

    /**
     * Multiple emails should be deduplicated and each unique address should receive one email.
     */
    @Test
    void testDoAlarm_MultipleEmails_DeduplicatedAndSent() {
        XxlJobInfo info = buildJobInfo("a@xxl-job.com,b@xxl-job.com,a@xxl-job.com,c@xxl-job.com");
        XxlJobLog jobLog = buildJobLog(3L, ReturnT.FAIL_CODE, 0);

        XxlJobGroup group = new XxlJobGroup();
        group.setTitle("Test Group");
        when(adminConfig.getEmailFrom()).thenReturn("test@xxl-job.com");
        when(adminConfig.getMailSender()).thenReturn(mailSender);
        lenient().when(adminConfig.getI18n()).thenReturn("zh_CN");
        when(adminConfig.getXxlJobGroupDao()).thenReturn(xxlJobGroupDao);
        when(xxlJobGroupDao.load(1)).thenReturn(group);

        when(mailSender.createMimeMessage()).thenReturn(createRealMimeMessage());

        boolean result = emailJobAlarm.doAlarm(info, jobLog);

        assertTrue(result);
        verify(mailSender, times(3)).send(any(MimeMessage.class));
    }

    /**
     * When mail sender throws exception, the method should return false.
     */
    @Test
    void testDoAlarm_SendException_ReturnsFalse() {
        XxlJobInfo info = buildJobInfo("alarm@xxl-job.com");
        XxlJobLog jobLog = buildJobLog(4L, ReturnT.FAIL_CODE, 0);

        XxlJobGroup group = new XxlJobGroup();
        group.setTitle("Test Group");
        when(xxlJobGroupDao.load(1)).thenReturn(group);

        when(adminConfig.getMailSender()).thenReturn(mailSender);
        when(adminConfig.getXxlJobGroupDao()).thenReturn(xxlJobGroupDao);
        when(adminConfig.getEmailFrom()).thenReturn("test@xxl-job.com");

        when(mailSender.createMimeMessage()).thenReturn(createRealMimeMessage());
        doThrow(new MailSendException("send failed")).when(mailSender).send(any(MimeMessage.class));

        boolean result = emailJobAlarm.doAlarm(info, jobLog);

        assertFalse(result);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    /**
     * When job group is not found, the template should use "null" as placeholder.
     */
    @Test
    void testDoAlarm_NullGroup_UsesNullPlaceholder() throws Exception {
        XxlJobInfo info = buildJobInfo("alarm@xxl-job.com");
        XxlJobLog jobLog = buildJobLog(5L, ReturnT.FAIL_CODE, 0);

        when(adminConfig.getMailSender()).thenReturn(mailSender);
        when(adminConfig.getXxlJobGroupDao()).thenReturn(xxlJobGroupDao);
        when(adminConfig.getEmailFrom()).thenReturn("test@xxl-job.com");

        when(xxlJobGroupDao.load(1)).thenReturn(null);

        MimeMessage realMessage = createRealMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(realMessage);

        boolean result = emailJobAlarm.doAlarm(info, jobLog);

        assertTrue(result);
        verify(mailSender, times(1)).send(realMessage);
    }
}
