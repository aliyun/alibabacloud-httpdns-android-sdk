package com.alibaba.sdk.android.httpdns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.sdk.android.httpdns.ranking.IPRankingBean;

/**
 * HttpDns服务接口
 */
public interface HttpDnsService {

	/**
	 * 设置是否打印log, 仅供调试, 正式发布时请勿开启
	 *
	 * @deprecated 请直接使用 {@link com.alibaba.sdk.android.httpdns.log.HttpDnsLog}
	 */
	@Deprecated
	void setLogEnabled(boolean shouldPrintLog);

	/**
	 * 设置预解析域名列表，默认解析ipv4
	 *
	 * @param hostList 预解析的host域名列表
	 */
	void setPreResolveHosts(ArrayList<String> hostList);

	/**
	 * 设置预解析域名列表和解析的ip类型
	 *
	 * @param hostList 预解析的host列表
	 * @param requestIpType {@link RequestIpType} 网络栈类型，v4,v6,both,auto(根据当前设备所连的网络自动判断网络栈)
	 */
	void setPreResolveHosts(ArrayList<String> hostList, RequestIpType requestIpType);

	/**
	 * 异步解析接口, 首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 *
	 * @param host 如www.aliyun.com
	 * @return 返回ip, 如果没得到解析结果, 则返回null
	 * @deprecated 该接口已废弃，后续版本可能会被删除，请使用{@link #getIPv4ForHostAsync(String)}
	 */
	@Deprecated
	String getIpByHostAsync(String host);

	/**
	 * 异步解析接口，只查询v4地址，首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 *
	 * @param host 要解析的host域名
	 * @return 返回v4地址
	 */
	String getIPv4ForHostAsync(String host);

	/**
	 * 异步解析接口, 获取ipv4列表
	 *
	 * @param host 要解析的host域名
	 * @return 返回v4地址的String 数组, 如果没得到解析结果, 则String 数组的长度为0
	 * @deprecated 该接口已废弃，后续版本可能会被删除，请使用{@link #getIPv4ListForHostAsync(String)}
	 */
	@Deprecated
	String[] getIpsByHostAsync(String host);

	/**
	 * 异步解析接口, 获取ipv4列表
	 *
	 * @param host 要解析的host域名
	 * @return 返回v4地址列表
	 */
	String[] getIPv4ListForHostAsync(String host);

	/**
	 * 异步解析接口，获取单个ipv6, 首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 *
	 * @param host 要解析的host域名
	 * @return 返回v6地址
	 * @deprecated 该接口已废弃，后续版本可能会被删除，请使用{@link #getIPv6ForHostAsync(String)}
	 */
	@Deprecated
	String getIPv6ByHostAsync(String host);

	/**
	 * 异步解析接口，获取单个ipv6, 首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 *
	 * @param host 要解析的host域名
	 * @return 返回v6地址
	 */
	String getIPv6ForHostAsync(String host);

	/**
	 * 异步解析接口，获取ipv6列表, 首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 *
	 * @param host 要解析的host域名
	 * @return 返回v6地址的String 数组, 如果没得到解析结果, 则String 数组的长度为0
	 * @deprecated 该接口已废弃，后续版本可能会被删除，请使用{@link #getIPv6ListForHostASync(String)}
	 */
	@Deprecated
	String[] getIPv6sByHostAsync(String host);

	/**
	 * 异步解析接口，获取ipv6列表, 首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 *
	 * @param host 要解析的host域名
	 * @return 返回v6地址的String 数组, 如果没得到解析结果, 则String 数组的长度为0
	 */
	String[] getIPv6ListForHostASync(String host);

	/**
	 * 异步解析接口，获取ipv4ipv6列表
	 *
	 * @param host 要解析的host域名
	 * @return {@link HTTPDNSResult}
	 * @deprecated 该接口已废弃，后续版本可能会被删除，请使用{@link #getHttpDnsResultForHostAsync(String)}
	 */
	@Deprecated
	HTTPDNSResult getAllByHostAsync(String host);

