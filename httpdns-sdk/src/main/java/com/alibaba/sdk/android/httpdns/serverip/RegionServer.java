package com.alibaba.sdk.android.httpdns.serverip;

import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

public class RegionServer {

	private final String mServerIp;
	private final int mPort;

	public RegionServer(String serverIp, int port) {
		this.mServerIp = serverIp;
		this.mPort = port;
	}

	public String getServerIp() {
		return mServerIp;
	}

	public int getPort(String scheme) {
		return CommonUtil.getPort(mPort, scheme);
	}
}
