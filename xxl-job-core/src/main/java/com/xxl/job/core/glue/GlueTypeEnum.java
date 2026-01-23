package com.xxl.job.core.glue;

/**
 * Created by xuxueli on 17/4/26.
 */
public enum GlueTypeEnum {

    /**
     * java Bean类型枚举
     */
    BEAN("BEAN", false, null, null),
    /**
     * GLUE脚本类型枚举(GROOVY)
     */
    GLUE_GROOVY("GLUE(Java)", false, null, null),
    /**
     * GLUE脚本类型枚举(Shell)
     */
    GLUE_SHELL("GLUE(Shell)", true, "bash", ".sh"),
    /**
     * GLUE脚本类型枚举(Python)
     */
    GLUE_PYTHON("GLUE(Python)", true, "python", ".py"),
    /**
     * GLUE脚本类型枚举(PHP)
     */
    GLUE_PHP("GLUE(PHP)", true, "php", ".php"),
    /**
     * GLUE脚本类型枚举(Nodejs)
     */
    GLUE_NODEJS("GLUE(Nodejs)", true, "node", ".js"),
    /**
     * GLUE脚本类型枚举(PowerShell)
     */
    GLUE_POWERSHELL("GLUE(PowerShell)", true, "powershell", ".ps1");

    private final String desc;
    private final boolean isScript;
    private final String cmd;
    private final String suffix;

    GlueTypeEnum(String desc, boolean isScript, String cmd, String suffix) {
        this.desc = desc;
        this.isScript = isScript;
        this.cmd = cmd;
        this.suffix = suffix;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isScript() {
        return isScript;
    }

    public String getCmd() {
        return cmd;
    }

    public String getSuffix() {
        return suffix;
    }

    public static GlueTypeEnum match(String name){
        for (GlueTypeEnum item: GlueTypeEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return null;
    }

}