	/**
	 * 异步解析接口，获取ipv4 + ipv6列表, 首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 *
	 * @param host 要解析的host域名
	 * @return {@link HTTPDNSResult}
	 */
	HTTPDNSResult getHttpDnsResultForHostAsync(String host);

	/**
	 * 异步解析接口，获取ipv4 + ipv6列表, 首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 * 支持 指定解析IP类型
	 *
	 * @param host 要解析的host域名
	 * @param type {@link RequestIpType} 网络栈类型，v4,v6,both,auto(根据当前设备所连的网络自动判断网络栈)
	 * @return {@link HTTPDNSResult}
	 * @deprecated 该接口已废弃，后续版本可能会被删除，请使用{@link #getHttpDnsResultForHostAsync(String, RequestIpType)}
	 */
	@Deprecated
	HTTPDNSResult getIpsByHostAsync(String host, RequestIpType type);

	/**
	 * 异步解析接口，获取ipv4 + ipv6列表, 首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 * 支持 指定解析IP类型
	 *
	 * @param host 要解析的host域名
	 * @param type {@link RequestIpType} 网络栈类型，v4,v6,both,auto(根据当前设备所连的网络自动判断网络栈)
	 * @return {@link HTTPDNSResult}
	 */
	HTTPDNSResult getHttpDnsResultForHostAsync(String host, RequestIpType type);

	/**
	 * 设置是否允许返回超过ttl 的ip
	 */
	void setExpiredIPEnabled(boolean enable);

	/**
	 * 设置是否允许使用DB缓存
	 */
	void setCachedIPEnabled(boolean enable);

	/**
	 * 设置是否允许使用DB缓存
	 *
	 * @param enable 是否允许DB缓存
	 * @param autoCleanCacheAfterLoad 数据库加载缓存后是否立即删除缓存
	 */
	void setCachedIPEnabled(boolean enable, boolean autoCleanCacheAfterLoad);

	/***
	 * 校正App签名时间
	 * @param time time为epoch时间戳，1970年1月1日以来的秒数
	 */
	void setAuthCurrentTime(long time);

	/**
	 * 设置降级策略, 用户可定制规则降级为原生DNS解析方式
	 *
	 * @param filter {@link  DegradationFilter}
	 */
	void setDegradationFilter(DegradationFilter filter);

	/**
	 * 设置网络切换时是否自动刷新所有域名解析结果
	 *
	 * @param enable 是否允许自动刷新域名解析结果
	 */
	void setPreResolveAfterNetworkChanged(boolean enable);

	/**
	 * 设置请求超时时间,单位ms,默认为2s
	 *
	 * @param timeoutInterval 超时时间，单位ms
	 */
	void setTimeoutInterval(int timeoutInterval);

	/**
	 * 设置HTTPDNS域名解析请求类型(HTTP/HTTPS)，若不调用该接口，默认为HTTP请求
	 *
	 * @param enabled 是否使用https
	 */
	void setHTTPSRequestEnabled(boolean enabled);

	/**
	 * 设置要探测的域名列表,默认只会对ipv4的地址进行ip优选
	 *
	 * @param ipProbeList {@link IPRankingBean}
	 * @deprecated 该接口已废弃，后续版本可能会删除，请使用{@link #setIPRankingList(List)}
	 */
	@Deprecated
	void setIPProbeList(List<IPRankingBean> ipProbeList);

	/**
	 * 设置要探测的域名列表,默认只会对ipv4的地址进行ip优选
	 *
	 * @param ipRankingList {@link IPRankingBean}
	 */
	void setIPRankingList(List<IPRankingBean> ipRankingList);

	/**
	 * 获取会话id
	 *
	 * @return sid
	 */
	String getSessionId();

	/**
	 * 设置用于接收sdk日志的回调类
	 *
	 * @param logger {@link ILogger}
	 * @deprecated 请直接使用 {@link com.alibaba.sdk.android.httpdns.log.HttpDnsLog}
	 */
	@Deprecated
	void setLogger(ILogger logger);

