package com.alibaba.sdk.android.httpdns.resolve;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.HttpDnsCallback;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.impl.HostResolveLocker;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
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
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d(
				"sync non blocking request host " + host + " with type " + type + " extras : " + CommonUtil.toString(
					extras) + " cacheKey " + cacheKey);
		}
		HTTPDNSResult result = mResultRepo.getIps(host, type, cacheKey);
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("host " + host + " result is " + CommonUtil.toString(result));
		}
		if (result == null || result.isExpired()) {
			if (type == RequestIpType.both) {
				// 过滤掉 未过期的请求
				HTTPDNSResult resultV4 = mResultRepo.getIps(host, RequestIpType.v4, cacheKey);
				HTTPDNSResult resultV6 = mResultRepo.getIps(host, RequestIpType.v6, cacheKey);
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
			return result;
		} else {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.i("request host " + host + " and return empty immediately");
			}
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
					public void onSuccess(final ResolveHostResponse interpretHostResponse) {
						if (HttpDnsLog.isPrint()) {
							HttpDnsLog.i("ip request for " + host + " " + type + " return "
								+ interpretHostResponse.toString());
						}
						mResultRepo.save(region, host, type, interpretHostResponse.getExtras(),
							cacheKey,
							interpretHostResponse);
						if (type == RequestIpType.v4 || type == RequestIpType.both) {
							mIpIPRankingService.probeIpv4(host, interpretHostResponse.getIps(),
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
						mAsyncLocker.endResolve(host, type, cacheKey);
					}

					@Override
					public void onFail(Throwable throwable) {
						HttpDnsLog.w("ip request for " + host + " fail", throwable);
						if (throwable instanceof HttpException
							&& ((HttpException)throwable).shouldCreateEmptyCache()) {
							ResolveHostResponse emptyResponse =
								ResolveHostResponse.createEmpty(
									host, 60 * 60);
							mResultRepo.save(region, host, type, emptyResponse.getExtras(),
								cacheKey,
								emptyResponse);
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
			HttpDnsLog.d("request host " + host + ", which is filtered");
			return Constants.EMPTY;
		}
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("sync request host " + host + " with type " + type + " extras : "
				+ CommonUtil.toString(extras) + " cacheKey " + cacheKey);
		}
		HTTPDNSResult result = mResultRepo.getIps(host, type, cacheKey);
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

			return result;
		}

		//如果没有缓存，或者有过期缓存但是不允许使用过期缓存
		if (result == null || result.isExpired()) {
			// 没有缓存，或者缓存过期，或者是从数据库读取的 需要解析
			if (type == RequestIpType.both) {
				// 过滤掉 未过期的请求
				HTTPDNSResult resultV4 = mResultRepo.getIps(host, RequestIpType.v4, cacheKey);
				HTTPDNSResult resultV6 = mResultRepo.getIps(host, RequestIpType.v6, cacheKey);
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

		result = mResultRepo.getIps(host, type, cacheKey);
		if (result != null && (!result.isExpired() || mEnableExpiredIp)) {
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

	private void syncResolveHostInner(final String host, final RequestIpType type,
									  Map<String, String> extras, final String cacheKey,
									  HTTPDNSResult result) {
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
					public void onSuccess(final ResolveHostResponse interpretHostResponse) {
						if (HttpDnsLog.isPrint()) {
							HttpDnsLog.i("ip request for " + host + " " + type + " return "
								+ interpretHostResponse.toString());
						}
						mResultRepo.save(region, host, type, interpretHostResponse.getExtras(),
							cacheKey,
							interpretHostResponse);
						if (type == RequestIpType.v4 || type == RequestIpType.both) {
							mIpIPRankingService.probeIpv4(host, interpretHostResponse.getIps(),
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
						mLocker.endResolve(host, type, cacheKey);
					}

					@Override
					public void onFail(Throwable throwable) {
						HttpDnsLog.w("ip request for " + host + " fail", throwable);
						if (throwable instanceof HttpException
							&& ((HttpException)throwable).shouldCreateEmptyCache()) {
							ResolveHostResponse emptyResponse =
								ResolveHostResponse.createEmpty(
									host, 60 * 60);
							mResultRepo.save(region, host, type, emptyResponse.getExtras(),
								cacheKey,
								emptyResponse);
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
				callback.onHttpDnsCompleted(Constants.EMPTY);
			}
			return;
		}
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d(
					"async request host " + host + " with type " + type + " extras : " + CommonUtil.toString(
							extras) + " cacheKey " + cacheKey);
		}
		HTTPDNSResult result = mResultRepo.getIps(host, type, cacheKey);
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

			if (callback != null) {
				callback.onHttpDnsCompleted(result);
			}
			return;
		}

		//没有可用的解析结果，需要发起请求
		if (type == RequestIpType.both) {
			// 过滤掉 未过期的请求
			HTTPDNSResult resultV4 = mResultRepo.getIps(host, RequestIpType.v4, cacheKey);
			HTTPDNSResult resultV6 = mResultRepo.getIps(host, RequestIpType.v6, cacheKey);
			boolean v4Invalid =
					resultV4 == null || resultV4.isExpired();
			boolean v6Invalid =
					resultV6 == null || resultV6.isExpired();
			if (v4Invalid && v6Invalid) {
				// 都过期，不过滤
				asyncResolveHost(host, type, extras, cacheKey, callback);
			} else if (v4Invalid) {
				// 仅v4过期
				asyncResolveHost(host, RequestIpType.v4, extras, cacheKey, callback);
			} else if (v6Invalid) {
				// 仅v6过期
				asyncResolveHost(host, RequestIpType.v6, extras, cacheKey, callback);
			}
		} else {
			asyncResolveHost(host, type, extras, cacheKey, callback);
		}
	}

	private void asyncResolveHost(String host, RequestIpType type, Map<String, String> extras,
									   String cacheKey, HttpDnsCallback callback) {
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

				HTTPDNSResult result = mResultRepo.getIps(host, type, cacheKey);
				if (result != null && !result.isExpired()) {
					if (HttpDnsLog.isPrint()) {
						HttpDnsLog.i(
								"request host " + host + " for " + type + " and return " + result.toString()
										+ " after request");
					}

					if (callback != null) {
						callback.onHttpDnsCompleted(result);
					}
				} else {
					if (HttpDnsLog.isPrint()) {
						HttpDnsLog.i("request host " + host + " and return empty after request");
					}

					if (callback != null) {
						callback.onHttpDnsCompleted(Constants.EMPTY);
					}
				}
			}
		});
	}

	public void setEnableExpiredIp(boolean enableExpiredIp) {
		this.mEnableExpiredIp = enableExpiredIp;
	}

}
