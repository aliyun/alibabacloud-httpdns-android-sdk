package com.alibaba.sdk.android.httpdns;

import java.util.HashMap;

/**
 * 增加初始化逻辑的单例
 * 用于在测试用的httpdns实例中增加测试需要的初始化逻辑
 * @author zonglin.nzl
 * @date 1/14/22
 */
public class InitManager {

    private static class Holder {
        private static final InitManager instance = new InitManager();
    }

    public static InitManager getInstance() {
        return Holder.instance;
    }

    private InitManager() {
    }

    private HashMap<String, BeforeHttpDnsServiceInit> initThings = new HashMap<>();


    public void add(String accountId, BeforeHttpDnsServiceInit beforeHttpDnsServiceInit) {
        initThings.put(accountId, beforeHttpDnsServiceInit);
    }

    public BeforeHttpDnsServiceInit getAndRemove(String accountId) {
        return initThings.remove(accountId);
    }
}
