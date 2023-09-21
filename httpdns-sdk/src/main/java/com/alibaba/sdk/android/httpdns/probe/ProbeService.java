package com.alibaba.sdk.android.httpdns.probe;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;

/**
 * IP优选服务
 */
public class ProbeService {

	private final HttpDnsConfig mHttpDnsConfig;
	private List<IPProbeItem> mIPProbeItems;
	private ProbeTask.SpeedTestSocketFactory mSocketFactory
		= new ProbeTask.SpeedTestSocketFactory() {
		@Override
		public Socket create() {
			return new Socket();
		}
	};
	private final ConcurrentSkipListSet<String> mProbingHosts = new ConcurrentSkipListSet<>();

	public ProbeService(HttpDnsConfig config) {
		this.mHttpDnsConfig = config;
	}

	/**
	 * 进行ipv4优选
	 */
	public void probeIpv4(String host, String[] ips, final ProbeCallback probeCallback) {
		if (mHttpDnsConfig.isProbeDisabled()) {
			return;
		}
		IPProbeItem ipProbeItem = getIpProbeItem(host);
		if (ipProbeItem != null && ips != null && ips.length > 1) {
			if (mProbingHosts.contains(host)) {
				return;
			}
			mProbingHosts.add(host);
			try {
				mHttpDnsConfig.getWorker().execute(
					new ProbeTask(mSocketFactory, host, ips, ipProbeItem, new ProbeCallback() {
						@Override
						public void onResult(String host, String[] sortedIps) {
							mProbingHosts.remove(host);
							if (probeCallback != null) {
								probeCallback.onResult(host, sortedIps);
							}
						}
					}));
			} catch (Exception e) {
				mProbingHosts.remove(host);
			}
		}
	}

	public void setIPProbeItems(List<IPProbeItem> IPProbeItems) {
		this.mIPProbeItems = IPProbeItems;
	}

	public void setSocketFactory(ProbeTask.SpeedTestSocketFactory socketFactory) {
		this.mSocketFactory = socketFactory;
	}

	private IPProbeItem getIpProbeItem(String host) {
		if (mIPProbeItems != null && mIPProbeItems.size() > 0) {
			ArrayList<IPProbeItem> list = new ArrayList<>(mIPProbeItems);
			for (IPProbeItem item : list) {
				if (host.equals(item.getHostName())) {
					return item;
				}
			}
		}
		return null;
	}
}
