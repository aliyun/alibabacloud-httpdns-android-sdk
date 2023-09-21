package com.alibaba.sdk.android.httpdns.probe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

/**
 * ip优选实现
 */
public class ProbeTask implements Runnable {

	public interface SpeedTestSocketFactory {
		Socket create();
	}

	private final SpeedTestSocketFactory mSocketFactory;
	private final String mHost;
	private final String[] mIps;
	private final IPProbeItem mIpProbeItem;
	private final ProbeCallback mProbeCallback;

	public ProbeTask(SpeedTestSocketFactory socketFactory, String host, String[] ips,
					 IPProbeItem ipProbeItem, ProbeCallback probeCallback) {
		this.mSocketFactory = socketFactory;
		this.mHost = host;
		this.mIps = ips;
		this.mIpProbeItem = ipProbeItem;
		this.mProbeCallback = probeCallback;
	}

	@Override
	public void run() {
		int[] speeds = new int[mIps.length];
		for (int i = 0; i < mIps.length; i++) {
			speeds[i] = testConnectSpeed(mIps[i], mIpProbeItem.getPort());
		}
		String[] result = CommonUtil.sortIpsWithSpeeds(mIps, speeds);
		if (mProbeCallback != null) {
			mProbeCallback.onResult(mHost, result);
		}
	}

	private int testConnectSpeed(String ip, int port) {
		Socket socket = mSocketFactory.create();
		long start = System.currentTimeMillis();
		long end = Long.MAX_VALUE;
		SocketAddress remoteAddress = new InetSocketAddress(ip, port);
		try {
			socket.connect(remoteAddress, 5 * 1000);
			end = System.currentTimeMillis();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (end == Long.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int)(end - start);

	}
}
