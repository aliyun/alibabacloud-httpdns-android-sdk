package com.alibaba.sdk.android.httpdns.resolve;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.impl.HostResolveRecorder;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.ranking.IPRankingCallback;
import com.alibaba.sdk.android.httpdns.ranking.IPRankingService;
import com.alibaba.sdk.android.httpdns.request.HttpException;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.util.ArrayList;

/**
 * 批量解析域名
 *
 * @author zonglin.nzl
 * @date 2020/12/11
 */
public class BatchResolveHostService {
	private final HttpDnsConfig mHttpDnsConfig;
	private final ResolveHostResultRepo mResultRepo;
	private final ResolveHostRequestHandler mRequestHandler;
	private final IPRankingService mIpIPRankingService;
	private final HostFilter mHostFilter;
	private final HostResolveRecorder mRecorder;

	public BatchResolveHostService(HttpDnsConfig config, ResolveHostResultRepo repo,
								   ResolveHostRequestHandler requestHandler,
								   IPRankingService ipIPRankingService, HostFilter filter,
								   HostResolveRecorder recorder) {
		this.mHttpDnsConfig = config;
		this.mResultRepo = repo;
		this.mRequestHandler = requestHandler;
		this.mIpIPRankingService = ipIPRankingService;
		this.mHostFilter = filter;
		this.mRecorder = recorder;
	}

	/**
	 * 批量解析域名
	 *
	 * @param hostList
	 * @param type
	 */
	public void batchResolveHostAsync(final ArrayList<String> hostList, final RequestIpType type) {
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("resolve host " + hostList.toString() + " " + type);
		}

		ArrayList<String> hostsRequestV4 = new ArrayList<>();
		ArrayList<String> hostsRequestV6 = new ArrayList<>();
		ArrayList<String> hostsRequestBoth = new ArrayList<>();

		for (String host : hostList) {
			if (!CommonUtil.isAHost(host)
				|| CommonUtil.isAnIP(host)
				|| this.mHostFilter.isFiltered(host)) {
				// 过滤掉不需要的域名
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.d("resolve ignore host as not invalid " + host);
				}
				continue;
			}

			// 过滤掉有缓存的域名

			if (type == RequestIpType.v4) {
				HTTPDNSResult result = mResultRepo.getIps(host, type, null);
				if (result == null || result.isExpired() || result.isFromDB()) {
					// 需要解析
					hostsRequestV4.add(host);
				}
			} else if (type == RequestIpType.v6) {
				HTTPDNSResult result = mResultRepo.getIps(host, type, null);
				if (result == null || result.isExpired() || result.isFromDB()) {
					// 需要解析
					hostsRequestV6.add(host);
				}
			} else {
				HTTPDNSResult resultV4 = mResultRepo.getIps(host, RequestIpType.v4, null);
				HTTPDNSResult resultV6 = mResultRepo.getIps(host, RequestIpType.v6, null);
				if ((resultV4 == null || resultV4.isExpired() || resultV4.isFromDB()) && (
					resultV6 == null || resultV6.isExpired() || resultV6.isFromDB())) {
					// 都需要解析
					hostsRequestBoth.add(host);
				} else if (resultV4 == null || resultV4.isExpired() || resultV4.isFromDB()) {
					hostsRequestV4.add(host);
				} else if (resultV6 == null || resultV6.isExpired() || resultV6.isFromDB()) {
					hostsRequestV6.add(host);
				}
			}
		}
		batchResolveHost(hostsRequestV4, RequestIpType.v4);
		batchResolveHost(hostsRequestV6, RequestIpType.v6);
		batchResolveHost(hostsRequestBoth, RequestIpType.both);
	}

	private void batchResolveHost(ArrayList<String> hostList, final RequestIpType type) {
		if (hostList == null || hostList.size() == 0) {
			return;
		}
		ArrayList<String> allHosts = new ArrayList<>(hostList);
		// 预解析每次最多5个域名
		final int maxCountPerRequest = 5;
		int requestCount = (hostList.size() + maxCountPerRequest - 1) / maxCountPerRequest;
		for (int i = 0; i < requestCount; i++) {
			final ArrayList<String> targetHost = new ArrayList<>();
			while (targetHost.size() < maxCountPerRequest && allHosts.size() > 0) {
				String host = allHosts.remove(0);
				if (mRecorder.beginResolve(host, type)) {
					targetHost.add(host);
				} else {
					if (HttpDnsLog.isPrint()) {
						HttpDnsLog.d("resolve ignore host as already interpret " + host);
					}
				}
			}
			if (targetHost.size() <= 0) {
				continue;
			}
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.i("resolve host " + targetHost.toString() + " " + type);
			}
			final String region = mHttpDnsConfig.getRegion();
			mRequestHandler.requestResolveHost(targetHost, type,
				new RequestCallback<BatchResolveHostResponse>() {
					@Override
					public void onSuccess(final BatchResolveHostResponse resolveHostResponse) {
						if (HttpDnsLog.isPrint()) {
							HttpDnsLog.d("resolve hosts for " + targetHost.toString() + " " + type
								+ " return " + resolveHostResponse.toString());
						}
						mResultRepo.save(region, type, resolveHostResponse);
						if (type == RequestIpType.v4 || type == RequestIpType.both) {
							for (final BatchResolveHostResponse.HostItem item :
								resolveHostResponse.getItems()) {
								if (item.getIpType() == RequestIpType.v4) {
									mIpIPRankingService.probeIpv4(item.getHost(), item.getIps(),
										new IPRankingCallback() {
											@Override
											public void onResult(String host, String[] sortedIps) {
												mResultRepo.update(item.getHost(),
													item.getIpType(),
													null, sortedIps);
											}
										});
								}
							}
						}
						for (String host : targetHost) {
							mRecorder.endResolve(host, type);
						}
					}

					@Override
					public void onFail(Throwable throwable) {
						HttpDnsLog.w("resolve hosts for " + targetHost.toString() + " fail",
							throwable);
						if (throwable instanceof HttpException
							&& ((HttpException)throwable).shouldCreateEmptyCache()) {
							BatchResolveHostResponse emptyResponse = BatchResolveHostResponse.createEmpty(
								targetHost, type, 60 * 60);
							mResultRepo.save(region, type, emptyResponse);
						}
						for (String host : targetHost) {
							mRecorder.endResolve(host, type);
						}
					}
				});
		}
	}
}
