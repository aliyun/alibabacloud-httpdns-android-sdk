package com.alibaba.sdk.android.httpdns.resolve;

import android.text.TextUtils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.HTTPDNSResultWrapper;
import com.alibaba.sdk.android.httpdns.HttpDnsCallback;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.impl.HostResolveLocker;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.observable.ObservableConstants;
import com.alibaba.sdk.android.httpdns.observable.event.CallSdkApiEvent;
import com.alibaba.sdk.android.httpdns.ranking.IPRankingCallback;
import com.alibaba.sdk.android.httpdns.ranking.IPRankingService;
import com.alibaba.sdk.android.httpdns.request.HttpException;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;

/**
 * 域名解析服务
 */
public class ResolveHostService {

	private final HttpDnsConfig mHttpDnsConfig;
	private final ResolveHostResultRepo mResultRepo;
	private final ResolveHostRequestHandler mRequestHandler;
	private final IPRankingService mIpIPRankingService;
	private final HostFilter mFilter;
	private boolean mEnableExpiredIp = true;
	private final HostResolveLocker mAsyncLocker;
	private final HostResolveLocker mLocker;

	public ResolveHostService(HttpDnsConfig config, IPRankingService ipIPRankingService,
							  ResolveHostRequestHandler requestHandler,
							  ResolveHostResultRepo repo, HostFilter filter,
							  HostResolveLocker locker) {
		this.mHttpDnsConfig = config;
		this.mIpIPRankingService = ipIPRankingService;
		this.mRequestHandler = requestHandler;
		this.mResultRepo = repo;
		this.mFilter = filter;
		this.mAsyncLocker = locker;
		this.mLocker = new HostResolveLocker();
	}

