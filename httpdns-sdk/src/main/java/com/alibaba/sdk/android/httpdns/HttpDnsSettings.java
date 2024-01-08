package com.alibaba.sdk.android.httpdns;

import android.content.Context;

public class HttpDnsSettings {

	private static boolean sDailyReport = true;

	public static void setDailyReport(boolean dailyReport) {
		HttpDnsSettings.sDailyReport = dailyReport;
	}

	public static boolean isDailyReport() {
		return sDailyReport;
	}

	private static NetworkChecker checker = new NetworkChecker() {
		@Override
		public boolean isIpv6Only() {
			return false;
		}
	};

	@Deprecated
	public static void setNetworkChecker(NetworkChecker checker) {
		HttpDnsSettings.checker = checker;
	}

	@Deprecated
	public static NetworkChecker getChecker() {
		return checker;
	}

	/**
	 * 需要外部注入的一些网络环境判断
	 */
	public interface NetworkChecker {
		boolean isIpv6Only();
	}

	/**
	 * 获取网络类型的接口
	 */
	public interface NetworkDetector {
		NetType getNetType(Context context);
	}
}
