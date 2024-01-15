package com.alibaba.sdk.android.httpdns.resolve;

import com.alibaba.sdk.android.httpdns.RequestIpType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量解析的结果
 */
public class BatchResolveHostResponse {

	private final ArrayList<HostItem> mHostItems;

	public BatchResolveHostResponse(ArrayList<HostItem> items) {
		this.mHostItems = items;
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

	public static class HostItem {
		private final String mHost;
		private final RequestIpType mIpType;
		private final String[] mIps;
		private final int mTtl;

		public HostItem(String host, RequestIpType type, String[] ips, int ttl) {
			this.mHost = host;
			this.mIpType = type;
			this.mIps = ips;
			if (ttl <= 0) {
				this.mTtl = 60;
			} else {
				this.mTtl = ttl;
			}
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
	}

	public static BatchResolveHostResponse fromResponse(String body) throws JSONException {
		// {"dns":[{"host":"www.taobao.com","ips":["124.239.239.235","124.239.159.105"],"type":1,
		// "ttl":31},{"host":"www.taobao.com","ips":["240e:b1:9801:400:3:0:0:3fa",
		// "240e:b1:a820:0:3:0:0:3f6"],"type":28,"ttl":60}]}
		JSONObject jsonObject = new JSONObject(body);
		if (!jsonObject.has("dns")) {
			return null;
		}
		JSONArray dns = jsonObject.getJSONArray("dns");
		ArrayList<HostItem> items = new ArrayList<>();
		String host;
		String[] ips;
		int type;
		int ttl;
		for (int i = 0; i < dns.length(); i++) {
			JSONObject itemJson = dns.getJSONObject(i);
			host = itemJson.getString("host");
			type = itemJson.getInt("type");
			ttl = itemJson.getInt("ttl");
			ips = null;
			if (itemJson.has("ips")) {
				JSONArray ipsArray = itemJson.getJSONArray("ips");
				int len = ipsArray.length();
				ips = new String[len];
				for (int j = 0; j < len; j++) {
					ips[j] = ipsArray.getString(j);
				}
			}

			HostItem item = null;
			if (type == 1) {
				item = new HostItem(host, RequestIpType.v4, ips, ttl);
			} else if (type == 28) {
				item = new HostItem(host, RequestIpType.v6, ips, ttl);
			}
			if (item != null) {
				items.add(item);
			}
		}
		return new BatchResolveHostResponse(items);
	}

	public static BatchResolveHostResponse createEmpty(List<String> hostList, RequestIpType type,
													   int ttl) {
		ArrayList<HostItem> list = new ArrayList<>();
		for (String host : hostList) {
			if (type == RequestIpType.v4 || type == RequestIpType.both) {
				list.add(new HostItem(host, RequestIpType.v4, null, ttl));
			}
			if (type == RequestIpType.v6 || type == RequestIpType.both) {
				list.add(new HostItem(host, RequestIpType.v6, null, ttl));
			}
		}
		return new BatchResolveHostResponse(list);
	}
}