	/**
	 * 解析域名
	 */
	public HTTPDNSResult resolveHostSyncNonBlocking(final String host, final RequestIpType type,
													final Map<String, String> extras,
													final String cacheKey) {
		if (mFilter.isFiltered(host)) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("request host " + host + ", which is filtered");
			}
			return Constants.EMPTY;
		}

		long start = System.currentTimeMillis();

		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d(
				"sync non blocking request host " + host + " with type " + type + " extras : " + CommonUtil.toString(
					extras) + " cacheKey " + cacheKey);
		}
		HTTPDNSResultWrapper result = mResultRepo.getIps(host, type, cacheKey);
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("host " + host + " result is " + CommonUtil.toString(result));
		}
		if (result == null || result.isExpired()) {
			if (type == RequestIpType.both) {
				// 过滤掉 未过期的请求
				HTTPDNSResultWrapper resultV4 = mResultRepo.getIps(host, RequestIpType.v4, cacheKey);
				HTTPDNSResultWrapper resultV6 = mResultRepo.getIps(host, RequestIpType.v6, cacheKey);
				boolean v4Invalid =
					resultV4 == null || resultV4.isExpired();
				boolean v6Invalid =
					resultV6 == null || resultV6.isExpired();
				if (v4Invalid && v6Invalid) {
					// 都过期，不过滤
					asyncResolveHostInner(host, type, extras, cacheKey);
				} else if (v4Invalid) {
					// 仅v4过期
					asyncResolveHostInner(host, RequestIpType.v4, extras, cacheKey);
				} else if (v6Invalid) {
					// 仅v6过期
					asyncResolveHostInner(host, RequestIpType.v6, extras, cacheKey);
				}
			} else {
				asyncResolveHostInner(host, type, extras, cacheKey);
			}
		}
		if (result != null && (!result.isExpired() || mEnableExpiredIp)) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.i(
					"request host " + host + " for " + type + " and return " + result.toString()
						+ " immediately");
			}

			addCallSdkApiEvent(getCallSdkApiEvent(ObservableConstants.RESOLVE_API_SYN_NON_BLOCKING, host, type, true, result, start),
					result);
			return result.getHTTPDNSResult();
		} else {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.i("request host " + host + " and return empty immediately");
			}

			addCallSdkApiEvent(getCallSdkApiEvent(ObservableConstants.RESOLVE_API_SYN_NON_BLOCKING, host, type, false, result, start),
					null);
			return Constants.EMPTY;
		}
	}

	private void asyncResolveHostInner(final String host, final RequestIpType type,
								  Map<String, String> extras, final String cacheKey) {
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.i("async start request for " + host + " " + type);
		}

		if (mAsyncLocker.beginResolve(host, type, cacheKey)) {
			final String region = mHttpDnsConfig.getRegion();
			mRequestHandler.requestResolveHost(host, type, extras, cacheKey,
				new RequestCallback<ResolveHostResponse>() {
					@Override
					public void onSuccess(final ResolveHostResponse resolveHostResponse) {
						if (HttpDnsLog.isPrint()) {
							HttpDnsLog.i("ip request for " + host + " " + type + " return "
								+ resolveHostResponse.toString());
						}
						mResultRepo.save(region, resolveHostResponse, cacheKey);

						if (type == RequestIpType.v4 || type == RequestIpType.both) {
							for (final ResolveHostResponse.HostItem item :
									resolveHostResponse.getItems()) {
								if (item.getIpType() == RequestIpType.v4) {
									mIpIPRankingService.probeIpv4(host, item.getIps(),
											new IPRankingCallback() {
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
							}

						}
						mAsyncLocker.endResolve(host, type, cacheKey);
					}

					@Override
					public void onFail(Throwable throwable) {
						HttpDnsLog.w("ip request for " + host + " fail", throwable);
						if (throwable instanceof Exception) {
							String query = "4";
							if (type == RequestIpType.v6) {
								query = "6";
							}else if (type == RequestIpType.both) {
								query = "4,6";
							}
							String errorMsg = (throwable instanceof HttpException) ? throwable.getMessage() : throwable.toString();
							HttpDnsLog.w("RESOLVE FAIL, HOST:" + host + ", QUERY:" + query
									+ ", Msg:" + errorMsg);
						}
						if (throwable instanceof HttpException
							&& ((HttpException)throwable).shouldCreateEmptyCache()) {
							ArrayList<String> targetHost = new ArrayList<>();
							targetHost.add(host);
							ResolveHostResponse emptyResponse = ResolveHostResponse.createEmpty(
									targetHost, type, 60 * 60);
							mResultRepo.save(region, emptyResponse, cacheKey);
						}

						mAsyncLocker.endResolve(host, type, cacheKey);
					}
				});
		}
	}

	/**
	 * 解析域名
	 */
	public HTTPDNSResult resolveHostSync(final String host, final RequestIpType type,
										 final Map<String, String> extras, final String cacheKey) {
		if (mFilter.isFiltered(host)) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("request host " + host + ", which is filtered");
			}
			return degradationLocalDns(host);
		}

		long start = System.currentTimeMillis();

		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("sync request host " + host + " with type " + type + " extras : "
				+ CommonUtil.toString(extras) + " cacheKey " + cacheKey);
		}
		HTTPDNSResultWrapper result = mResultRepo.getIps(host, type, cacheKey);
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("host " + host + " result in cache is " + CommonUtil.toString(result));
		}

		if (result != null && (!result.isExpired() || mEnableExpiredIp)) {
			//有缓存，如果没过期或者允许使用过期解析结果，直接返回
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.i(
						"request host " + host + " for " + type + " and return " + result.toString()
								+ " immediately");
			}

			if (result.isExpired()) {
				//如果缓存已经过期了，发起异步解析更新缓存
				asyncResolveHostInner(host, type, extras, cacheKey);
			}

			//可能是空结果，如果开启降级local dns，走local dns解析
			if ((result.getIps() == null || result.getIps().length == 0)
					&& (result.getIpv6s() == null || result.getIpv6s().length == 0)) {
				if (mHttpDnsConfig.isEnableDegradationLocalDns()) {
					return degradationLocalDns(host);
				}
			}

			addCallSdkApiEvent(getCallSdkApiEvent(ObservableConstants.RESOLVE_API_SYNC, host, type, true, result, start), result);

			return result.getHTTPDNSResult();
		}

		//如果没有缓存，或者有过期缓存但是不允许使用过期缓存
		if (result == null || result.isExpired()) {
			// 没有缓存，或者缓存过期，或者是从数据库读取的 需要解析
			if (type == RequestIpType.both) {
				// 过滤掉 未过期的请求
				HTTPDNSResultWrapper resultV4 = mResultRepo.getIps(host, RequestIpType.v4, cacheKey);
				HTTPDNSResultWrapper resultV6 = mResultRepo.getIps(host, RequestIpType.v6, cacheKey);
				boolean v4Invalid =
					resultV4 == null || resultV4.isExpired();
				boolean v6Invalid =
					resultV6 == null || resultV6.isExpired();
				if (v4Invalid && v6Invalid) {
					// 都过期，不过滤
					syncResolveHostInner(host, type, extras, cacheKey, result);
				} else if (v4Invalid) {
					// 仅v4过期
					syncResolveHostInner(host, RequestIpType.v4, extras, cacheKey, result);
				} else if (v6Invalid) {
					// 仅v6过期
					syncResolveHostInner(host, RequestIpType.v6, extras, cacheKey, result);
				}
			} else {
				syncResolveHostInner(host, type, extras, cacheKey, result);
			}
		}

		CallSdkApiEvent callSdkApiEvent = getCallSdkApiEvent(ObservableConstants.RESOLVE_API_SYNC, host, type, false, result, start);
		result = mResultRepo.getIps(host, type, cacheKey);

		if (result != null && (!result.isExpired() || mEnableExpiredIp)) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.i(
					"request host " + host + " for " + type + " and return " + result.toString()
						+ " after request");
			}

			//可能是空结果，如果开启降级local dns，走local dns解析
			if ((result.getIps() == null || result.getIps().length == 0)
					&& (result.getIpv6s() == null || result.getIpv6s().length == 0)) {
				if (mHttpDnsConfig.isEnableDegradationLocalDns()) {
					return degradationLocalDns(host);
				}
			}

			addCallSdkApiEvent(callSdkApiEvent, result);
			return result.getHTTPDNSResult();
		} else {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.i("request host " + host + " and return empty after request");
			}

			addCallSdkApiEvent(callSdkApiEvent, null);
			return degradationLocalDns(host);
		}
	}

	private HTTPDNSResult degradationLocalDns(String host) {
		if (mHttpDnsConfig.isEnableDegradationLocalDns()) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("request host " + host + " via local dns");
			}
			try {
				InetAddress[] addresses = InetAddress.getAllByName(host);
				if (addresses != null && addresses.length > 0) {
					List<String> ips = new ArrayList<>();
					List<String> ipv6s = new ArrayList<>();
					for (InetAddress address : addresses) {
						if (address instanceof Inet4Address) {
							ips.add(address.getHostAddress());
						} else if (address instanceof Inet6Address) {
							ipv6s.add(address.getHostAddress());
						}
					}

					HTTPDNSResult result = new HTTPDNSResult(host, ips.toArray(new String[0]), ipv6s.toArray(new String[0]), null, false, false, true);

					if (HttpDnsLog.isPrint()) {
						HttpDnsLog.i("request host " + host + " via local dns return " + result);
					}
					return result;
				}
			} catch (UnknownHostException e) {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.e("failed request host " + host + " via local dns", e);
				}
			}
		}

		return Constants.EMPTY;
	}

	private void syncResolveHostInner(final String host, final RequestIpType type,
									  Map<String, String> extras, final String cacheKey,
									  HTTPDNSResultWrapper result) {
		long start = System.currentTimeMillis();
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.i("sync start request for " + host + " " + type);
		}
		if (mLocker.beginResolve(host, type, cacheKey)) {
			final String region = mHttpDnsConfig.getRegion();
			// 没有正在进行的解析，发起新的解析
			mRequestHandler.requestResolveHost(host, type, extras, cacheKey,
				new RequestCallback<ResolveHostResponse>() {
					@Override
					public void onSuccess(final ResolveHostResponse resolveHostResponse) {
						if (HttpDnsLog.isPrint()) {
							HttpDnsLog.i("ip request for " + host + " " + type + " return "
								+ resolveHostResponse.toString());
						}
						mResultRepo.save(region, resolveHostResponse, cacheKey);

						if (type == RequestIpType.v4 || type == RequestIpType.both) {
							for (final ResolveHostResponse.HostItem item :
									resolveHostResponse.getItems()) {
								if (item.getIpType() == RequestIpType.v4) {
									mIpIPRankingService.probeIpv4(host, item.getIps(),
											new IPRankingCallback() {
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
							}

						}
						mLocker.endResolve(host, type, cacheKey);
					}

					@Override
					public void onFail(Throwable throwable) {
						HttpDnsLog.w("ip request for " + host + " fail", throwable);
						if (throwable instanceof Exception) {
							String query = "4";
							if (type == RequestIpType.v6) {
								query = "6";
							}else if (type == RequestIpType.both) {
								query = "4,6";
							}
							String errorMsg = (throwable instanceof HttpException) ? throwable.getMessage() : throwable.toString();
							HttpDnsLog.w("RESOLVE FAIL, HOST:" + host + ", QUERY:" + query
									+ ", Msg:" + errorMsg);
						}
						if (throwable instanceof HttpException
							&& ((HttpException)throwable).shouldCreateEmptyCache()) {
							ArrayList<String> targetHost = new ArrayList<>();
							targetHost.add(host);
							ResolveHostResponse emptyResponse = ResolveHostResponse.createEmpty(
									targetHost, type, 60 * 60);
							mResultRepo.save(region, emptyResponse, cacheKey);
						}

						mLocker.endResolve(host, type, cacheKey);
					}
				});
		}

		if (result == null || !mEnableExpiredIp) {
			// 有结果，但是过期了，不允许返回过期结果，等请求结束
			/* 同步锁超时时间不能大于5s，不论请求是否已结束，保证同步锁都会在至多5s内结束 */
			int timeout = mHttpDnsConfig.getTimeout();
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("the httpDnsConfig timeout is: " + timeout);
			}
			//锁的超时上限不能大于5s
			timeout = Math.min(timeout, Constants.SYNC_TIMEOUT_MAX_LIMIT);
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("final timeout is: " + timeout);
			}

			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("wait for request finish");
			}
			try {
				boolean waitResult = mLocker.await(host, type, cacheKey, timeout, TimeUnit.MILLISECONDS);
				if (!waitResult) {
					if (HttpDnsLog.isPrint()) {
						HttpDnsLog.d("lock await timeout finished");
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// 不管什么情况释放锁
			mLocker.endResolve(host, type, cacheKey);
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("sync resolve time is: " + (System.currentTimeMillis() - start));
			}
		}
	}

	public void resolveHostAsync(final String host, final RequestIpType type, final Map<String, String> extras,
								 final String cacheKey, HttpDnsCallback callback) {
		if (mFilter.isFiltered(host)) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("request host " + host + ", which is filtered");
			}
			if (callback != null) {
				if (mHttpDnsConfig.isEnableDegradationLocalDns()) {
					mHttpDnsConfig.getResolveWorker().execute(new Runnable() {
						@Override
						public void run() {
							callback.onHttpDnsCompleted(degradationLocalDns(host));
						}
					});
				} else {
					callback.onHttpDnsCompleted(Constants.EMPTY);
				}
			}
			return;
		}

		long start = System.currentTimeMillis();

		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d(
					"async request host " + host + " with type " + type + " extras : " + CommonUtil.toString(
							extras) + " cacheKey " + cacheKey);
		}
		HTTPDNSResultWrapper result = mResultRepo.getIps(host, type, cacheKey);
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("host " + host + " result in cache is " + CommonUtil.toString(result));
		}

		if (result != null && (!result.isExpired() || mEnableExpiredIp)) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.i(
						"request host " + host + " for " + type + " and return " + result.toString()
								+ " immediately");
			}

			if (result.isExpired()) {
				//如果缓存已经过期了，发起异步解析更新缓存
				asyncResolveHostInner(host, type, extras, cacheKey);
			}

			//可能是空结果，如果开启降级local dns，走local dns解析
			if ((result.getIps() == null || result.getIps().length == 0)
					&& (result.getIpv6s() == null || result.getIpv6s().length == 0)) {
				if (mHttpDnsConfig.isEnableDegradationLocalDns()) {
					mHttpDnsConfig.getResolveWorker().execute(new Runnable() {
						@Override
						public void run() {
							callback.onHttpDnsCompleted(degradationLocalDns(host));
						}
					});
					return;
				}
			}

			addCallSdkApiEvent(getCallSdkApiEvent(ObservableConstants.RESOLVE_API_ASYNC, host, type, true, result, start), result);

			if (callback != null) {
				callback.onHttpDnsCompleted(result.getHTTPDNSResult());
			}
			return;
		}

		CallSdkApiEvent callSdkApiEvent = getCallSdkApiEvent(ObservableConstants.RESOLVE_API_ASYNC, host, type, false, result, start);
		//没有可用的解析结果，需要发起请求
		if (type == RequestIpType.both) {
			// 过滤掉 未过期的请求
			HTTPDNSResultWrapper resultV4 = mResultRepo.getIps(host, RequestIpType.v4, cacheKey);
			HTTPDNSResultWrapper resultV6 = mResultRepo.getIps(host, RequestIpType.v6, cacheKey);
			boolean v4Invalid =
					resultV4 == null || resultV4.isExpired();
			boolean v6Invalid =
					resultV6 == null || resultV6.isExpired();
			if (v4Invalid && v6Invalid) {
				// 都过期，不过滤
				asyncResolveHost(host, type, extras, cacheKey, callback, callSdkApiEvent);
			} else if (v4Invalid) {
				// 仅v4过期
				asyncResolveHost(host, RequestIpType.v4, extras, cacheKey, callback, callSdkApiEvent);
			} else if (v6Invalid) {
				// 仅v6过期
				asyncResolveHost(host, RequestIpType.v6, extras, cacheKey, callback, callSdkApiEvent);
			}
		} else {
			asyncResolveHost(host, type, extras, cacheKey, callback, callSdkApiEvent);
		}
	}

	private void asyncResolveHost(String host, RequestIpType type, Map<String, String> extras,
								  String cacheKey, HttpDnsCallback callback, CallSdkApiEvent event) {
		mHttpDnsConfig.getResolveWorker().execute(new Runnable() {
			@Override
			public void run() {
				asyncResolveHostInner(host, type, extras, cacheKey);

				/* 同步锁超时时间不能大于5s，不论请求是否已结束，保证同步锁都会在至多5s内结束 */
				int timeout = mHttpDnsConfig.getTimeout();
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.d("the httpDnsConfig timeout is: " + timeout);
				}
				//锁的超时上限不能大于5s
				timeout = Math.min(timeout, Constants.SYNC_TIMEOUT_MAX_LIMIT);
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.d("final timeout is: " + timeout);
				}
				//发起异步请求，有无并发，统一在这里通过锁同步机制，从缓存中处理解析结果
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.d("wait for request finish");
				}
				try {
					boolean waitResult = mAsyncLocker.await(host, type, cacheKey, timeout, TimeUnit.MILLISECONDS);
					if (!waitResult) {
						if (HttpDnsLog.isPrint()) {
							HttpDnsLog.d("lock await timeout finished");
						}
					}
				} catch (InterruptedException e) {
					HttpDnsLog.e(e.getMessage(), e);
				}

				HTTPDNSResultWrapper result = mResultRepo.getIps(host, type, cacheKey);

				addCallSdkApiEvent(event, result);

				if (result != null && !result.isExpired()) {
					if (HttpDnsLog.isPrint()) {
						HttpDnsLog.i(
								"request host " + host + " for " + type + " and return " + result.toString()
										+ " after request");
					}

					if (callback != null) {
						//可能是空结果，如果开启降级local dns，走local dns解析
						if ((result.getIps() == null || result.getIps().length == 0)
								&& (result.getIpv6s() == null || result.getIpv6s().length == 0)) {
							if (mHttpDnsConfig.isEnableDegradationLocalDns()) {
								callback.onHttpDnsCompleted(degradationLocalDns(host));
								return;
							}
						}

						callback.onHttpDnsCompleted(result.getHTTPDNSResult());
					}
				} else {
					if (HttpDnsLog.isPrint()) {
						HttpDnsLog.i("request host " + host + " and return empty after request");
					}

					if (callback != null) {
						callback.onHttpDnsCompleted(degradationLocalDns(host));
					}
				}
			}
		});
	}

	public void setEnableExpiredIp(boolean enableExpiredIp) {
		this.mEnableExpiredIp = enableExpiredIp;
	}

	private void addCallSdkApiEvent(CallSdkApiEvent event, HTTPDNSResultWrapper result) {
		event.setCostTime((int) (System.currentTimeMillis() - event.getTimestamp()));

		if (result != null) {
			event.setServerIp(result.getServerIp());
			event.setHttpDnsIps(result.getIps(), result.getIpv6s());
			if (TextUtils.isEmpty(event.getHttpDnsIps())) {
				event.setStatusCode(204);
			} else {
				event.setResultStatus(ObservableConstants.NOT_EMPTY_RESULT);
				event.setStatusCode(200);
			}
		} else {
			event.setHttpDnsIps(null, null);
			event.setStatusCode(204);
		}

		mHttpDnsConfig.getObservableManager().addObservableEvent(event);
	}

	private CallSdkApiEvent getCallSdkApiEvent(int api, String host, RequestIpType type, boolean hitCache, HTTPDNSResultWrapper cacheResult, long start) {
		CallSdkApiEvent callSdkApiEvent = new CallSdkApiEvent(start);
		callSdkApiEvent.setRequestType(type);
		callSdkApiEvent.setHostName(host);
		callSdkApiEvent.setInvokeApi(api);
		int cacheScene = cacheResult == null ? ObservableConstants.CACHE_NONE : (hitCache ? (cacheResult.isExpired() ? ObservableConstants.CACHE_EXPIRED_USE : ObservableConstants.CACHE_NOT_EXPIRED) : ObservableConstants.CACHE_EXPIRED_NOT_USE);
		callSdkApiEvent.setCacheScene(cacheScene);

		return callSdkApiEvent;
	}
}
