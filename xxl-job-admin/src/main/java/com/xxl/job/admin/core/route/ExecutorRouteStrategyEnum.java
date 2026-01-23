package com.xxl.job.admin.core.route;

import com.xxl.job.admin.core.route.strategy.*;
import com.xxl.job.admin.core.util.I18nUtil;

/**
 * Created by xuxueli on 17/3/10.
 */
public enum ExecutorRouteStrategyEnum {

    /**
     * First
     */
    FIRST(I18nUtil.getString("jobconf_route_first"), new ExecutorRouteFirst()),
    /**
     * Last
     */
    LAST(I18nUtil.getString("jobconf_route_last"), new ExecutorRouteLast()),
    /**
     * 轮询
     */
    ROUND(I18nUtil.getString("jobconf_route_round"), new ExecutorRouteRound()),
    /**
     * 随机
     */
    RANDOM(I18nUtil.getString("jobconf_route_random"), new ExecutorRouteRandom()),
    /**
     * 一致性哈希
     */
    CONSISTENT_HASH(I18nUtil.getString("jobconf_route_consistenthash"), new ExecutorRouteConsistentHash()),
    /**
     * 最少使用
     */
    LEAST_FREQUENTLY_USED(I18nUtil.getString("jobconf_route_lfu"), new ExecutorRouteLFU()),
    /**
     * 最近最少使用
     */
    LEAST_RECENTLY_USED(I18nUtil.getString("jobconf_route_lru"), new ExecutorRouteLRU()),
    /**
     * 失败转移
     */
    FAILOVER(I18nUtil.getString("jobconf_route_failover"), new ExecutorRouteFailover()),
    /**
     * 忙碌转移
     */
    BUSYOVER(I18nUtil.getString("jobconf_route_busyover"), new ExecutorRouteBusyover()),
    /**
     * 广播
     */
    SHARDING_BROADCAST(I18nUtil.getString("jobconf_route_shard"), null);

    ExecutorRouteStrategyEnum(String title, ExecutorRouter router) {
        this.title = title;
        this.router = router;
    }

    /**
     * 执行器路由策略名称
     */
    private final String title;
    /**
     * 执行器路由策略实现
     */
    private final ExecutorRouter router;

    /**
     *  Get title
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /** 
     * Get router
     * @return the router
     */
    public ExecutorRouter getRouter() {
        return router;
    }

    public static ExecutorRouteStrategyEnum match(String name, ExecutorRouteStrategyEnum defaultItem){
        if (name != null) {
            for (ExecutorRouteStrategyEnum item: ExecutorRouteStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }

}
