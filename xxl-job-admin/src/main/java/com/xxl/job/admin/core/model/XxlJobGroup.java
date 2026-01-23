package com.xxl.job.admin.core.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by xuxueli on 16/9/30.
 */
@Getter
@Setter
public class XxlJobGroup implements Serializable {
    
    private static final long serialVersionUID = 42L;

    private int id;
    private String appname;
    private String title;
    
    /**
     * 执行器地址类型：0=自动注册、1=手动录入
     */
    private int addressType;
    
    /**
     * 执行器地址列表，多地址逗号分隔(手动录入)
     */
    private String addressList;
    
    private Date updateTime;

    /**
     * 执行器地址列表(系统注册)
     */
    private List<String> registryList;  
    public List<String> getRegistryList() {
        if (addressList!=null && !addressList.trim().isEmpty()) {
            registryList = new ArrayList<>(Arrays.asList(addressList.split(",")));
        }
        return registryList;
    }
}
