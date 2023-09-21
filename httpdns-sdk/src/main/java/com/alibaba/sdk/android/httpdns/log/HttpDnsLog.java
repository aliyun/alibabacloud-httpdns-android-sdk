package com.alibaba.sdk.android.httpdns.log;

import java.util.HashSet;

import com.alibaba.sdk.android.httpdns.ILogger;

import android.util.Log;

/**
 * 日志工具类
 */
public class HttpDnsLog {

	public static final String TAG = "httpdns";
	private static boolean printToLogcat = false;
	private static final HashSet<ILogger> LOGGERS = new HashSet<>();

	/**
	 * 设置日志接口
	 * 不受{@link #printToLogcat} 控制
	 */
	public static void setLogger(ILogger logger) {
		if (logger != null) {
			LOGGERS.add(logger);
		}
	}

	/**
	 * 移除日志接口
	 */
	public static void removeLogger(ILogger logger) {
		if (logger != null) {
			LOGGERS.remove(logger);
		}
	}

	/**
	 * logcat开关
	 *
	 * @param enable
	 */
	public static void enable(boolean enable) {
		HttpDnsLog.printToLogcat = enable;
	}

	public static boolean isPrint() {
		return HttpDnsLog.printToLogcat;
	}

	public static void e(String errLog) {
		if (HttpDnsLog.printToLogcat) {
			Log.e(TAG, errLog);
		}
		if (HttpDnsLog.LOGGERS.size() > 0) {
			for (ILogger logger : HttpDnsLog.LOGGERS) {
				logger.log("[E]" + errLog);
			}
		}
	}

	public static void i(String infoLog) {
		if (HttpDnsLog.printToLogcat) {
			Log.i(TAG, infoLog);
		}
		if (HttpDnsLog.LOGGERS.size() > 0) {
			for (ILogger logger : HttpDnsLog.LOGGERS) {
				logger.log("[I]" + infoLog);
			}
		}
	}

	public static void d(String debugLog) {
		if (HttpDnsLog.printToLogcat) {
			Log.d(TAG, debugLog);
		}
		if (HttpDnsLog.LOGGERS.size() > 0) {
			for (ILogger logger : HttpDnsLog.LOGGERS) {
				logger.log("[D]" + debugLog);
			}
		}
	}

	public static void w(String warnLog) {
		if (HttpDnsLog.printToLogcat) {
			Log.w(TAG, warnLog);
		}
		if (HttpDnsLog.LOGGERS.size() > 0) {
			for (ILogger logger : HttpDnsLog.LOGGERS) {
				logger.log("[W]" + warnLog);
			}
		}
	}

	public static void e(String errLog, Throwable throwable) {
		if (HttpDnsLog.printToLogcat) {
			Log.e(TAG, errLog, throwable);
		}
		if (HttpDnsLog.LOGGERS.size() > 0) {
			for (ILogger logger : HttpDnsLog.LOGGERS) {
				logger.log("[E]" + errLog);
			}
			printStackTrace(throwable);
		}
	}

	public static void w(String warnLog, Throwable throwable) {
		if (HttpDnsLog.printToLogcat) {
			Log.e(TAG, warnLog, throwable);
		}
		if (HttpDnsLog.LOGGERS.size() > 0) {
			for (ILogger logger : HttpDnsLog.LOGGERS) {
				logger.log("[W]" + warnLog);
			}
			printStackTrace(throwable);
		}
	}

	private static void printStackTrace(Throwable throwable) {
		if (HttpDnsLog.LOGGERS.size() > 0) {
			for (ILogger logger : HttpDnsLog.LOGGERS) {
				logger.log(Log.getStackTraceString(throwable));
			}
		}
	}

	/**
	 * ip数组转成字符串方便输出
	 *
	 * @param ips
	 * @return
	 */
	public static String wrap(String[] ips) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("[");
		for (int i = 0; i < ips.length; i++) {
			if (i != 0) {
				stringBuilder.append(",");
			}
			stringBuilder.append(ips[i]);
		}
		stringBuilder.append("]");
		return stringBuilder.toString();
	}
}
