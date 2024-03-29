package com.alibaba.sdk.android.httpdns.ranking;

/**
 * IP优选配置项
 */
public class IPRankingBean {
	/**
	 * 进行ip优选的域名
	 */
	String hostName;
	/**
	 * 用于测试速度的端口
	 */
	int port;

	public IPRankingBean(String hostName, int port) {
		this.hostName = hostName;
		this.port = port;
	}

	public String getHostName() {
		return hostName;
	}

	public int getPort() {
		return port;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		IPRankingBean that = (IPRankingBean)o;
		return port == that.port && hostName != null && hostName.equals(that.hostName);
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + (hostName == null ? 0 : hostName.hashCode());
		hash = 31 * hash + port;
		return hash;
	}
}
