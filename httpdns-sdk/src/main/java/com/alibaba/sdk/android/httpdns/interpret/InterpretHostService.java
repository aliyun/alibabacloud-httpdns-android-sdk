package com.alibaba.sdk.android.httpdns.interpret;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.impl.HostInterpretLocker;
import com.alibaba.sdk.android.httpdns.impl.HostInterpretRecorder;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.probe.ProbeCallback;
import com.alibaba.sdk.android.httpdns.probe.ProbeService;
import com.alibaba.sdk.android.httpdns.request.HttpException;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;

/**
 * 域名解析服务
 */
public class InterpretHostService {

	private final HttpDnsConfig mHttpDnsConfig;
	private final InterpretHostResultRepo mResultRepo;
	private final InterpretHostRequestHandler mRequestHandler;
	private final ProbeService mIpProbeService;
	private final HostFilter mFilter;
	private boolean mEnableExpiredIp = true;
	private final HostInterpretRecorder mRecorder;
	private final HostInterpretLocker mLocker;

	public InterpretHostService(HttpDnsConfig config, ProbeService ipProbeService,
								InterpretHostRequestHandler requestHandler,
								InterpretHostResultRepo repo, HostFilter filter,
								HostInterpretRecorder recorder) {
		this.mHttpDnsConfig = config;
		this.mIpProbeService = ipProbeService;
		this.mRequestHandler = requestHandler;
		this.mResultRepo = repo;
		this.mFilter = filter;
		this.mRecorder = recorder;
		this.mLocker = new HostInterpretLocker();
	}

