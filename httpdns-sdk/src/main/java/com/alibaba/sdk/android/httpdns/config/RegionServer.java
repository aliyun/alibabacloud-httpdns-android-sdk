package com.alibaba.sdk.android.httpdns.config;

import java.util.Arrays;

import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

public class RegionServer {
	/**
	 * HttpDns的服务IP
	 */
	private String[] mServerIps;
	/**
	 * HttpDns的服务端口，线上都是默认端口 80 或者 443
	 * 此处是为了测试场景指定端口
	 * 下标和{@link #mServerIps} 对应
	 * 如果为null 表示没有指定端口
	 */
	private int[] mPorts;
	private String mRegion;

	private String[] mIpv6ServerIps;
	private int[] mIpv6Ports;

	public RegionServer(String[] serverIps, int[] ports, String[] ipv6ServerIps, int[] ipv6Ports,
						String region) {
		this.mServerIps = serverIps == null ? new String[0] : serverIps;
		this.mPorts = ports;
		this.mRegion = region;
		this.mIpv6ServerIps = ipv6ServerIps == null ? new String[0] : ipv6ServerIps;
		this.mIpv6Ports = ipv6Ports;
	}

	public String[] getServerIps() {
		return mServerIps;
	}

	public int[] getPorts() {
		return mPorts;
	}

	public String getRegion() {
		return mRegion;
	}

	public String[] getIpv6ServerIps() {
		return mIpv6ServerIps;
	}

	public int[] getIpv6Ports() {
		return mIpv6Ports;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {return true;}
		if (o == null || getClass() != o.getClass()) {return false;}
		RegionServer that = (RegionServer)o;

		return Arrays.equals(mServerIps, that.mServerIps) &&
			Arrays.equals(mPorts, that.mPorts) &&
			Arrays.equals(mIpv6ServerIps, that.mIpv6ServerIps) &&
			Arrays.equals(mIpv6Ports, that.mIpv6Ports) &&
			CommonUtil.equals(mRegion, that.mRegion);
	}

	public boolean serverEquals(RegionServer that) {
		return Arrays.equals(mServerIps, that.mServerIps) &&
			Arrays.equals(mPorts, that.mPorts) &&
			Arrays.equals(mIpv6ServerIps, that.mIpv6ServerIps) &&
			Arrays.equals(mIpv6Ports, that.mIpv6Ports) &&
			CommonUtil.equals(mRegion, that.mRegion);
	}

	@Override
	public int hashCode() {
		int result = Arrays.hashCode(new Object[] {mRegion});
		result = 31 * result + Arrays.hashCode(mServerIps);
		result = 31 * result + Arrays.hashCode(mPorts);
		result = 31 * result + Arrays.hashCode(mIpv6ServerIps);
		result = 31 * result + Arrays.hashCode(mIpv6Ports);
		return result;
	}

	public boolean updateIpv6(String[] ips, int[] ports) {
		boolean same = CommonUtil.isSameServer(this.mIpv6ServerIps, this.mIpv6Ports, ips, ports);
		if (same) {
			return false;
		}
		this.mIpv6ServerIps = ips;
		this.mIpv6Ports = ports;
		return true;
	}

	public boolean updateRegionAndIpv4(String region, String[] ips, int[] ports) {
		boolean same = CommonUtil.isSameServer(this.mServerIps, this.mPorts, ips, ports);
		if (same && region.equals(this.mRegion)) {
			return false;
		}
		this.mRegion = region;
		this.mServerIps = ips;
		this.mPorts = ports;
		return true;
	}

	public boolean updateAll(String region, String[] ips, int[] ports, String[] ipv6s,
							 int[] v6ports) {
		boolean same = CommonUtil.isSameServer(this.mServerIps, this.mPorts, ips, ports);
		boolean v6same = CommonUtil.isSameServer(this.mIpv6ServerIps, this.mIpv6Ports, ipv6s,
			v6ports);
		if (same && v6same && region.equals(this.mRegion)) {
			return false;
		}
		this.mRegion = region;
		this.mServerIps = ips;
		this.mPorts = ports;
		this.mIpv6ServerIps = ipv6s;
		this.mIpv6Ports = v6ports;
		return true;
	}
}
