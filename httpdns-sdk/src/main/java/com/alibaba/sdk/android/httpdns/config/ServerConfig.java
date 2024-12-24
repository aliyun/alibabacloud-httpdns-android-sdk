package com.alibaba.sdk.android.httpdns.config;

import java.util.Arrays;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * 服务节点配置
 * 维护 服务节点的一些状态
 */
public class ServerConfig extends RegionServer implements SpCacheItem {

	private final HttpDnsConfig mHttpDnsConfig;
	private int mLastOkServerIndex = 0;
	private int mCurrentServerIndex = 0;
	private long mServerIpsLastUpdatedTime = 0;

	private int mLastOkServerIndexForV6 = 0;
	private int mCurrentServerIndexForV6 = 0;

	public ServerConfig(HttpDnsConfig config, String[] serverIps, int[] ports, String[] ipv6ServerIps, int[] ipv6Ports) {
		super(serverIps, ports, ipv6ServerIps, ipv6Ports, config.getRegion());
		mHttpDnsConfig = config;
	}

	/**
	 * 获取当前使用的服务IP
	 */
	public String getServerIp() {
		final String[] serverIps = getServerIps();
		if (serverIps == null || mCurrentServerIndex >= serverIps.length
			|| mCurrentServerIndex < 0) {
			return null;
		}
		return serverIps[mCurrentServerIndex];
	}

	/**
	 * 获取当前使用的服务IP ipv6
	 */
	public String getServerIpForV6() {
		final String[] serverIps = getIpv6ServerIps();
		if (serverIps == null || mCurrentServerIndexForV6 >= serverIps.length
			|| mCurrentServerIndexForV6 < 0) {
			return null;
		}
		return serverIps[mCurrentServerIndexForV6];
	}

	/**
	 * 获取当前使用的服务端口
	 */
	public int getPort() {
		final int[] ports = getPorts();
		if (ports == null || mCurrentServerIndex >= ports.length || mCurrentServerIndex < 0) {
			return CommonUtil.getPort(-1, mHttpDnsConfig.getSchema());
		}
		return CommonUtil.getPort(ports[mCurrentServerIndex], mHttpDnsConfig.getSchema());
	}

	/**
	 * 获取当前使用的服务端口
	 */
	public int getPortForV6() {
		final int[] ports = getIpv6Ports();
		if (ports == null || mCurrentServerIndexForV6 >= ports.length
			|| mCurrentServerIndexForV6 < 0) {
			return CommonUtil.getPort(-1, mHttpDnsConfig.getSchema());
		}
		return CommonUtil.getPort(ports[mCurrentServerIndexForV6], mHttpDnsConfig.getSchema());
	}

	/**
	 * 是否应该更新服务IP
	 */
	public boolean shouldUpdateServerIp() {
		return System.currentTimeMillis() - mServerIpsLastUpdatedTime >= 24 * 60 * 60 * 1000;
	}

	/**
	 * 设置服务IP
	 *
	 * @return false 表示 前后服务一直，没有更新
	 */
	public synchronized boolean setServerIps(String region, String[] serverIps, int[] ports,
								String[] serverV6Ips, int[] v6Ports) {
		region = CommonUtil.fixRegion(region);
		if (serverIps == null || serverIps.length == 0) {
			serverIps = mHttpDnsConfig.getInitServer().getServerIps();
			ports = mHttpDnsConfig.getInitServer().getPorts();
		}
		if (serverV6Ips == null || serverV6Ips.length == 0) {
			serverV6Ips = mHttpDnsConfig.getInitServer().getIpv6ServerIps();
			v6Ports = mHttpDnsConfig.getInitServer().getIpv6Ports();
		}
		boolean changed = updateRegionAndIpv4(region, serverIps, ports);
		boolean v6changed = updateIpv6(serverV6Ips, v6Ports);
		if (changed) {
			this.mLastOkServerIndex = 0;
			this.mCurrentServerIndex = 0;
		}
		if (v6changed) {
			this.mLastOkServerIndexForV6 = 0;
			this.mCurrentServerIndexForV6 = 0;
		}
		if (!CommonUtil.isSameServer(serverIps, ports,
			mHttpDnsConfig.getInitServer().getServerIps(),
			mHttpDnsConfig.getInitServer().getPorts())
			|| !CommonUtil.isSameServer(serverV6Ips, v6Ports,
			mHttpDnsConfig.getInitServer().getIpv6ServerIps(),
			mHttpDnsConfig.getInitServer().getIpv6Ports())) {
			// 非初始化IP，才认为是真正的更新了服务IP
			this.mServerIpsLastUpdatedTime = System.currentTimeMillis();
			// 非初始IP才有缓存的必要
			mHttpDnsConfig.saveToCache();
		}
		return changed || v6changed;
	}

	public synchronized void updateServerIpv4sRank(String[] sortedIps, int[] ports) {
		String[] serverIps = getServerIps();
		int[] serverPorts = getPorts();
		String region = getRegion();

		//对比和当前的region server是否是同一批，避免测速完已经被更新
		if (serverIps.length != sortedIps.length) {
			//ip数量不一致，数据已经被更新
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("abort rank server ip count changed, current ips: " + Arrays.toString(serverIps)
						+ ", sorted ips: " + Arrays.toString(sortedIps));
			}
			return;
		}