	/**
	 * 解析域名
	 */
	public HTTPDNSResult interpretHostAsync(final String host, final RequestIpType type,
											final Map<String, String> extras,
											final String cacheKey) {
		if (mFilter.isFiltered(host)) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("request host " + host + ", which is filtered");
			}
			return Constants.EMPTY;
		}
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d(
				"request host " + host + " with type " + type + " extras : " + CommonUtil.toString(
					extras) + " cacheKey " + cacheKey);
		}
		HTTPDNSResult result = mResultRepo.getIps(host, type, cacheKey);
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("host " + host + " result is " + CommonUtil.toString(result));
		}
		if (result == null || result.isExpired() || result.isFromDB()) {
			if (type == RequestIpType.both) {
				// 过滤掉 未过期的请求
				HTTPDNSResult resultV4 = mResultRepo.getIps(host, RequestIpType.v4, cacheKey);
				HTTPDNSResult resultV6 = mResultRepo.getIps(host, RequestIpType.v6, cacheKey);
				boolean v4Invalid =
					resultV4 == null || resultV4.isExpired() || resultV4.isFromDB();
				boolean v6Invalid =
					resultV6 == null || resultV6.isExpired() || resultV6.isFromDB();
				if (v4Invalid && v6Invalid) {
					// 都过期，不过滤
					interpretHostInner(host, type, extras, cacheKey);
				} else if (v4Invalid) {
					// 仅v4过期
					interpretHostInner(host, RequestIpType.v4, extras, cacheKey);
				} else if (v6Invalid) {
					// 仅v6过期
					interpretHostInner(host, RequestIpType.v6, extras, cacheKey);
				}
			} else {
				interpretHostInner(host, type, extras, cacheKey);
			}
		}
		if (result != null && (!result.isExpired() || mEnableExpiredIp || result.isFromDB())) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.i(
					"request host " + host + " for " + type + " and return " + result.toString()
						+ " immediately");
			}
			return result;
		} else {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.i("request host " + host + " and return empty immediately");
			}
			return Constants.EMPTY;
		}
	}

	private void interpretHostInner(final String host, final RequestIpType type,
									Map<String, String> extras, final String cacheKey) {
		if (mRecorder.beginInterpret(host, type, cacheKey)) {
			final String region = mHttpDnsConfig.getRegion();
			mRequestHandler.requestInterpretHost(host, type, extras, cacheKey,
				new RequestCallback<InterpretHostResponse>() {
					@Override
					public void onSuccess(final InterpretHostResponse interpretHostResponse) {
						if (HttpDnsLog.isPrint()) {
							HttpDnsLog.i("ip request for " + host + " " + type + " return "
								+ interpretHostResponse.toString());
						}
						mResultRepo.save(region, host, type, interpretHostResponse.getExtras(),
							cacheKey,
							interpretHostResponse);
						if (type == RequestIpType.v4 || type == RequestIpType.both) {
							mIpProbeService.probeIpv4(host, interpretHostResponse.getIps(),
								new ProbeCallback() {
									@Override
									public void onResult(String host, String[] sortedIps) {
										if (HttpDnsLog.isPrint()) {
											HttpDnsLog.i(
												"ip probe for " + host + " " + type + " return "
													+ CommonUtil.translateStringArray(sortedIps));
										}
										mResultRepo.update(host, RequestIpType.v4, cacheKey,
											sortedIps);
									}
								});
						}
						mRecorder.endInterpret(host, type, cacheKey);
					}

					@Override
					public void onFail(Throwable throwable) {
						HttpDnsLog.w("ip request for " + host + " fail", throwable);
						if (throwable instanceof HttpException
							&& ((HttpException)throwable).shouldCreateEmptyCache()) {
							InterpretHostResponse emptyResponse =
								InterpretHostResponse.createEmpty(
									host, 60 * 60);
							mResultRepo.save(region, host, type, emptyResponse.getExtras(),
								cacheKey,
								emptyResponse);
						}
						mRecorder.endInterpret(host, type, cacheKey);
					}
				});
		}
	}

	/**
	 * 解析域名
	 */
	public HTTPDNSResult interpretHost(final String host, final RequestIpType type,
									   final Map<String, String> extras, final String cacheKey) {
		if (mFilter.isFiltered(host)) {
			HttpDnsLog.d("request host " + host + ", which is filtered");
			return Constants.EMPTY;
		}
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("request host " + host + " sync with type " + type + " extras : "
				+ CommonUtil.toString(extras) + " cacheKey " + cacheKey);
		}
		HTTPDNSResult result = mResultRepo.getIps(host, type, cacheKey);
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("host " + host + " result is " + CommonUtil.toString(result));
		}
		if (result == null || result.isExpired() || result.isFromDB()) {
			// 没有缓存，或者缓存过期，或者是从数据库读取的 需要解析
			if (type == RequestIpType.both) {
				// 过滤掉 未过期的请求
				HTTPDNSResult resultV4 = mResultRepo.getIps(host, RequestIpType.v4, cacheKey);
				HTTPDNSResult resultV6 = mResultRepo.getIps(host, RequestIpType.v6, cacheKey);
				boolean v4Invalid =
					resultV4 == null || resultV4.isExpired() || resultV4.isFromDB();
				boolean v6Invalid =
					resultV6 == null || resultV6.isExpired() || resultV6.isFromDB();
				if (v4Invalid && v6Invalid) {
					// 都过期，不过滤
					syncInterpretHostInner(host, type, extras, cacheKey, result);
				} else if (v4Invalid) {
					// 仅v4过期
					syncInterpretHostInner(host, RequestIpType.v4, extras, cacheKey, result);
				} else if (v6Invalid) {
					// 仅v6过期
					syncInterpretHostInner(host, RequestIpType.v6, extras, cacheKey, result);
				}
			} else {
				syncInterpretHostInner(host, type, extras, cacheKey, result);
			}
		} else {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.i(
					"request host " + host + " for " + type + " and return " + result.toString()
						+ " immediately");
			}
			return result;
		}

		result = mResultRepo.getIps(host, type, cacheKey);
		if (result != null && (!result.isExpired() || mEnableExpiredIp || result.isFromDB())) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.i(
					"request host " + host + " for " + type + " and return " + result.toString()
						+ " after request");
			}
			return result;
		} else {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.i("request host " + host + " and return empty after request");
			}
			return Constants.EMPTY;
		}
	}

	private void syncInterpretHostInner(final String host, final RequestIpType type,
										Map<String, String> extras, final String cacheKey,
										HTTPDNSResult result) {
		if (mLocker.beginInterpret(host, type, cacheKey)) {
			final String region = mHttpDnsConfig.getRegion();
			// 没有正在进行的解析，发起新的解析
			mRequestHandler.requestInterpretHost(host, type, extras, cacheKey,
				new RequestCallback<InterpretHostResponse>() {
					@Override
					public void onSuccess(final InterpretHostResponse interpretHostResponse) {
						if (HttpDnsLog.isPrint()) {
							HttpDnsLog.i("ip request for " + host + " " + type + " return "
								+ interpretHostResponse.toString());
						}
						mResultRepo.save(region, host, type, interpretHostResponse.getExtras(),
							cacheKey,
							interpretHostResponse);
						if (type == RequestIpType.v4 || type == RequestIpType.both) {
							mIpProbeService.probeIpv4(host, interpretHostResponse.getIps(),
								new ProbeCallback() {
									@Override
									public void onResult(String host, String[] sortedIps) {
										if (HttpDnsLog.isPrint()) {
											HttpDnsLog.i(
												"ip probe for " + host + " " + type + " return "
													+ CommonUtil.translateStringArray(sortedIps));
										}
										mResultRepo.update(host, RequestIpType.v4, cacheKey,
											sortedIps);
									}
								});
						}
						mLocker.endInterpret(host, type, cacheKey);
					}

					@Override
					public void onFail(Throwable throwable) {
						HttpDnsLog.w("ip request for " + host + " fail", throwable);
						if (throwable instanceof HttpException
							&& ((HttpException)throwable).shouldCreateEmptyCache()) {
							InterpretHostResponse emptyResponse =
								InterpretHostResponse.createEmpty(
									host, 60 * 60);
							mResultRepo.save(region, host, type, emptyResponse.getExtras(),
								cacheKey,
								emptyResponse);
						}
						mLocker.endInterpret(host, type, cacheKey);
					}
				});
		}

		if (result == null || !mEnableExpiredIp) {
			// 有结果，但是过期了，不允许返回过期结果，等请求结束
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("wait for request finish");
			}
			try {
				mLocker.await(host, type, cacheKey, 15, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// 不管什么情况释放锁
			mLocker.endInterpret(host, type, cacheKey);
		}
	}

	public void setEnableExpiredIp(boolean enableExpiredIp) {
		this.mEnableExpiredIp = enableExpiredIp;
	}

}
