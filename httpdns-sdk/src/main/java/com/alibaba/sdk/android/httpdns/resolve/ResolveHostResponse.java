package com.alibaba.sdk.android.httpdns.resolve;

import java.util.Arrays;

import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 域名解析服务返回的数据结构
 */
public class ResolveHostResponse {
	private final String mHostName;
	private final String[] mIps;
	private final String[] mIpsV6;
	private final int mTtl;
	private final String mExtra;

	/**
	 * 数据解析
	 */
	public static ResolveHostResponse fromResponse(String jsonString) throws JSONException {
		JSONObject jsonObject = new JSONObject(jsonString);
		String hostName = jsonObject.getString("host");
		String[] ips = null;
		String[] ipsv6 = null;
		String extra = null;
		int ttl = 0;
		try {
			if (jsonObject.has("ips")) {
				JSONArray ipsArray = jsonObject.getJSONArray("ips");
				int len = ipsArray.length();
				ips = new String[len];
				for (int i = 0; i < len; i++) {
					ips[i] = ipsArray.getString(i);
				}
			}
			// ipv6解析逻辑
			if (jsonObject.has("ipsv6")) {
				JSONArray ipsv6Array = jsonObject.getJSONArray("ipsv6");
				if (ipsv6Array.length() != 0) {
					ipsv6 = new String[ipsv6Array.length()];
					for (int i = 0; i < ipsv6Array.length(); i++) {
						ipsv6[i] = ipsv6Array.getString(i);
					}
				}
			}
			//额外参数解析逻辑
			if (jsonObject.has("extra")) {
				extra = jsonObject.getString("extra");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		ttl = jsonObject.getInt("ttl");
		return new ResolveHostResponse(hostName, ips, ipsv6, ttl, extra);
	}

	public static ResolveHostResponse createEmpty(String host, int ttl) {
		return new ResolveHostResponse(host, null, null, ttl, null);
	}

	public ResolveHostResponse(String hostName, String[] ips, String[] ipsv6, int ttl,
							   String extra) {
		this.mHostName = hostName;
		if (ips != null) {
			this.mIps = ips;
		} else {
			this.mIps = new String[0];
		}
		if (ipsv6 != null) {
			this.mIpsV6 = ipsv6;
		} else {
			this.mIpsV6 = new String[0];
		}
		if (ttl > 0) {
			this.mTtl = ttl;
		} else {
			this.mTtl = 60;
		}
		this.mExtra = extra;
	}

	public String getHostName() {
		return mHostName;
	}

	public String[] getIps() {
		return mIps;
	}

	public String[] getIpsV6() {
		return mIpsV6;
	}

	public int getTtl() {
		return mTtl;
	}

	public String getExtras() {
		return mExtra;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder(
            "host: " + mHostName + " ip cnt: " + mIps.length + " ttl: "
                + mTtl);
        for (String ip : mIps) {
            ret.append("\n ip: ").append(ip);
        }
        ret.append("\n ipv6 cnt: ").append(mIpsV6.length);
        for (String s : mIpsV6) {
            ret.append("\n ipv6: ").append(s);
        }
        ret.append("\n extra: ").append(mExtra);

		return ret.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ResolveHostResponse that = (ResolveHostResponse)o;
		return mTtl == that.mTtl &&
			mHostName.equals(that.mHostName) &&
			Arrays.equals(mIps, that.mIps) &&
			Arrays.equals(mIpsV6, that.mIpsV6) &&
			CommonUtil.equals(mExtra, that.mExtra);
	}

	@Override
	public int hashCode() {
		int result = Arrays.hashCode(new Object[] {mHostName, mTtl, mExtra});
		result = 31 * result + Arrays.hashCode(mIps);
		result = 31 * result + Arrays.hashCode(mIpsV6);
		return result;
	}
}
