package com.aliyun.ams.httpdns.demo;

import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.sdk.android.httpdns.CacheTtlChanger;
import com.alibaba.sdk.android.httpdns.HttpDns;
import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.InitConfig;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.ranking.IPRankingBean;
import com.aliyun.ams.httpdns.demo.utils.SpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 保存Httpdns 及 相关配置，
 * 方便修改
 */
public class HttpDnsHolder {

    public static final String SP_PREFIX = "httpdns_config_";

    public static String getSpName(String accountId) {
        return SP_PREFIX + accountId;
    }

    public static final String KEY_EXPIRED_IP = "enableExpiredIp";
    public static final String KEY_CACHE_IP = "enableCacheIp";
    public static final String KEY_TIMEOUT = "timeout";
    public static final String KEY_HTTPS = "enableHttps";
    public static final String KEY_IP_RANKING_ITEMS = "ipProbeItems";
    public static final String KEY_REGION = "region";
    public static final String KEY_TTL_CHANGER = "cacheTtlChanger";
    public static final String KEY_HOST_NOT_CHANGE = "hostListWithFixedIp";

    private HttpDnsService service;
    private String accountId;
    private String secret;
    private Context context;

    private boolean enableExpiredIp;
    private boolean enableCacheIp;
    private int timeout;
    private boolean enableHttps;
    private ArrayList<IPRankingBean> ipRankingList = null;
    private String region;
    private ArrayList<String> hostListWithFixedIp;
    private HashMap<String, Integer> ttlCache;
    private final CacheTtlChanger cacheTtlChanger = new CacheTtlChanger() {
        @Override
        public int changeCacheTtl(String host, RequestIpType type, int ttl) {
            if (ttlCache != null && ttlCache.get(host) != null) {
                return ttlCache.get(host);
            }
            return ttl;
        }
    };

    public HttpDnsHolder(String accountId) {
        this.accountId = accountId;
    }

    public HttpDnsHolder(String accountId, String secret) {
        this.accountId = accountId;
        this.secret = secret;
    }

    /**
     * 初始化httpdns的配置
     *
     * @param context
     */
    public void init(Context context) {
        this.context = context.getApplicationContext();
        SpUtil.readSp(context, getSpName(accountId), new SpUtil.OnGetSp() {
            @Override
            public void onGetSp(SharedPreferences sp) {
                enableExpiredIp = sp.getBoolean(KEY_EXPIRED_IP, true);
                enableCacheIp = sp.getBoolean(KEY_CACHE_IP, false);
                timeout = sp.getInt(KEY_TIMEOUT, 5 * 1000);
                enableHttps = sp.getBoolean(KEY_HTTPS, false);
                ipRankingList = convertToProbeList(sp.getString(KEY_IP_RANKING_ITEMS, null));
                region = sp.getString(KEY_REGION, null);
                ttlCache = convertToCacheTtlData(sp.getString(KEY_TTL_CHANGER, null));
                hostListWithFixedIp = convertToStringList(sp.getString(KEY_HOST_NOT_CHANGE, null));
            }
        });

        // 初始化httpdns 的配置，此步骤需要在第一次获取HttpDnsService实例之前
        new InitConfig.Builder()
                .setEnableExpiredIp(enableExpiredIp)
                .setEnableCacheIp(enableCacheIp)
                .setTimeout(timeout)
                .setEnableHttps(enableHttps)
                .setIPRankingList(ipRankingList)
                .setRegion(region)
                .configCacheTtlChanger(cacheTtlChanger)
                .configHostWithFixedIp(hostListWithFixedIp)
                .buildFor(accountId);

        getService();
    }

