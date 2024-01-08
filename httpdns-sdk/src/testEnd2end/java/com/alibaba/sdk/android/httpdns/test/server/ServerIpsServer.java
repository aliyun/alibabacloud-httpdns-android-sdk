package com.alibaba.sdk.android.httpdns.test.server;

import com.alibaba.sdk.android.httpdns.serverip.UpdateRegionServerResponse;
import com.alibaba.sdk.android.httpdns.test.server.base.BaseDataServer;

import org.json.JSONException;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;

/**
 * 服务IP更新接口服务
 *
 * @author zonglin.nzl
 * @date 2020/11/9
 */
public class ServerIpsServer extends BaseDataServer<String, UpdateRegionServerResponse> {

	@Override
	public String convert(UpdateRegionServerResponse updateRegionServerResponse) {
		return toResponseBodyStr(updateRegionServerResponse);
	}

	@Override
	public UpdateRegionServerResponse convert(String body) {
		try {
			return UpdateRegionServerResponse.fromResponse(body);
		} catch (JSONException e) {
			throw new IllegalStateException("解析服务IP数据失败", e);
		}
	}

	@Override
	public UpdateRegionServerResponse randomData(String s) {
		return new UpdateRegionServerResponse(null, null, null, null);
	}

	@Override
	public String getRequestArg(RecordedRequest recordedRequest) {
		return getRegionIfIsUpdateServerRequest(recordedRequest);
	}

	@Override
	public boolean isMyBusinessRequest(RecordedRequest request) {
		return getRegionIfIsUpdateServerRequest(request) != null;
	}

	/**
	 * 构建 调度接口结果字符串
	 *
	 * @param enable
	 * @param serverIps
	 * @param serverIpv6s
	 * @param ports
	 * @param v6ports
	 * @return
	 */
	private static StringBuilder constructUpdateServerResultBody(boolean enable,
																 String[] serverIps,
																 String[] serverIpv6s, int[] ports,
																 int[] v6ports) {
		StringBuilder stringBuilder = new StringBuilder();
		// "{\"service_ip\":[\""+serverIp+"\"],\"service_ipv6\":[\"2401:b180:2000:20::10\"]}"
		boolean hasValue = false;
		stringBuilder.append("{");
		if (!enable) {
			if (hasValue) {
				stringBuilder.append(",");
			}
			hasValue = true;
			stringBuilder.append("\"service_status\":\"disable\"");
		}
		if (serverIps != null) {
			if (hasValue) {
				stringBuilder.append(",");
			}
			hasValue = true;
			stringBuilder.append("\"service_ip\":[");
			for (int i = 0; i < serverIps.length; i++) {
				if (i != 0) {
					stringBuilder.append(",");
				}
				stringBuilder.append("\"").append(serverIps[i]).append("\"");
			}
			stringBuilder.append("]");
		}
		if (serverIpv6s != null) {
			if (hasValue) {
				stringBuilder.append(",");
			}
			hasValue = true;
			stringBuilder.append("\"service_ipv6\":[");
			for (int i = 0; i < serverIpv6s.length; i++) {
				if (i != 0) {
					stringBuilder.append(",");
				}
				stringBuilder.append("\"").append(serverIpv6s[i]).append("\"");
			}
			stringBuilder.append("]");
		}
		if (ports != null) {
			if (hasValue) {
				stringBuilder.append(",");
			}
			hasValue = true;
			stringBuilder.append("\"service_ip_port\":[");
			for (int i = 0; i < ports.length; i++) {
				if (i != 0) {
					stringBuilder.append(",");
				}
				stringBuilder.append(ports[i]);
			}
			stringBuilder.append("]");
		}
		if (v6ports != null) {
			if (hasValue) {
				stringBuilder.append(",");
			}
			hasValue = true;
			stringBuilder.append("\"service_ipv6_port\":[");
			for (int i = 0; i < v6ports.length; i++) {
				if (i != 0) {
					stringBuilder.append(",");
				}
				stringBuilder.append(v6ports[i]);
			}
			stringBuilder.append("]");
		}
		stringBuilder.append("}");
		return stringBuilder;
	}

	/**
	 * 服务侧 从调度请求中获取 region 参数
	 *
	 * @param request
	 * @return
	 */
	private static String getRegionIfIsUpdateServerRequest(RecordedRequest request) {
		List<String> pathSegments = request.getRequestUrl().pathSegments();
		if (pathSegments.size() == 2 && pathSegments.contains("ss")) {
			String region = request.getRequestUrl().queryParameter("region");
			if (region == null) {
				return "";
			} else {
				return region;
			}
		}
		return null;
	}

	/**
	 * 构建 调度结果 下行数据
	 *
	 * @param serverIps
	 * @param serverIpv6s
	 * @param ports
	 * @param v6ports
	 * @return
	 */
	public static String createUpdateServerResponse(String[] serverIps, String[] serverIpv6s,
													int[] ports, int[] v6ports) {
		return constructUpdateServerResultBody(true, serverIps, serverIpv6s, ports,
			v6ports).toString();
	}

	/**
	 * 将调度结果 改为下行body字符串
	 *
	 * @param updateRegionServerResponse
	 * @return
	 */
	private static String toResponseBodyStr(UpdateRegionServerResponse updateRegionServerResponse) {
		return constructUpdateServerResultBody(updateRegionServerResponse.isEnable(),
			updateRegionServerResponse.getServerIps(), updateRegionServerResponse.getServerIpv6s(),
			updateRegionServerResponse.getServerPorts(),
			updateRegionServerResponse.getServerIpv6Ports()).toString();
	}

	/**
	 * 创建含义为禁止服务的 调度解析结果
	 *
	 * @return
	 */
	public static String createUpdateServerDisableResponse() {
		return constructUpdateServerResultBody(false, null, null, null, null).toString();
	}
}
