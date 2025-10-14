package com.alibaba.sdk.android.httpdns;

import android.content.Context;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsInstanceHolder;
import com.alibaba.sdk.android.httpdns.impl.InstanceCreator;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

/**
 * Httpdns实例管理
 */
public class HttpDns {

	private static final HttpDnsInstanceHolder holder = new HttpDnsInstanceHolder(
		new InstanceCreator());

	/**
	 * 获取HttpDnsService对象
	 * @param accountId HttpDns控制台分配的AccountID
	 * @return
	 */
	public synchronized static HttpDnsService getService(final String accountId) {
		return holder.get(null, accountId, null);
	}


	/**
	 * 获取HttpDnsService对象
	 * 该方法已弃用，建议使用{@link HttpDns#getService(String)}方法
	 * @param applicationContext 当前APP的Context
	 * @param accountID          HttpDns控制台分配的AccountID
	 * @return
	 */
	@Deprecated
	public synchronized static HttpDnsService getService(final Context applicationContext,
														 final String accountID) {
		return holder.get(applicationContext, accountID, null);
	}

	/**
	 * 获取HttpDnsService对象，并启用鉴权功能
	 * 该方法已弃用，建议使用{@link HttpDns#getService(String)}方法
	 * @param applicationContext 当前APP的Context
	 * @param accountID          HttpDns控制台分配的AccountID
	 * @param secretKey          用户鉴权私钥
	 * @return
	 */
	@Deprecated
	public synchronized static HttpDnsService getService(final Context applicationContext,
														 final String accountID,
														 final String secretKey) {
		return holder.get(applicationContext, accountID, secretKey);
	}

	/**
	 * 获取HttpDnsService对象，初始化时不传入任何参数，靠统一接入服务获取相关参数
	 * 该方法已弃用，建议使用{@link HttpDns#getService(String)}方法
	 * @param applicationContext 当前APP的Context
	 * @return
	 */
	@Deprecated
	public synchronized static HttpDnsService getService(final Context applicationContext) {
		return holder.get(applicationContext, CommonUtil.getAccountId(applicationContext),
			CommonUtil.getSecretKey(applicationContext));
	}

	/**
	 * 初始化方法，该方法主要是保存{@link InitConfig}，不会真正进行初始化。真正初始化是在{@link HttpDns#getService(Context, String)}中
	 * 这么实现主要是为了兼容{@link InitConfig.Builder#buildFor(String)}方法，新客户使用该方法和旧的方法功能一致
	 * @param accountId HttpDns控制台分配的AccountID
	 * @param config {@link InitConfig}
	 */
	public static void init(String accountId, InitConfig config) {
		InitConfig.addConfig(accountId, config);
	}

	/**
	 * 启用或者禁用httpdns，理论上这个是内部接口，不给外部使用的
	 * 但是已经对外暴露，所以保留
	 *
	 * @param enabled
	 * @deprecated 启用禁用应该调用实例的方法，而不是控制全部实例的方法
	 */
	@Deprecated
	public synchronized static void switchDnsService(boolean enabled) {
		// do nothing as deprecated
	}
}
