package com.alibaba.sdk.android.httpdns.request;

import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 网络请求失败的异常
 */
public class HttpException extends Exception {
	public static final int ERROR_CODE_403 = 403;
	public static final int ERROR_CODE_400 = 400;
	public static final int ERROR_CODE_500 = 500;

	/**
	 * 用户服务level不匹配
	 * （用户有不同的level，高level有一些专用的服务节点，如果一个低level的用户，向一个高level专用的服务节点请求，就会返回此错误）
	 * <p>
	 * 需要重试 切换服务IP
	 */
	public static final String ERROR_MSG_SERVICE_LEVEL_DENY = "ServiceLevelDeny";

	/**
	 * 未用签名访问
	 * 不重试 不切换
	 * 生成空解析 缓存1小时
	 */
	public static final String ERROR_MSG_UNSIGNED = "UnsignedInterfaceDisabled";

	/**
	 * 签名过期
	 * 不重试 不切换 （sdk逻辑保证不应该过期）
	 */
	public static final String ERROR_MSG_SIGNATURE_EXPIRED = "SignatureExpired";

	/**
	 * 签名验证失败
	 * 不重试 不切换
	 */
	public static final String ERROR_MSG_INVALID_SIGNATURE = "InvalidSignature";

	/**
	 * 账户服务level缺失
	 * 不重试 不切换
	 * 生成空解析 缓存1小时
	 */
	public static final String ERROR_MSG_INVALID_ACCOUNT = "InvalidAccount";

	/**
	 * 账户不存在或者禁用
	 * 不重试 不切换
	 * 生成空解析 缓存1小时
	 */
	public static final String ERROR_MSG_ACCOUNT_NOT_EXISTS = "AccountNotExists";

	/**
	 * 签名有效时间过长
	 * 不重试 不切换
	 */
	public static final String ERROR_MSG_INVALID_DURATION = "InvalidDuration";

	/**
	 * 无效域名
	 * 不重试 不切换
	 */
	public static final String ERROR_MSG_INVALID_HOST = "InvalidHost";

	public static final String ERROR_MSG_TAG = "code";
	private final int mCode;

	private HttpException(int code, String message) {
		super(message);
		this.mCode = code;
	}

	/**
	 * Http status code
	 */
	public int getCode() {
		return mCode;
	}

	/**
	 * 创建异常
	 *
	 * @param code
	 * @param message 服务器返回的特定格式的数据
	 * @return
	 */
	public static HttpException create(int code, String message) {
		return new HttpException(code, tryTranslateMessage(message));
	}

	private static String tryTranslateMessage(String message) {
		JSONObject object;
		try {
			object = new JSONObject(message);
			return object.getString(ERROR_MSG_TAG);
		} catch (JSONException ignored) {
		}
		return message;
	}

	@Override
	public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
		HttpException that = (HttpException)o;
		return mCode == that.mCode && getMessage().equals(that.getMessage());
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] {mCode, getMessage()});
	}

	/**
	 * 是不是要切换服务IP
	 */
	public boolean shouldShiftServer() {
		String msg = getMessage();
        return !ERROR_MSG_UNSIGNED.equals(msg)
            && !ERROR_MSG_SIGNATURE_EXPIRED.equals(msg)
            && !ERROR_MSG_INVALID_SIGNATURE.equals(msg)
            && !ERROR_MSG_INVALID_ACCOUNT.equals(msg)
            && !ERROR_MSG_ACCOUNT_NOT_EXISTS.equals(msg)
            && !ERROR_MSG_INVALID_DURATION.equals(msg)
            && !ERROR_MSG_INVALID_HOST.equals(msg);
	}

	/**
	 * 这个错误是否建议重试
	 */
	public boolean shouldRetry() {
		String msg = getMessage();
        return !ERROR_MSG_UNSIGNED.equals(msg)
            && !ERROR_MSG_SIGNATURE_EXPIRED.equals(msg)
            && !ERROR_MSG_INVALID_SIGNATURE.equals(msg)
            && !ERROR_MSG_INVALID_ACCOUNT.equals(msg)
            && !ERROR_MSG_ACCOUNT_NOT_EXISTS.equals(msg)
            && !ERROR_MSG_INVALID_DURATION.equals(msg)
            && !ERROR_MSG_INVALID_HOST.equals(msg);
	}

	/**
	 * 这个错误是否要创建空缓存
	 */
	public boolean shouldCreateEmptyCache() {
		String msg = getMessage();
        return ERROR_MSG_UNSIGNED.equals(msg)
            || ERROR_MSG_INVALID_ACCOUNT.equals(msg)
            || ERROR_MSG_ACCOUNT_NOT_EXISTS.equals(msg);
	}
}