    public HttpDnsService getService() {
        if (service == null) {
            if (secret != null) {
                service = HttpDns.getService(context, accountId, secret);
            } else {
                service = HttpDns.getService(context, accountId);
            }
        }
        return service;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setEnableExpiredIp(final boolean enableExpiredIp) {
        this.enableExpiredIp = enableExpiredIp;
        getService().setExpiredIPEnabled(enableExpiredIp);
        SpUtil.writeSp(context, getSpName(accountId), new SpUtil.OnGetSpEditor() {
            @Override
            public void onGetSpEditor(SharedPreferences.Editor editor) {
                editor.putBoolean(KEY_EXPIRED_IP, enableExpiredIp);
            }
        });
    }

    public void setEnableCacheIp(final boolean enableCacheIp) {
        this.enableCacheIp = enableCacheIp;
        getService().setCachedIPEnabled(enableCacheIp);
        SpUtil.writeSp(context, getSpName(accountId), new SpUtil.OnGetSpEditor() {
            @Override
            public void onGetSpEditor(SharedPreferences.Editor editor) {
                editor.putBoolean(KEY_CACHE_IP, enableCacheIp);
            }
        });
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
        getService().setTimeoutInterval(timeout);
        SpUtil.writeSp(context, getSpName(accountId), new SpUtil.OnGetSpEditor() {
            @Override
            public void onGetSpEditor(SharedPreferences.Editor editor) {
                editor.putInt(KEY_TIMEOUT, timeout);
            }
        });
    }

    public void setEnableHttps(final boolean enableHttps) {
        this.enableHttps = enableHttps;
        getService().setHTTPSRequestEnabled(enableHttps);
        SpUtil.writeSp(context, getSpName(accountId), new SpUtil.OnGetSpEditor() {
            @Override
            public void onGetSpEditor(SharedPreferences.Editor editor) {
                editor.putBoolean(KEY_HTTPS, enableHttps);
            }
        });
    }

    public void setRegion(final String region) {
        this.region = region;
        getService().setRegion(region);
        SpUtil.writeSp(context, getSpName(accountId), new SpUtil.OnGetSpEditor() {
            @Override
            public void onGetSpEditor(SharedPreferences.Editor editor) {
                editor.putString(KEY_REGION, region);
            }
        });
    }

    public void addHostWithFixedIp(String host) {
        if (this.hostListWithFixedIp == null) {
            this.hostListWithFixedIp = new ArrayList<>();
        }
        this.hostListWithFixedIp.add(host);
        // 重启生效
        SpUtil.writeSp(context, getSpName(accountId), new SpUtil.OnGetSpEditor() {
            @Override
            public void onGetSpEditor(SharedPreferences.Editor editor) {
                editor.putString(KEY_HOST_NOT_CHANGE, convertHostList(hostListWithFixedIp));
            }
        });
    }

    public void addIpProbeItem(IPRankingBean ipProbeItem) {
        if (this.ipRankingList == null) {
            this.ipRankingList = new ArrayList<>();
        }
        this.ipRankingList.add(ipProbeItem);
        getService().setIPProbeList(ipRankingList);
        SpUtil.writeSp(context, getSpName(accountId), new SpUtil.OnGetSpEditor() {
            @Override
            public void onGetSpEditor(SharedPreferences.Editor editor) {
                editor.putString(KEY_IP_RANKING_ITEMS, convertProbeList(ipRankingList));
            }
        });
    }


    public void setHostTtl(String host, int ttl) {
        if (ttlCache == null) {
            ttlCache = new HashMap<>();
        }
        ttlCache.put(host, ttl);
        SpUtil.writeSp(context, getSpName(accountId), new SpUtil.OnGetSpEditor() {
            @Override
            public void onGetSpEditor(SharedPreferences.Editor editor) {
                editor.putString(KEY_TTL_CHANGER, convertTtlCache(ttlCache));
            }
        });
    }

    public void cleanSp() {
        SpUtil.writeSp(context, getSpName(accountId), new SpUtil.OnGetSpEditor() {
            @Override
            public void onGetSpEditor(SharedPreferences.Editor editor) {
                editor.clear();
            }
        });
    }

    public String getCurrentConfig() {
        StringBuilder sb = new StringBuilder();
        sb.append("当前配置 accountId : ").append(accountId).append("\n")
                .append("是否允许过期IP : ").append(enableExpiredIp).append("\n")
                .append("是否开启本地缓存 : ").append(enableCacheIp).append("\n")
                .append("是否开启HTTPS : ").append(enableHttps).append("\n")
                .append("当前region设置 : ").append(region).append("\n")
                .append("当前超时设置 : ").append(timeout).append("\n")
                .append("当前探测配置 : ").append(convertProbeList(ipRankingList)).append("\n")
                .append("当前缓存配置 : ").append(convertTtlCache(ttlCache)).append("\n")
                .append("当前主站域名配置 : ").append(convertHostList(hostListWithFixedIp)).append("\n")
        ;
        return sb.toString();
    }


    private static String convertHostList(List<String> hostListWithFixedIp) {
        if (hostListWithFixedIp == null) {
            return null;
        }
        JSONArray array = new JSONArray();
        for (String host : hostListWithFixedIp) {
            array.put(host);
        }
        return array.toString();
    }

    private static String convertTtlCache(HashMap<String, Integer> ttlCache) {
        if (ttlCache == null) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        for (String host : ttlCache.keySet()) {
            try {
                jsonObject.put(host, ttlCache.get(host));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonObject.toString();
    }

    private static String convertProbeList(List<IPRankingBean> ipProbeItems) {
        if (ipProbeItems == null) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        for (IPRankingBean item : ipProbeItems) {
            try {
                jsonObject.put(item.getHostName(), item.getPort());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonObject.toString();
    }

    private static ArrayList<IPRankingBean> convertToProbeList(String json) {
        if (json == null) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            ArrayList<IPRankingBean> list = new ArrayList<>();
            for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                String host = it.next();
                list.add(new IPRankingBean(host, jsonObject.getInt(host)));
            }
            return list;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static HashMap<String, Integer> convertToCacheTtlData(String json) {
        if (json == null) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            HashMap<String, Integer> map = new HashMap<>();
            for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                String host = it.next();
                map.put(host, jsonObject.getInt(host));
            }
            return map;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static ArrayList<String> convertToStringList(String json) {
        if (json != null) {
            try {
                JSONArray array = new JSONArray(json);
                ArrayList<String> list = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    list.add(array.getString(i));
                }
                return list;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
