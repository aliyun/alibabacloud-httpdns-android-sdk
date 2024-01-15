package com.alibaba.sdk.android.httpdns.serverip;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 更新服务IP返回接口
 */
public class UpdateRegionServerResponse {
	private final boolean mEnable;
	private String[] mServerIps;
	private String[] mServerIpv6s;
	private int[] mServerPorts;
	private int[] mServerIpv6Ports;

	public UpdateRegionServerResponse(String[] serverIps, String[] serverIpv6s, int[] serverPorts,
									  int[] serverIpv6Ports) {
		this.mServerIps = serverIps;
		this.mServerIpv6s = serverIpv6s;
		this.mServerPorts = serverPorts;
		this.mServerIpv6Ports = serverIpv6Ports;
		this.mEnable = true;
	}

	public UpdateRegionServerResponse(boolean enable, String[] serverIps, String[] serverIpv6s,
									  int[] serverPorts, int[] serverIpv6Ports) {
		this.mEnable = enable;
		this.mServerIps = serverIps;
		this.mServerIpv6s = serverIpv6s;
		this.mServerPorts = serverPorts;
		this.mServerIpv6Ports = serverIpv6Ports;
	}

	public String[] getServerIps() {
		return mServerIps;
	}

	public void setServerIps(String[] serverIps) {
		this.mServerIps = serverIps;
	}

	public String[] getServerIpv6s() {
		return mServerIpv6s;
	}

	public void setServerIpv6s(String[] serverIpv6s) {
		this.mServerIpv6s = serverIpv6s;
	}

	public int[] getServerPorts() {
		return mServerPorts;
	}

	public void setServerPorts(int[] serverPorts) {
		this.mServerPorts = serverPorts;
	}

	public int[] getServerIpv6Ports() {
		return mServerIpv6Ports;
	}

	public void setServerIpv6Ports(int[] serverIpv6Ports) {
		this.mServerIpv6Ports = serverIpv6Ports;
	}

	public boolean isEnable() {
		return mEnable;
	}

	public static UpdateRegionServerResponse fromResponse(String response) throws JSONException {
		JSONObject jsonObject = new JSONObject(response);
		boolean enable = true;
		if (jsonObject.has("service_status")) {
			enable = !jsonObject.optString("service_status").equals("disable");
		}
		String[] ips = null;
		if (jsonObject.has("service_ip")) {
			JSONArray ipsArray = jsonObject.getJSONArray("service_ip");
			int len = ipsArray.length();
			ips = new String[len];
			for (int i = 0; i < len; i++) {
				ips[i] = ipsArray.getString(i);
			}
		}
		String[] ipv6s = null;
		if (jsonObject.has("service_ipv6")) {
			JSONArray ipsArray = jsonObject.getJSONArray("service_ipv6");
			int len = ipsArray.length();
			ipv6s = new String[len];
			for (int i = 0; i < len; i++) {
				ipv6s[i] = ipsArray.getString(i);
			}
		}
		int[] ports = null;
		if (jsonObject.has("service_ip_port")) {
			JSONArray ipsArray = jsonObject.getJSONArray("service_ip_port");
			int len = ipsArray.length();
			ports = new int[len];
			for (int i = 0; i < len; i++) {
				ports[i] = ipsArray.optInt(i);
			}
		}
		int[] v6Ports = null;
		if (jsonObject.has("service_ipv6_port")) {
			JSONArray ipsArray = jsonObject.getJSONArray("service_ipv6_port");
			int len = ipsArray.length();
			v6Ports = new int[len];
			for (int i = 0; i < len; i++) {
				v6Ports[i] = ipsArray.optInt(i);
			}
		}
		return new UpdateRegionServerResponse(enable, ips, ipv6s, ports, v6Ports);
	}

	@Override
	public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
		UpdateRegionServerResponse that = (UpdateRegionServerResponse)o;
		return mEnable == that.mEnable &&
			Arrays.equals(mServerIps, that.mServerIps) &&
			Arrays.equals(mServerIpv6s, that.mServerIpv6s) &&
			Arrays.equals(mServerPorts, that.mServerPorts) &&
			Arrays.equals(mServerIpv6Ports, that.mServerIpv6Ports);
	}

	@Override
	public int hashCode() {
		int result = Arrays.hashCode(new Object[] {mEnable});
		result = 31 * result + Arrays.hashCode(mServerIps);
		result = 31 * result + Arrays.hashCode(mServerIpv6s);
		result = 31 * result + Arrays.hashCode(mServerPorts);
		result = 31 * result + Arrays.hashCode(mServerIpv6Ports);
		return result;
	}

	@Override
	public String toString() {
		return "UpdateServerResponse{" +
			"enable=" + mEnable +
			", serverIps=" + Arrays.toString(mServerIps) +
			", serverIpv6s=" + Arrays.toString(mServerIpv6s) +
			", serverPorts=" + Arrays.toString(mServerPorts) +
			", serverIpv6Ports=" + Arrays.toString(mServerIpv6Ports) +
			'}';
	}
}
