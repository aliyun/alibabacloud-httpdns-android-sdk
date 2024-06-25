package com.alibaba.sdk.android.httpdns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HttpDns服务接口
 */
public interface HttpDnsService {
	/**
	 * 设置预解析域名列表，默认解析ipv4
	 *
	 * @param hostList 预解析的host域名列表
	 */
	void setPreResolveHosts(List<String> hostList);

	/**
	 * 设置预解析域名列表和解析的ip类型
	 *
	 * @param hostList 预解析的host列表
	 * @param requestIpType {@link RequestIpType} 网络栈类型，v4,v6,both,auto(根据当前设备所连的网络自动判断网络栈)
	 */
	void setPreResolveHosts(List<String> hostList, RequestIpType requestIpType);

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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
	HTTPDNSResult getHttpDnsResultForHostAsync(String host, RequestIpType type);

	/***
	 * 校正App签名时间
	 * @param time time为epoch时间戳，1970年1月1日以来的秒数
	 */
	void setAuthCurrentTime(long time);

	/**
	 * 获取会话id
	 *
	 * @return sid
	 */
	String getSessionId();

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
	@Deprecated
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
	 * @param params 自定义解析参数
	 * @param cacheKey 缓存的key
	 * @return {@link HTTPDNSResult}
	 */
	@Deprecated
	HTTPDNSResult getHttpDnsResultForHostAsync(String host, RequestIpType type, Map<String,
		String> params, String cacheKey);

	/**
	 * 同步解析接口，支持指定解析类型
	 * 支持配置sdns参数
	 * 需要注意的地方:
	 * 1. 该方法必须在子线程中执行，如果在主线程中调用该方法，方法内部会自动切换成异步执行
	 * 2. 同步接口会阻塞当前子线程，阻塞时长可以通过{@link InitConfig.Builder#setTimeout(int)}设置，
	 * 不过阻塞时长的上限是5s，如果设置的超时时长超过5s则无效
	 *
	 * @param host 要解析的host域名列表
	 * @param type {@link RequestIpType}
	 * @param params 自定义解析参数
	 * @param cacheKey 缓存的key
	 * @return {@link HTTPDNSResult}
	 */
	HTTPDNSResult getHttpDnsResultForHostSync(String host, RequestIpType type, Map<String,
			String> params, String cacheKey);

	/**
	 * 异步解析接口, 获取ipv4 + ipv6列表, 通过回调返回解析结果，首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 * 支持配置sdns参数
	 * 支持指定解析类型
	 *
	 * @param host 要解析的host域名
	 * @param type {@link RequestIpType} 网络栈类型，v4,v6,both,auto(根据当前设备所连的网络自动判断网络栈)
	 * @param params 自定义解析参数
	 * @param cacheKey 缓存的key
	 */
	void getHttpDnsResultForHostAsync(String host, RequestIpType type, Map<String,
			String> params, String cacheKey, HttpDnsCallback callback);

	/**
	 * 异步解析接口, 获取ipv4 + ipv6列表, 首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 * 支持配置sdns参数
	 * 支持指定解析类型
	 *
	 * @param host 要解析的host域名
	 * @param type {@link RequestIpType} 网络栈类型，v4,v6,both,auto(根据当前设备所连的网络自动判断网络栈)
	 * @param params 自定义解析参数
	 * @param cacheKey 缓存的key
	 */
	HTTPDNSResult getHttpDnsResultForHostSyncNonBlocking(String host, RequestIpType type, Map<String,
			String> params, String cacheKey);

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
	 * 同步解析接口，支持指定解析类型
	 * 需要注意的地方:
	 * 1. 该方法必须在子线程中执行，如果在主线程中调用该方法，方法内部会自动切换成异步执行
	 * 2. 同步接口会阻塞当前子线程，阻塞时长可以通过{@link InitConfig.Builder#setTimeout(int)}设置，
	 * 不过阻塞时长的上限是5s，如果设置的超时时长超过5s则无效
	 *
	 * @param host 要解析的host域名列表
	 * @param type {@link RequestIpType}
	 * @return {@link HTTPDNSResult}
	 */
	HTTPDNSResult getHttpDnsResultForHostSync(String host, RequestIpType type);

	/**
	 * 异步解析接口，根据type获取ip, 通过回调返回解析结果，首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 * 支持 指定解析IP类型
	 *
	 * @param host 要解析的host域名
	 * @param type {@link RequestIpType} 网络栈类型，v4,v6,both,auto(根据当前设备所连的网络自动判断网络栈)
	 */
	void getHttpDnsResultForHostAsync(String host, RequestIpType type, HttpDnsCallback callback);

	/**
	 * 异步解析接口，根据type获取ip, 首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求（不会通过回调给调用方）
	 * 支持 指定解析IP类型
	 *
	 * @param host 要解析的host域名
	 * @param type {@link RequestIpType} 网络栈类型，v4,v6,both,auto(根据当前设备所连的网络自动判断网络栈)
	 * @return {@link HTTPDNSResult}
	 */
	HTTPDNSResult getHttpDnsResultForHostSyncNonBlocking(String host, RequestIpType type);
}
