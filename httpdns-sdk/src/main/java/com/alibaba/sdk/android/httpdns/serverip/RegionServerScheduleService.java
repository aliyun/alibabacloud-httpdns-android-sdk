package com.alibaba.sdk.android.httpdns.serverip;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

/**
 * 服务IP的调度服务
 */
public class RegionServerScheduleService {

	private final HttpDnsConfig mHttpDnsConfig;
	private final OnRegionServerIpUpdate mOnServerIpUpdate;
	private final RegionServerIpRepo mServerIpRepo;
	private final UpdateRegionServerLocker mLocker;

	public RegionServerScheduleService(HttpDnsConfig config, OnRegionServerIpUpdate onServerIpUpdate) {
		this.mHttpDnsConfig = config;
		this.mOnServerIpUpdate = onServerIpUpdate;
		this.mServerIpRepo = new RegionServerIpRepo();
		this.mLocker = new UpdateRegionServerLocker();
	}

	/**
	 * 修改region
	 *
	 * @param newRegion
	 */
	public void updateRegionServerIps(final String newRegion, final int scenes) {
		String[] serverIps = mServerIpRepo.getServerIps(newRegion);
		int[] ports = mServerIpRepo.getPorts(newRegion);
		String[] serverV6Ips = mServerIpRepo.getServerV6Ips(newRegion);
		int[] v6Ports = mServerIpRepo.getV6Ports(newRegion);
		if (serverIps != null || serverV6Ips != null) {
			updateServerConfig(newRegion, serverIps, ports, serverV6Ips, v6Ports);
			return;
		}

		if (mLocker.begin(newRegion)) {
			UpdateRegionServerTask.updateRegionServer(mHttpDnsConfig, newRegion, scenes,
				new RequestCallback<UpdateRegionServerResponse>() {
					@Override
					public void onSuccess(UpdateRegionServerResponse updateServerResponse) {
						if (!updateServerResponse.isEnable()) {
							HttpDnsLog.i("disable service by server response "
								+ updateServerResponse.toString());
							mHttpDnsConfig.setEnabled(false);
							return;
						} else {
							if (!mHttpDnsConfig.isEnabled()) {
								mHttpDnsConfig.setEnabled(true);
							}
						}
						if (updateServerResponse.getServerIps() != null) {
							updateServerConfig(newRegion, updateServerResponse.getServerIps(),
								updateServerResponse.getServerPorts(),
								updateServerResponse.getServerIpv6s(),
								updateServerResponse.getServerIpv6Ports());
							mServerIpRepo.save(newRegion, updateServerResponse.getServerIps(),
								updateServerResponse.getServerPorts(),
								updateServerResponse.getServerIpv6s(),
								updateServerResponse.getServerIpv6Ports());
						}

						boolean updated = mHttpDnsConfig.getObservableConfig().updateConfig(updateServerResponse.getObservableConfig());
						if (updated) {
							mHttpDnsConfig.saveToCache();
						}

						mLocker.end(newRegion);
					}

					@Override
					public void onFail(Throwable throwable) {
						HttpDnsLog.w("update server ips fail", throwable);
						mLocker.end(newRegion);
					}
				});
		}
	}

	private void updateServerConfig(String newRegion, String[] serverIps, int[] serverPorts,
									String[] serverV6Ips, int[] serverV6Ports) {
		boolean regionUpdated =
            !CommonUtil.regionEquals(this.mHttpDnsConfig.getCurrentServer().getRegion(),
			newRegion);
		boolean updated = mHttpDnsConfig.getCurrentServer().setServerIps(newRegion, serverIps, serverPorts,
			serverV6Ips, serverV6Ports);
		if (updated && mOnServerIpUpdate != null) {
			mOnServerIpUpdate.serverIpUpdated(regionUpdated);
		}
	}

	/**
	 * 更新服务ip
	 */
	public void updateRegionServerIps(int scenes) {
		updateRegionServerIps(this.mHttpDnsConfig.getRegion(), scenes);
	}

	/**
	 * 设置服务IP的更新间隔
	 */
	public void setTimeInterval(int timeInterval) {
		this.mServerIpRepo.setTimeInterval(timeInterval);
	}

	/**
	 * 服务ip更新时的回调接口
	 */
	public interface OnRegionServerIpUpdate {
		/**
		 * 服务ip更新了
		 */
		void serverIpUpdated(boolean regionUpdated);
	}
}