		boolean contain;
		//如果排序的ip都在当前Server ip列表中，认为是一批服务ip，ip和端口需要一起判断
		for (int i = 0; i != sortedIps.length; ++i) {
			contain = isContainServiceIp(serverIps, serverPorts, sortedIps[i], ports == null ? -1 : ports[i]);
			if (!contain) {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.d("abort rank server ip as changed, current ips: " + Arrays.toString(serverIps)
							+ ", ports: " + Arrays.toString(serverPorts)
							+ ", sorted ips: " + Arrays.toString(sortedIps)
							+ ", ports: " + Arrays.toString(ports)
					);
				}

				return;
			}
		}

		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("update ranked server ips: " + Arrays.toString(sortedIps)
					+ ", ports: " + Arrays.toString(ports));
		}
		//仅更新内存
		boolean changed = updateRegionAndIpv4(region, sortedIps, ports);
		if (changed) {
			this.mLastOkServerIndex = 0;
			this.mCurrentServerIndex = 0;
		}
	}

	public synchronized void updateServerIpv6sRank(String[] sortedIps, int[] ports) {
		//和当前ip进行对比，看看是不是已经被更新了，如果被更新了那此次排序结果不使用
		String[] serverIps = getIpv6ServerIps();
		int[] serverPorts = getIpv6Ports();

		//对比和当前的region server是否是同一批，避免测速完已经被更新
		if (serverIps.length != sortedIps.length) {
			//ip数量不一致，数据已经被更新
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("abort rank server ip count changed, current ipv6s: " + Arrays.toString(serverIps)
						+ ", sorted ipv6s: " + Arrays.toString(sortedIps));
			}
			return;
		}

		boolean contain;
		//如果排序的ip都在当前Server ip列表中，认为是一批服务ip
		for (int i = 0; i != sortedIps.length; ++i) {
			contain = isContainServiceIp(serverIps, serverPorts, sortedIps[i], ports == null ? -1 : ports[i]);
			if (!contain) {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.d("abort rank server ip as changed, current ipv6s: " + Arrays.toString(serverIps)
							+ ", ports: " + Arrays.toString(serverPorts)
							+ ", sorted ipv6s: " + Arrays.toString(sortedIps)
							+ ", ports: " + Arrays.toString(ports)
					);
				}

				return;
			}
		}

		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("update ranked server ipv6s: " + Arrays.toString(sortedIps)
					+ ", ports: " + Arrays.toString(ports));
		}

		//仅更新内存
		boolean v6changed = updateIpv6(sortedIps, ports);
		if (v6changed) {
			mLastOkServerIndexForV6 = 0;
			mCurrentServerIndexForV6 = 0;
		}
	}

	private boolean isContainServiceIp(String[] sourceIps, int[] sourcePorts, String targetIp, int targetPort) {
		if (sourceIps == null || sourceIps.length == 0) {
			return false;
		}

		for (int i = 0; i != sourceIps.length; ++i) {
			if (TextUtils.equals(sourceIps[i], targetIp)) {
				if (sourcePorts == null) {
					return targetPort <= 0;
				}

				if (i < sourcePorts.length) {
					if (sourcePorts[i] == targetPort) {
						return true;
					}
				} else {
					if (targetPort <= 0) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * 切换域名解析服务
	 *
	 * @param ip   请求失败的服务IP
	 * @param port 请求失败的服务端口
	 * @return 是否切换回了最开始的服务。当请求切换的ip和port不是当前ip和port时，说明这个切换请求是无效的，不切换，返回false 认为没有切换回最开始的ip
	 */
	public boolean shiftServer(String ip, int port) {
		return shiftServerV4(ip, port);
	}

	private boolean shiftServerV4(String ip, int port) {
		final String[] serverIps = getServerIps();
		final int[] ports = getPorts();
		if (serverIps == null) {
			return false;
		}
		if (!(ip.equals(serverIps[mCurrentServerIndex]) && (ports == null
			|| ports[mCurrentServerIndex] == port))) {
			return false;
		}
		mCurrentServerIndex++;
		if (mCurrentServerIndex >= serverIps.length) {
			mCurrentServerIndex = 0;
		}
		return mCurrentServerIndex == mLastOkServerIndex;
	}

	public boolean shiftServerV6(String ip, int port) {
		final String[] serverIps = getIpv6ServerIps();
		final int[] ports = getIpv6Ports();
		if (serverIps == null) {
			return false;
		}
		if (!(ip.equals(serverIps[mCurrentServerIndexForV6]) && (ports == null
			|| ports[mCurrentServerIndexForV6] == port))) {
			return false;
		}
		mCurrentServerIndexForV6++;
		if (mCurrentServerIndexForV6 >= serverIps.length) {
			mCurrentServerIndexForV6 = 0;
		}
		return mCurrentServerIndexForV6 == mLastOkServerIndexForV6;
	}

	/**
	 * 标记当前好用的域名解析服务
	 *
	 * @return 标记成功与否
	 */
	public boolean markOkServer(String serverIp, int port) {
		final String[] serverIps = getServerIps();
		final int[] ports = getPorts();
		if (serverIps == null) {
			return false;
		}
		if (serverIps[mCurrentServerIndex].equals(serverIp) && (ports == null
			|| ports[mCurrentServerIndex] == port)) {
			if (mLastOkServerIndex != mCurrentServerIndex) {
				mLastOkServerIndex = mCurrentServerIndex;
				mHttpDnsConfig.saveToCache();
			}
			return true;
		}
		return false;
	}

	/**
	 * 标记当前好用的域名解析服务
	 *
	 * @return 标记成功与否
	 */
	public boolean markOkServerV6(String serverIp, int port) {
		final String[] serverIps = getIpv6ServerIps();
		final int[] ports = getIpv6Ports();
		if (serverIps == null) {
			return false;
		}
		if (serverIps[mCurrentServerIndexForV6].equals(serverIp) && (ports == null
			|| ports[mCurrentServerIndexForV6] == port)) {
			if (mLastOkServerIndexForV6 != mCurrentServerIndexForV6) {
				mLastOkServerIndexForV6 = mCurrentServerIndexForV6;
				mHttpDnsConfig.saveToCache();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {return true;}
		if (o == null || getClass() != o.getClass()) {return false;}
		if (!super.equals(o)) {return false;}
		ServerConfig that = (ServerConfig)o;
		return mLastOkServerIndex == that.mLastOkServerIndex &&
			mCurrentServerIndex == that.mCurrentServerIndex &&
			mLastOkServerIndexForV6 == that.mLastOkServerIndexForV6 &&
			mCurrentServerIndexForV6 == that.mCurrentServerIndexForV6 &&
			mServerIpsLastUpdatedTime == that.mServerIpsLastUpdatedTime &&
			mHttpDnsConfig.equals(that.mHttpDnsConfig);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(
			new Object[] {super.hashCode(), mHttpDnsConfig, mLastOkServerIndex,
				mCurrentServerIndex,
				mLastOkServerIndexForV6, mCurrentServerIndexForV6, mServerIpsLastUpdatedTime});
	}

	@Override
	public void restoreFromCache(SharedPreferences sp) {
		String cachedServerRegion = sp.getString(Constants.CONFIG_CURRENT_SERVER_REGION,
				getRegion());
		//初始化region和缓存server region一致的情况，使用缓存的服务IP。否则初始化的region优先级更高
		if (TextUtils.equals(cachedServerRegion, getRegion())) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("restore service ip of " + (TextUtils.isEmpty(cachedServerRegion) ? "default" : cachedServerRegion));
			}
			String[] serverIps = CommonUtil.parseStringArray(sp.getString(Constants.CONFIG_KEY_SERVERS,
					CommonUtil.translateStringArray(getServerIps())));
			int[] ports = CommonUtil.parsePorts(
					sp.getString(Constants.CONFIG_KEY_PORTS, CommonUtil.translateIntArray(getPorts())));
			String[] serverV6Ips = CommonUtil.parseStringArray(
					sp.getString(Constants.CONFIG_KEY_SERVERS_IPV6,
							CommonUtil.translateStringArray(getIpv6ServerIps())));
			int[] v6Ports = CommonUtil.parsePorts(sp.getString(Constants.CONFIG_KEY_PORTS_IPV6,
					CommonUtil.translateIntArray(getIpv6Ports())));

			updateAll(cachedServerRegion, serverIps, ports, serverV6Ips, v6Ports);
			mServerIpsLastUpdatedTime = sp.getLong(Constants.CONFIG_SERVERS_LAST_UPDATED_TIME, 0);
		}
	}

	@Override
	public void saveToCache(SharedPreferences.Editor editor) {
		editor.putString(Constants.CONFIG_KEY_SERVERS,
			CommonUtil.translateStringArray(getServerIps()));
		editor.putString(Constants.CONFIG_KEY_PORTS, CommonUtil.translateIntArray(getPorts()));
		editor.putInt(Constants.CONFIG_CURRENT_INDEX, mCurrentServerIndex);
		editor.putInt(Constants.CONFIG_LAST_INDEX, mLastOkServerIndex);
		editor.putString(Constants.CONFIG_KEY_SERVERS_IPV6,
			CommonUtil.translateStringArray(getIpv6ServerIps()));
		editor.putString(Constants.CONFIG_KEY_PORTS_IPV6,
			CommonUtil.translateIntArray(getIpv6Ports()));
		editor.putInt(Constants.CONFIG_CURRENT_INDEX_IPV6, mCurrentServerIndexForV6);
		editor.putInt(Constants.CONFIG_LAST_INDEX_IPV6, mLastOkServerIndexForV6);
		editor.putLong(Constants.CONFIG_SERVERS_LAST_UPDATED_TIME, mServerIpsLastUpdatedTime);
		editor.putString(Constants.CONFIG_CURRENT_SERVER_REGION, getRegion());
	}
}
