package com.alibaba.sdk.android.httpdns.serverip;

import java.util.HashMap;

import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

/**
 * 服务ip数据仓库
 * 实现有效期逻辑，有效期内返回值，有效期外返回空
 */
public class ServerIpRepo {

	private int interval = 5 * 60 * 1000;
	private final HashMap<String, ServerIpData> mCache = new HashMap<>();

	public String[] getServerIps(String region) {
		region = CommonUtil.fixRegion(region);
		ServerIpData data = mCache.get(region);
		if (data == null || data.getRequestTime() + interval < System.currentTimeMillis()) {
			return null;
		}
		return data.getServerIps();
	}

	public int[] getPorts(String region) {
		region = CommonUtil.fixRegion(region);
		ServerIpData data = mCache.get(region);
		if (data == null || data.getRequestTime() + interval < System.currentTimeMillis()) {
			return null;
		}
		return data.getServerPorts();
	}

	public String[] getServerV6Ips(String region) {
		region = CommonUtil.fixRegion(region);
		ServerIpData data = mCache.get(region);
		if (data == null || data.getRequestTime() + interval < System.currentTimeMillis()) {
			return null;
		}
		return data.getServerV6Ips();
	}

	public int[] getV6Ports(String region) {
		region = CommonUtil.fixRegion(region);
		ServerIpData data = mCache.get(region);
		if (data == null || data.getRequestTime() + interval < System.currentTimeMillis()) {
			return null;
		}
		return data.getServerV6Ports();
	}

	public void save(String region, String[] serverIps, int[] ports, String[] serverV6Ips,
					 int[] v6Ports) {
		region = CommonUtil.fixRegion(region);
		ServerIpData data = new ServerIpData(region, serverIps, ports, serverV6Ips, v6Ports);
		mCache.put(region, data);
	}

	public void setTimeInterval(int timeInterval) {
		this.interval = timeInterval;
	}

}
