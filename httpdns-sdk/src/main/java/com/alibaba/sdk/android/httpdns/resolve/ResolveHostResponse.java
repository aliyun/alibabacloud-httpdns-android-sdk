package com.alibaba.sdk.android.httpdns.resolve;

import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析的结果
 */
public class ResolveHostResponse {

	private final ArrayList<HostItem> mHostItems;
	private String mServerIp;

	public ResolveHostResponse(ArrayList<HostItem> items, String serverIp) {
		this.mHostItems = items;
		mServerIp = serverIp;
	}

	public HostItem getHostItem(String host, RequestIpType type) {
		for (HostItem item : mHostItems) {
			if (item.mHost.equals(host) && item.mIpType == type) {
				return item;
			}
		}
		return null;
	}

	public List<HostItem> getItems() {
		return mHostItems;
	}

	public String getServerIp() {
		return mServerIp;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		if (mHostItems != null) {
			for (int i = 0; i < mHostItems.size(); i++) {
				ret.append(mHostItems.get(i).toString());
				if (i != mHostItems.size() - 1) {
					ret.append("\n");
				}
			}
		}
		return ret.toString();
	}

	public static class HostItem {
		private final String mHost;
		private final RequestIpType mIpType;
		private final String[] mIps;
		private final int mTtl;

		private final String mExtra;

		private final String noIpCode;

		public HostItem(String host, RequestIpType type, String[] ips, int ttl, String extra, String noIpCode) {
			this.mHost = host;
			this.mIpType = type;
			this.mIps = ips;
			if (ttl <= 0) {
				this.mTtl = 60;
			} else {
				this.mTtl = ttl;
			}
			this.mExtra = extra;
			this.noIpCode = noIpCode;
		}

		public String getHost() {
			return mHost;
		}

		public RequestIpType getIpType() {
			return mIpType;
		}

		public String[] getIps() {
			return mIps;
		}

		public int getTtl() {
			return mTtl;
		}

		public String getExtra() {
			return mExtra;
		}

		public String getNoIpCode() {
			return noIpCode;
		}

		@Override
		public String toString() {
			StringBuilder ret = new StringBuilder(
					"host: " + mHost + " ip cnt: " + (mIps != null ? mIps.length : 0) + " ttl: "
							+ mTtl);
			if (mIps != null) {
				for (String ip : mIps) {
					ret.append("\n ip: ").append(ip);
				}
			}

			ret.append("\n extra: ").append(mExtra);
			ret.append("\n noIpCode: ").append(noIpCode);

			return ret.toString();
		}
	}

	public static ResolveHostResponse fromResponse(String serverIp, String body) throws JSONException {

		ArrayList<HostItem> items = new ArrayList<>();
		JSONObject jsonObject = new JSONObject(body);

		if (jsonObject.has("answers")) {
			JSONArray answers = jsonObject.getJSONArray("answers");
			for (int i = 0; i < answers.length(); i++) {
				JSONObject answer = answers.getJSONObject(i);
				String hostName = null;
				int ttl = 0;
				String extra = null;
				String[] ips = null;
				String[] ipsv6 = null;
				String noIpCode = null;
				if (answer.has("dn")) {
					hostName = answer.getString("dn");
				}

				if (answer.has("v4")) {
					JSONObject ipv4 = answer.getJSONObject("v4");
					if (ipv4.has("ips")) {
						JSONArray ipArray = ipv4.getJSONArray("ips");
						if (ipArray.length() != 0) {
							ips = new String[ipArray.length()];
							for (int j = 0; j < ipArray.length(); j++) {
								ips[j] = ipArray.getString(j);
							}
						}
					}
					if (ipv4.has("ttl")) {
						ttl = ipv4.getInt("ttl");
					}
					if (ipv4.has("extra")) {
						extra = ipv4.getString("extra");
					}
					if (ipv4.has("no_ip_code")) {
						noIpCode = ipv4.getString("no_ip_code");
					}
					items.add(new HostItem(hostName, RequestIpType.v4, ips, ttl, extra, noIpCode));
					if (!TextUtils.isEmpty(noIpCode)) {
						HttpDnsLog.w("RESOLVE FAIL, HOST:" + hostName + ", QUERY:4, "
								+ "Msg:" + noIpCode);
					}
				}
				if (answer.has("v6")) {
					JSONObject ipv6 = answer.getJSONObject("v6");
					if (ipv6.has("ips")) {
						JSONArray ipArray = ipv6.getJSONArray("ips");
						if (ipArray.length() != 0) {
							ipsv6 = new String[ipArray.length()];
							for (int j = 0; j < ipArray.length(); j++) {
								ipsv6[j] = ipArray.getString(j);
							}
						}
					}
					if (ipv6.has("ttl")) {
						ttl = ipv6.getInt("ttl");
					}
					if (ipv6.has("extra")) {
						extra = ipv6.getString("extra");
					}
					if (ipv6.has("no_ip_code")) {
						noIpCode = ipv6.getString("no_ip_code");
					}
					items.add(new HostItem(hostName, RequestIpType.v6, ipsv6, ttl, extra, noIpCode));
					if (!TextUtils.isEmpty(noIpCode)) {
						HttpDnsLog.w("RESOLVE FAIL, HOST:" + hostName + ", QUERY:6, "
								+ "Msg:" + noIpCode);
					}
				}
			}

		}
		return new ResolveHostResponse(items, serverIp);
	}

	public static ResolveHostResponse createEmpty(List<String> hostList, RequestIpType type,
                                                  int ttl) {
		ArrayList<HostItem> list = new ArrayList<>();
		for (String host : hostList) {
			if (type == RequestIpType.v4 || type == RequestIpType.both) {
				list.add(new HostItem(host, RequestIpType.v4, null, ttl, null, null));
			}
			if (type == RequestIpType.v6 || type == RequestIpType.both) {
				list.add(new HostItem(host, RequestIpType.v6, null, ttl, null, null));
			}
		}
		return new ResolveHostResponse(list, "");
	}
}
