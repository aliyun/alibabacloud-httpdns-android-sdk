package com.alibaba.sdk.android.httpdns.serverip;

/**
 * 服务IP存储的数据
 */
public class ServerIpData {
	private final String mRegion;
	private final long mRequestTime;
	private final String[] mServerIps;
	private final int[] mServerPorts;
	private final String[] mServerV6Ips;
	private final int[] mServerV6Ports;

	public ServerIpData(String region, long requestTime, String[] serverIps, int[] ports,
						String[] serverV6Ips, int[] v6Ports) {
		this.mRegion = region;
		this.mRequestTime = requestTime;
		this.mServerIps = serverIps;
		this.mServerPorts = ports;
		this.mServerV6Ips = serverV6Ips;
		this.mServerV6Ports = v6Ports;
	}

	public ServerIpData(String region, String[] serverIps, int[] ports, String[] serverV6Ips,
						int[] v6Ports) {
		this(region, System.currentTimeMillis(), serverIps, ports, serverV6Ips, v6Ports);
	}

	public String getRegion() {
		return mRegion;
	}

	public long getRequestTime() {
		return mRequestTime;
	}

	public String[] getServerIps() {
		return mServerIps;
	}

	public int[] getServerPorts() {
		return mServerPorts;
	}

	public String[] getServerV6Ips() {
		return mServerV6Ips;
	}

	public int[] getServerV6Ports() {
		return mServerV6Ports;
	}
}
