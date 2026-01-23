package com.xxl.job.admin.core.trigger;

import com.xxl.job.admin.core.util.I18nUtil;

/**
 * trigger type enum
 *
 * @author xuxueli 2018-09-16 04:56:41
 */
public enum TriggerTypeEnum {

    /**
     * manual trigger
     */
    MANUAL(I18nUtil.getString("jobconf_trigger_type_manual")),
    /**
     * cron trigger
     */
    CRON(I18nUtil.getString("jobconf_trigger_type_cron")),
    /**
     * retry trigger
     */
    RETRY(I18nUtil.getString("jobconf_trigger_type_retry")),
    /**
     * parent-child trigger
     */
    PARENT(I18nUtil.getString("jobconf_trigger_type_parent")),
    /**
     * api trigger
     */
    API(I18nUtil.getString("jobconf_trigger_type_api")),
    /**
     * misfire trigger
     */
    MISFIRE(I18nUtil.getString("jobconf_trigger_type_misfire"));

    TriggerTypeEnum(String title){
        this.title = title;
    }
    private final String title;
    public String getTitle() {
        return title;
    }

}