	//以下针对SDNS

	/**
	 * 异步解析接口, 获取ipv4 + ipv6 列表,首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 * 支持配置sdns参数
	 *
	 * @param  host 要解析的host域名
	 * @return {@link HTTPDNSResult}
	 * @deprecated 该接口已废弃，后续版本可能会被删除，请使用{@link #getHttpDnsResultForHostAsync(String, Map, String)}
	 */
	@Deprecated
	HTTPDNSResult getIpsByHostAsync(String host, Map<String, String> params, String cacheKey);

	/**
	 * 异步解析接口, 获取ipv4 + ipv6列表, 首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 * 支持配置sdns参数
	 *
	 * @param host 要解析的host域名
	 * @param params
	 * @param cacheKey
	 * @return {@link HTTPDNSResult}
	 */
	HTTPDNSResult getHttpDnsResultForHostAsync(String host, Map<String, String> params,
											   String cacheKey);

	/**
	 * 异步解析接口, 获取ipv4 + ipv6列表, 首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 * 支持配置sdns参数
	 * 支持指定解析类型
	 *
	 * @param host 要解析的host域名
	 * @param type {@link RequestIpType} 网络栈类型，v4,v6,both,auto(根据当前设备所连的网络自动判断网络栈)
	 * @param params
	 * @param cacheKey
	 * @return {@link HTTPDNSResult}
	 * @deprecated 该接口已废弃，后续版本可能会被删除，请使用{@link #getHttpDnsResultForHostAsync(String, RequestIpType,
	 * Map, String)}
	 */
	@Deprecated
	HTTPDNSResult getIpsByHostAsync(String host, RequestIpType type, Map<String, String> params,
									String cacheKey);

	/**
	 * 异步解析接口, 获取ipv4 + ipv6列表, 首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 * 支持配置sdns参数
	 * 支持指定解析类型
	 *
	 * @param host 要解析的host域名
	 * @param type {@link RequestIpType} 网络栈类型，v4,v6,both,auto(根据当前设备所连的网络自动判断网络栈)
	 * @param params
	 * @param cacheKey
	 * @return {@link HTTPDNSResult}
	 */
	HTTPDNSResult getHttpDnsResultForHostAsync(String host, RequestIpType type, Map<String,
		String> params, String cacheKey);

	/**
	 * 设置sdns全局参数（该全局参数不影响异步解析任务，只用于解析接口调用时进行参数合并）
	 */
	void setSdnsGlobalParams(Map<String, String> params);

	/**
	 * 清除sdns全局参数
	 */
	void clearSdnsGlobalParams();

	//以上针对SDNS

	/**
	 * 设置region，海外节点
	 * 国内版默认是中国大陆节点，国际版默认是新加坡节点
	 *
	 * @param region sg(新家坡), hk(中国香港), ""(中国大陆)
	 */
	void setRegion(String region);

	/**
	 * 立即清除域名端侧内存和本地缓存。
	 * 后续调用异步接口，会先返回空，触发域名解析
	 *
	 * @param hosts host域名列表
	 */
	void cleanHostCache(ArrayList<String> hosts);

	/**
	 * 是否开启sdk内部的崩溃保护机制，默认是关闭的
	 */
	void enableCrashDefend(boolean enabled);

	/**
	 * 同步解析接口，支持指定解析类型
	 * 需要注意的地方:
	 * 1. 该方法必须在子线程中执行，如果在主线程中调用该方法，方法内部会自动切换成异步执行
	 * 2. 同步接口会阻塞当前子线程，阻塞时长可以通过{@link #setTimeoutInterval(int)}或者{@link InitConfig.Builder#setTimeout(int)}设置，
	 * 不过阻塞时长的上限是5s，如果设置的超时时长超过5s则无效
	 *
	 * @param host 要解析的host域名列表
	 * @param type {@link RequestIpType}
	 * @return {@link HTTPDNSResult}
	 */
	HTTPDNSResult getHttpDnsResultForHostSync(String host, RequestIpType type);
}
