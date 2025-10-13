package com.alibaba.sdk.android.httpdns.net;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;

import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.utils.ThreadUtil;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Network;
import android.net.LinkProperties;
import android.net.LinkAddress;
import android.os.Build;
import android.os.Process;

public class NetworkStateManager {
	public static final String NONE_NETWORK = "None_Network";
	private static final String NETWORK_KEY_PREFIX = "emasInner_";
	private Context mContext;
	private String mLastConnectedNetwork = NONE_NETWORK;
	private volatile String mCurrentNetworkKey = NONE_NETWORK;
	private final ArrayList<OnNetworkChange> mListeners = new ArrayList<>();

	private final ExecutorService mWorker = ThreadUtil.createSingleThreadService("network");

	private static class Holder {
		private static final NetworkStateManager instance = new NetworkStateManager();
	}

	public static NetworkStateManager getInstance() {
		return Holder.instance;
	}

	private NetworkStateManager() {
	}

	public void init(Context ctx) {
		if (ctx == null) {
			throw new IllegalStateException("Context can't be null");
		}
		if (this.mContext != null) {
			// inited
			return;
		}
		this.mContext = ctx.getApplicationContext();
		
		// 立即检测当前网络状态
		mWorker.execute(() -> {
			try {
				String currentNetwork = detectCurrentNetwork();
				if (!currentNetwork.equals(NONE_NETWORK)) {
					mLastConnectedNetwork = currentNetwork;
				}
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.d("[NetworkStateManager.init] - Initial network detected: " + currentNetwork);
				}
			} catch (Exception e) {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.e("[NetworkStateManager.init] - Failed to detect initial network", e);
				}
			}
		});

		BroadcastReceiver bcReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(final Context context, final Intent intent) {
				try {
					mWorker.execute(new Runnable() {
						@Override
						public void run() {
							try {
								if (isInitialStickyBroadcast()) { // no need to handle initial
                                    // sticky broadcast
									return;
								}
								String action = intent.getAction();
								if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)
									&& hasNetInfoPermission(context)) {
									String currentNetwork = detectCurrentNetwork();
									HttpDnsNetworkDetector.getInstance().cleanCache(
										!currentNetwork.equals(NONE_NETWORK));
									if (!currentNetwork.equals(NONE_NETWORK)
										&& !currentNetwork.equalsIgnoreCase(mLastConnectedNetwork)) {
										for (OnNetworkChange onNetworkChange : mListeners) {
											onNetworkChange.onNetworkChange(currentNetwork);
										}
									}
									if (!currentNetwork.equals(NONE_NETWORK)) {
										mLastConnectedNetwork = currentNetwork;
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				} catch (Exception ignored) {
				}
			}
		};

		try {
			if (hasNetInfoPermission(mContext)) {
				IntentFilter mFilter = new IntentFilter();
				mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
				mContext.registerReceiver(bcReceiver, mFilter);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String detectCurrentNetwork() {
		try {
			ConnectivityManager connectivityManager = (ConnectivityManager)mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo info = connectivityManager.getActiveNetworkInfo();
			if (info != null && info.isAvailable() && info.isConnected()) {
				String name = info.getTypeName();
				
				String ipPrefix = getLocalIpPrefix();
				if (ipPrefix != null) {
					name = name + "_" + ipPrefix;
				}
				// 增加前缀，防止与用户cacheKey冲突
				mCurrentNetworkKey = (name == null) ? NONE_NETWORK : (NETWORK_KEY_PREFIX + name);
				
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.d("[detectCurrentNetwork] - Network key:" + mCurrentNetworkKey + " subType "
                        + "name: "
						+ info.getSubtypeName());
				}
				return mCurrentNetworkKey;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		mCurrentNetworkKey = NONE_NETWORK;
		return NONE_NETWORK;
	}

	private String getLocalIpPrefix() {
		try {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
				return null;
			}

			ConnectivityManager connectivityManager = (ConnectivityManager) mContext
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			Network activeNetwork = connectivityManager.getActiveNetwork();

			if (activeNetwork == null) {
				return null;
			}

			LinkProperties linkProperties = connectivityManager.getLinkProperties(activeNetwork);
			if (linkProperties == null) {
				return null;
			}

			String ipv6Backup = null;
			for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
				InetAddress address = linkAddress.getAddress();
				if (address.isLoopbackAddress() || address.isLinkLocalAddress()) {
					continue;
				}

				if (address instanceof Inet4Address) {
					String ip = address.getHostAddress();
					String[] parts = ip.split("\\.");
					if (parts.length >= 3) {
						return parts[0] + "." + parts[1] + "." + parts[2];
					}
				} else if (address instanceof Inet6Address && ipv6Backup == null) {
					String ipv6 = address.getHostAddress();
					if (ipv6.contains("%")) {
						ipv6 = ipv6.substring(0, ipv6.indexOf("%"));
					}
					String[] parts = ipv6.split(":");
					if (parts.length >= 4) {
						ipv6Backup = parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3];
					}
				}
			}
			return ipv6Backup;
		} catch (Exception e) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("[getLocalIpPrefix] - Failed to get IP prefix: " + e.getMessage());
			}
		}
		return null;
	}

	public String getCurrentNetworkKey() {
		return mCurrentNetworkKey;
	}

	public void addListener(OnNetworkChange change) {
		this.mListeners.add(change);
	}

	public interface OnNetworkChange {
		void onNetworkChange(String networkType);
	}

	public void reset() {
		mContext = null;
		mLastConnectedNetwork = NONE_NETWORK;
		mCurrentNetworkKey = NONE_NETWORK;
		mListeners.clear();
	}

	private static boolean hasNetInfoPermission(Context context) {
		try {
			return checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)
				== PackageManager.PERMISSION_GRANTED;
		} catch (Throwable e) {
			HttpDnsLog.w("check network info permission fail", e);
		}
		return false;
	}

	private static int checkSelfPermission(Context context, String permission) {
		return context.checkPermission(permission, Process.myPid(), Process.myUid());
	}
}
