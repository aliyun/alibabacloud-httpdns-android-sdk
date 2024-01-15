package com.alibaba.sdk.android.httpdns.ranking;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;

/**
 * IP优选服务
 */
public class IPRankingService {

	private final HttpDnsConfig mHttpDnsConfig;
	private List<IPRankingBean> mIPRankingBeanList;
	private IPRankingTask.SpeedTestSocketFactory mSocketFactory
		= new IPRankingTask.SpeedTestSocketFactory() {
		@Override
		public Socket create() {
			return new Socket();
		}
	};
	private final ConcurrentSkipListSet<String> mProbingHosts = new ConcurrentSkipListSet<>();

	public IPRankingService(HttpDnsConfig config) {
		this.mHttpDnsConfig = config;
	}

	/**
	 * 进行ipv4优选
	 */
	public void probeIpv4(String host, String[] ips, final IPRankingCallback IPRankingCallback) {
		if (mHttpDnsConfig.isIPRankingDisabled()) {
			return;
		}
		IPRankingBean ipRankingBean = getIPRankingBean(host);
		if (ipRankingBean != null && ips != null && ips.length > 1) {
			if (mProbingHosts.contains(host)) {
				return;
			}
			mProbingHosts.add(host);
			try {
				mHttpDnsConfig.getWorker().execute(
					new IPRankingTask(mSocketFactory, host, ips, ipRankingBean, new IPRankingCallback() {
						@Override
						public void onResult(String host, String[] sortedIps) {
							mProbingHosts.remove(host);
							if (IPRankingCallback != null) {
								IPRankingCallback.onResult(host, sortedIps);
							}
						}
					}));
			} catch (Exception e) {
				mProbingHosts.remove(host);
			}
		}
	}

	public void setIPRankingList(List<IPRankingBean> ipRankingBeanList) {
		this.mIPRankingBeanList = ipRankingBeanList;
	}

	public void setSocketFactory(IPRankingTask.SpeedTestSocketFactory socketFactory) {
		this.mSocketFactory = socketFactory;
	}

	private IPRankingBean getIPRankingBean(String host) {
		if (mIPRankingBeanList != null && mIPRankingBeanList.size() > 0) {
			ArrayList<IPRankingBean> list = new ArrayList<>(mIPRankingBeanList);
			for (IPRankingBean item : list) {
				if (host.equals(item.getHostName())) {
					return item;
				}
			}
		}
		return null;
	}
}
