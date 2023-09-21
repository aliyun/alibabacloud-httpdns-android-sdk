package com.alibaba.sdk.android.httpdns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.sdk.android.httpdns.net64.Net64Service;
import com.alibaba.sdk.android.httpdns.probe.IPProbeItem;

/**
 * HttpDns服务接口
 */
public interface HttpDnsService extends Net64Service {

	/**
	 * 设置是否打印log, 仅供调试, 正式发布时请勿开启
	 *
	 * @param shouldPrintLog
	 * @deprecated 请直接使用 {@link com.alibaba.sdk.android.httpdns.log.HttpDnsLog}
	 */
	@Deprecated
	void setLogEnabled(boolean shouldPrintLog);

	/**
	 * 设置预解析域名列表，默认解析ipv4
	 *
	 * @param hostList
	 */
	void setPreResolveHosts(ArrayList<String> hostList);

	/**
	 * 设置预解析域名列表和解析的ip类型
	 *
	 * @param hostList
	 * @param requestIpType
	 */
	void setPreResolveHosts(ArrayList<String> hostList, RequestIpType requestIpType);

	/**
	 * 异步解析接口, 首先查询缓存, 若存在则返回结果, 若不存在返回null 并且进行异步域名解析请求
	 *
	 * @param host 如www.aliyun.com
	 * @return 返回ip, 如果没得到解析结果, 则返回null
	 */
	String getIpByHostAsync(String host);

	/**
	 * 异步解析接口, 获取ip列表
	 *
	 * @param host
	 * @return 返回String 数组, 如果没得到解析结果, 则String 数组的长度为0
	 */
	String[] getIpsByHostAsync(String host);

	/**
	 * 异步解析接口，获取ipv6列表
	 *
	 * @param host
	 * @return
	 */
	String[] getIPv6sByHostAsync(String host);

	/**
	 * 异步解析接口，获取ipv4ipv6列表
	 *
	 * @param host
	 * @return
	 */
	HTTPDNSResult getAllByHostAsync(String host);

	/**
	 * 异步解析接口，获取ipv4ipv6列表
	 * 支持 指定解析IP类型
	 *
	 * @return
	 */
	HTTPDNSResult getIpsByHostAsync(String host, RequestIpType type);

	/**
	 * 设置是否允许返回超过ttl 的ip
	 *
	 * @param enable
	 */
	void setExpiredIPEnabled(boolean enable);

	/**
	 * 设置是否允许使用DB缓存
	 *
	 * @param enable
	 */
	void setCachedIPEnabled(boolean enable);

	/**
	 * 设置是否允许使用DB缓存
	 *
	 * @param enable
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
	 * @param filter
	 */
	void setDegradationFilter(DegradationFilter filter);

	/**
	 * 设置网络切换时是否自动刷新所有域名解析结果
	 *
	 * @param enable
	 */

	void setPreResolveAfterNetworkChanged(boolean enable);

	/**
	 * 设置请求超时时间,单位ms,默认为15S
	 *
	 * @param timeoutInterval
	 */
	void setTimeoutInterval(int timeoutInterval);

	/**
	 * 设置HTTPDNS域名解析请求类型(HTTP/HTTPS)，若不调用该接口，默认为HTTP请求
	 *
	 * @param enabled
	 */
	void setHTTPSRequestEnabled(boolean enabled);

	/**
	 * 设置要探测的域名列表,默认只会对ipv4的地址进行ip优选
	 *
	 * @param ipProbeList
	 */
	void setIPProbeList(List<IPProbeItem> ipProbeList);

	/**
	 * 获取会话id
	 *
	 * @return sid
	 */
	String getSessionId();

	/**
	 * 设置用于接收sdk日志的回调类
	 *
	 * @param logger
	 * @deprecated 请直接使用 {@link com.alibaba.sdk.android.httpdns.log.HttpDnsLog}
	 */
	@Deprecated
	void setLogger(ILogger logger);

	//以下针对SDNS

	/**
	 * 异步解析接口, 获取ip列表
	 * 支持配置sdns参数
	 *
	 * @return 返回HTTPDNSResult对象
	 * @host host
	 */
	HTTPDNSResult getIpsByHostAsync(String host, Map<String, String> params, String cacheKey);

	/**
	 * 异步解析接口, 获取ip列表
	 * 支持配置sdns参数
	 * 支持指定解析类型
	 *
	 * @param host
	 * @param type
	 * @param params
	 * @param cacheKey
	 * @return
	 */
	HTTPDNSResult getIpsByHostAsync(String host, RequestIpType type, Map<String, String> params,
									String cacheKey);

	/**
	 * 设置sdns全局参数（该全局参数不影响异步解析任务，只用于解析接口调用时进行参数合并）
	 *
	 * @param params
	 */
	void setSdnsGlobalParams(Map<String, String> params);

	/**
	 * 清除sdns全局参数
	 */
	void clearSdnsGlobalParams();

	//以上针对SDNS

	/**
	 * 设置region，海外节点
	 *
	 * @param region
	 */
	void setRegion(String region);

	/**
	 * 立即清除域名端侧内存和本地缓存。
	 * 后续调用异步接口，会先返回空，触发域名解析
	 *
	 * @param hosts
	 */
	void cleanHostCache(ArrayList<String> hosts);

	/**
	 * 是否开启sdk内部的崩溃保护机制，默认是关闭的
	 *
	 * @param enabled
	 */
	void enableCrashDefend(boolean enabled);
}
