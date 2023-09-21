package com.alibaba.sdk.android.httpdns.net;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

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
import android.os.Process;

public class NetworkStateManager {
	public static final String NONE_NETWORK = "None_Network";
	private Context mContext;
	private String mLastConnectedNetwork = NONE_NETWORK;
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
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.d("[detectCurrentNetwork] - Network name:" + name + " subType "
                        + "name: "
						+ info.getSubtypeName());
				}
				return name == null ? NONE_NETWORK : name;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return NONE_NETWORK;
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
