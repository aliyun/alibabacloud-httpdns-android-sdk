package com.alibaba.sdk.android.httpdns.utils;

import java.util.HashMap;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.resolve.SniffException;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;

public class Constants {

	// region 定义
	// 默认的region
	public static final String REGION_DEFAULT = Constants.REGION_MAINLAND;
	// 中国大陆
	public static final String REGION_MAINLAND = "";
	// 中国香港
	public static final String REGION_HK = "hk";
	// 新加坡
	public static final String REGION_SG = "sg";
	// 德国
	public static final String REGION_DE = "de";
	//美国
	public static final String REGION_US = "us";
	//预发
	public static final String REGION_DEBUG_PRE = "pre";
	//TIPS 增加region需要去RegionServerManager添加策略
	// global
	public static final String REGION_GLOBAL = "global";

	// 一些默认值 或者 单一场景定义
	public static final int NO_PORT = -1;
	public static final int[] NO_PORTS = null;

	public static final String[] NO_IPS = new String[0];
	public static final HashMap<String, String> NO_EXTRA = new HashMap<>();

	public static final HTTPDNSResult EMPTY = new HTTPDNSResult("", NO_IPS, NO_IPS, "",
        false,
		false);

	public static final SniffException sniff_too_often = new SniffException("sniff too often");

	public static final Exception region_not_match = new Exception("region not match");

	public static final boolean DEFAULT_SDK_ENABLE = true;
	public static final boolean DEFAULT_ENABLE_EXPIRE_IP = true;
	public static final boolean DEFAULT_ENABLE_CACHE_IP = false;
	public static final boolean DEFAULT_ENABLE_AUTO_CLEAN_CACHE_AFTER_LOAD = false;
	public static final boolean DEFAULT_ENABLE_HTTPS = false;
	public static final boolean DEFAULT_ENABLE_DEGRADATION_LOCAL_DNS = false;
	public static final String DEFAULT_SCHEMA = Constants.DEFAULT_ENABLE_HTTPS
		? HttpRequestConfig.HTTPS_SCHEMA : HttpRequestConfig.HTTP_SCHEMA;
	/**
	 * 默认超时时间2s
	 */
	public static final int DEFAULT_TIMEOUT = 2 * 1000;

	/**
	 * 同步锁的超时上限
	 */
	public static final int SYNC_TIMEOUT_MAX_LIMIT = 5 * 1000;

	// 配置缓存使用的变量
	public static final String CONFIG_CACHE_PREFIX = "httpdns_config_";
	public static final String CONFIG_ENABLE = "enable";

	public static final String CONFIG_KEY_SERVERS = "serverIps";
	public static final String CONFIG_KEY_PORTS = "ports";
	public static final String CONFIG_CURRENT_INDEX = "current";
	public static final String CONFIG_LAST_INDEX = "last";
	public static final String CONFIG_KEY_SERVERS_IPV6 = "serverIpsIpv6";
	public static final String CONFIG_KEY_PORTS_IPV6 = "portsIpv6";
	public static final String CONFIG_CURRENT_INDEX_IPV6 = "currentIpv6";
	public static final String CONFIG_LAST_INDEX_IPV6 = "lastIpv6";
	public static final String CONFIG_SERVERS_LAST_UPDATED_TIME = "servers_last_updated_time";
	public static final String CONFIG_CURRENT_SERVER_REGION = "server_region";

	public static final String CONFIG_OBSERVABLE_CONFIG = "observable_config";
	public static final String CONFIG_OBSERVABLE_BENCH_MARKS = "observable_bench_marks";
	public static final int UPDATE_REGION_SERVER_SCENES_INIT = 3;
	public static final int UPDATE_REGION_SERVER_SCENES_REGION_CHANGE = 2;
	public static final int UPDATE_REGION_SERVER_SCENES_SERVER_UNAVAILABLE = 1;
}
