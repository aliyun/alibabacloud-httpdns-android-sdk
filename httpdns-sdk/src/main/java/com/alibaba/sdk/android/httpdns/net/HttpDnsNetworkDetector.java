package com.alibaba.sdk.android.httpdns.net;

import java.util.concurrent.ExecutorService;

import com.alibaba.sdk.android.httpdns.HttpDnsSettings;
import com.alibaba.sdk.android.httpdns.NetType;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.utils.ThreadUtil;

import android.content.Context;
import com.aliyun.ams.ipdetector.Inet64Util;

public class HttpDnsNetworkDetector implements HttpDnsSettings.NetworkDetector {

	private static class Holder {
		private static final HttpDnsNetworkDetector INSTANCE = new HttpDnsNetworkDetector();
	}

	public static HttpDnsNetworkDetector getInstance() {
		return Holder.INSTANCE;
	}

	private HttpDnsNetworkDetector() {
	}

	private final ExecutorService mWorker = ThreadUtil.createSingleThreadService("NetType");
	private boolean mCheckInterface = true;
	private String mHostToCheckNetType = "www.taobao.com";
	private NetType mCache = NetType.none;
	private boolean mDisableCache = false;
	private Context mContext;

	/**
	 * 网络变化时，清除缓存
	 */
	public void cleanCache(final boolean connected) {
		if (mDisableCache) {
			return;
		}
		mCache = NetType.none;
		if (connected && this.mContext != null) {
			mWorker.execute(new Runnable() {
				@Override
				public void run() {
					// 异步探测一下
					if (mContext != null) {
						mCache = detectNetType(mContext);
					}
				}
			});
		}
	}

	/**
	 * 是否禁用缓存，默认不禁用
	 * 不确定是否存在网络链接不变的情况下，网络情况会发生变化的情况，所以提供了此开关
	 */
	public void disableCache(boolean disable) {
		this.mDisableCache = disable;
	}

	/**
	 * 如果不能检查本地网关ip,可以调用此接口关闭
	 */
	public void setCheckInterface(boolean checkInterface) {
		this.mCheckInterface = checkInterface;
	}

	/**
	 * 有些场景需要通过本地解析来确认网络类型，默认使用 www.taobao.com
	 */
	public void setHostToCheckNetType(String hostToCheckNetType) {
		this.mHostToCheckNetType = hostToCheckNetType;
	}

	@Override
	public NetType getNetType(Context context) {
		if (mDisableCache) {
			NetType tmp = detectNetType(context);
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("ipdetector type is " + tmp.name());
			}
			return tmp;
		}
		if (mCache != NetType.none) {
			return mCache;
		}
		mCache = detectNetType(context);
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("ipdetector type is " + mCache.name());
		}
		return mCache;
	}

	private NetType detectNetType(Context context) {
		this.mContext = context.getApplicationContext();
		try {
			int type;// 不检查本地IP的情况下，无法过滤ipv6只有本地ip的情况，需要通过其它方式检测下。
			// 没有网络？
			if (mCheckInterface) {
				type = Inet64Util.getIpStack(context);
			} else {
				type = Inet64Util.getIpStackCheckLocal(context);
			}
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("ip detector type is " + type);
			}
			if (type == IP_DUAL_STACK) {
				return NetType.both;
			} else if (type == IPV4_ONLY) {
				return NetType.v4;
			} else if (type == IPV6_ONLY) {
				return NetType.v6;
			} else {
				// 没有网络？
				return NetType.none;
			}

		} catch (Throwable e) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.i("ip detector not exist.");
			}
			return NetType.none;
		}
	}

	private final static int IPV4_ONLY = 1;
	private final static int IPV6_ONLY = 2;
	private final static int IP_DUAL_STACK = 3;

}
