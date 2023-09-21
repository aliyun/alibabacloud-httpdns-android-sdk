package com.alibaba.sdk.android.httpdns;

/**
 * 修改ttl时长的接口
 * 用于用户定制ttl，以控制缓存的时长
 */
public interface CacheTtlChanger {

	/**
	 * 根据 域名 ip类型 和 服务的ttl 返回 定制的ttl
	 *
	 * @param host 域名
	 * @param type ip类型
	 * @param ttl  服务下发的ttl 单位秒
	 * @return 定制的ttl 单位秒
	 */
	int changeCacheTtl(String host, RequestIpType type, int ttl);
}
