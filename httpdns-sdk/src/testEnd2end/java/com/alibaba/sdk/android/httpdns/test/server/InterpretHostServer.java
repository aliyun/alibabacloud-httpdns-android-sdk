package com.alibaba.sdk.android.httpdns.test.server;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.interpret.InterpretHostResponse;
import com.alibaba.sdk.android.httpdns.test.server.base.BaseDataServer;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import okhttp3.mockwebserver.RecordedRequest;

/**
 * 域名解析服务
 *
 * @author zonglin.nzl
 * @date 2020/11/9
 */
public class InterpretHostServer
	extends BaseDataServer<InterpretHostServer.InterpretHostArg, InterpretHostResponse> {

	private SecretService secretService;

	public InterpretHostServer(SecretService secretService) {
		this.secretService = secretService;
	}

	@Override
	public String convert(InterpretHostResponse interpretHostResponse) {
		return toResponseBodyStr(interpretHostResponse);
	}

	@Override
	public InterpretHostResponse convert(String body) {
		try {
			return InterpretHostResponse.fromResponse(body);
		} catch (JSONException e) {
			throw new IllegalStateException("解析域名ip数据失败", e);
		}
	}

	@Override
	public InterpretHostResponse randomData(InterpretHostServer.InterpretHostArg arg) {
		return randomInterpretHostResponse(arg.host);
	}

	@Override
	public InterpretHostServer.InterpretHostArg getRequestArg(RecordedRequest recordedRequest) {
		return InterpretHostServer.InterpretHostArg.createFromInterpretHostRequest(recordedRequest);
	}

	@Override
	public boolean isMyBusinessRequest(RecordedRequest request) {
		return InterpretHostServer.InterpretHostArg.createFromInterpretHostRequest(request) != null
			&& SecretService.checkSign(secretService, request);
	}

	/**
	 * 构建 解析结果字符串builder
	 *
	 * @param targetHost
	 * @param resultIps
	 * @param resultIpv6s
	 * @param ttl
	 * @param extra
	 * @return
	 */
	public static StringBuilder constructInterpretHostResultBody(String targetHost,
																 String[] resultIps,
																 String[] resultIpv6s, int ttl,
																 String extra) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("{\"host\":\"");
		stringBuilder.append(targetHost);
		stringBuilder.append("\",\"ips\":");
		if (resultIps.length == 0) {
			stringBuilder.append("[]");
		} else {
			for (int i = 0; i < resultIps.length; i++) {
				if (i == 0) {
					stringBuilder.append("[\"");
				}
				stringBuilder.append(resultIps[i]);
				if (i == resultIps.length - 1) {
					stringBuilder.append("\"]");
				} else {
					stringBuilder.append("\",\"");
				}
			}
		}
		if (resultIpv6s != null && resultIpv6s.length != 0) {
			stringBuilder.append(",\"ipsv6\":");
			for (int i = 0; i < resultIpv6s.length; i++) {
				if (i == 0) {
					stringBuilder.append("[\"");
				}
				stringBuilder.append(resultIpv6s[i]);
				if (i == resultIpv6s.length - 1) {
					stringBuilder.append("\"]");
				} else {
					stringBuilder.append("\",\"");
				}
			}
		}
		stringBuilder.append(",\"ttl\":");
		stringBuilder.append(ttl);
		if (extra != null) {
			stringBuilder.append(",\"extra\":\"" + extra.replace("\"", "\\\"") + "\"");
		}
		stringBuilder.append(",\"origin_ttl\":60,\"client_ip\":\"106.11.41.215\"}");
		return stringBuilder;
	}

	/**
	 * 将解析结果 改为 下行body字符串
	 *
	 * @param response
	 * @return
	 */
	public static String toResponseBodyStr(InterpretHostResponse response) {
		return constructInterpretHostResultBody(response.getHostName(), response.getIps(),
			response.getIpsV6(), (int)response.getTtl(), response.getExtras()).toString();
	}

	/**
	 * 构建 解析结果，数据随机
	 *
	 * @param host
	 * @return
	 */
	public static InterpretHostResponse randomInterpretHostResponse(String host) {
		return new InterpretHostResponse(host, RandomValue.randomIpv4s(),
			RandomValue.randomIpv6s(),
			RandomValue.randomInt(60), RandomValue.randomJsonMap());
	}

	/**
	 * 构建 解析结果，ttl指定，ip数据随机
	 *
	 * @param host
	 * @param ttl
	 * @return
	 */
	public static InterpretHostResponse randomInterpretHostResponse(String host, int ttl) {
		return new InterpretHostResponse(host, RandomValue.randomIpv4s(),
			RandomValue.randomIpv6s(),
			ttl, RandomValue.randomJsonMap());
	}

	/**
	 * 解析服务的参数
	 */
	public static class InterpretHostArg {
		public String host;
		public RequestIpType type = RequestIpType.v4;
		public HashMap<String, String> extras = new HashMap<>();

		public static InterpretHostArg create(String host) {
			InterpretHostArg interpretHostArg = new InterpretHostArg();
			interpretHostArg.host = host;
			return interpretHostArg;
		}

		public static InterpretHostArg create(String host, RequestIpType type) {
			InterpretHostArg interpretHostArg = new InterpretHostArg();
			interpretHostArg.host = host;
			interpretHostArg.type = type;
			return interpretHostArg;
		}

		public static InterpretHostArg create(String host, RequestIpType type,
											  HashMap<String, String> extras) {
			InterpretHostArg interpretHostArg = new InterpretHostArg();
			interpretHostArg.host = host;
			interpretHostArg.type = type;
			interpretHostArg.extras = extras;
			return interpretHostArg;
		}

		public static InterpretHostArg createFromInterpretHostRequest(
			RecordedRequest recordedRequest) {
			List<String> pathSegments = recordedRequest.getRequestUrl().pathSegments();
			if (pathSegments.size() == 2 && (pathSegments.contains("d") || pathSegments.contains(
				"sign_d"))) {
				String host = recordedRequest.getRequestUrl().queryParameter("host");
				RequestIpType type = getQueryType(recordedRequest);
				HashMap<String, String> extras = getExtras(recordedRequest);
				return create(host, type, extras);
			}
			return null;
		}

		private static RequestIpType getQueryType(RecordedRequest recordedRequest) {
			String query = recordedRequest.getRequestUrl().queryParameter("query");
			RequestIpType type = RequestIpType.v4;
			if (query != null && query.contains("6") && query.contains("4")) {
				type = RequestIpType.both;
			} else if (query != null && query.contains("6")) {
				type = RequestIpType.v6;
			}
			return type;
		}

		private static HashMap<String, String> getExtras(RecordedRequest recordedRequest) {
			Set<String> queryKeys = recordedRequest.getRequestUrl().queryParameterNames();
			ArrayList<String> sdnsKeys = new ArrayList<>();
			for (String qKey : queryKeys) {
				if (qKey.startsWith("sdns-")) {
					sdnsKeys.add(qKey);
				}
			}
			HashMap<String, String> extras = new HashMap<>();
			for (String sKey : sdnsKeys) {
				String value = recordedRequest.getRequestUrl().queryParameter(sKey);
				extras.put(sKey.replace("sdns-", ""), value);
			}
			return extras;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {return true;}
			if (o == null || getClass() != o.getClass()) {return false;}

			String str = formInterpretHostArg(host, type, extras);
			String str1 = formInterpretHostArg(((InterpretHostArg)o).host,
				((InterpretHostArg)o).type, ((InterpretHostArg)o).extras);
			return str.equals(str1);
		}

		@Override
		public int hashCode() {
			String str = formInterpretHostArg(host, type, extras);
			return str.hashCode();
		}

		/**
		 * 构造 服务侧 的参数字符串，是自定义的字符串，用于区分不同的请求和快速获取参数数据
		 * 可以用于匹配请求和获取参数
		 *
		 * @param host
		 * @param type
		 * @param extras
		 * @return
		 */
		private static String formInterpretHostArg(String host, RequestIpType type,
												   HashMap<String, String> extras) {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(host);
			switch (type) {
				case v6:
					stringBuilder.append("&v6");
					break;
				case both:
					stringBuilder.append("&v4v6");
					break;
			}
			if (extras != null && extras.size() > 0) {
				ArrayList<String> keys = new ArrayList<>(extras.keySet());
				Collections.sort(keys);
				for (String key : keys) {
					stringBuilder.append("&" + key + "=" + extras.get(key));
				}
			}
			return stringBuilder.toString();
		}
	}

	public void preSetRequestTimeout(String host, int count) {
		preSetRequestTimeout(InterpretHostArg.create(host), count);
	}

	public void preSetRequestResponse(String host, InterpretHostResponse data, int count) {
		preSetRequestResponse(InterpretHostArg.create(host), data, count);
	}

	public void preSetRequestResponse(String host, int httpCode, String body, int count) {
		preSetRequestResponse(InterpretHostArg.create(host), httpCode, body, count);
	}

	public List<InterpretHostResponse> getResponse(String host, int count, boolean removeRecord) {
		return getResponse(InterpretHostArg.create(host), count, removeRecord);
	}

	public boolean hasRequestForArg(String host, int count, boolean removeRecord) {
		return hasRequestForArg(InterpretHostArg.create(host), count, removeRecord);
	}

	public boolean hasRequestForArgWithParams(String host, List<String> params, int count,
											  boolean removeRecord) {
		return hasRequestForArgWithParams(InterpretHostArg.create(host), params, count,
			removeRecord);
	}

	public boolean hasRequestForArgTimeout(String host, int count, boolean removeRecord) {
		return hasRequestForArgTimeout(InterpretHostArg.create(host), count, removeRecord);
	}

	public boolean hasRequestForArgWithResult(String host, InterpretHostResponse data, int count,
											  boolean removeRecord) {
		return hasRequestForArgWithResult(InterpretHostArg.create(host), data, count,
			removeRecord);
	}

	public boolean hasRequestForArgWithResult(String host, int httpCode, String body, int count,
											  boolean removeRecord) {
		return hasRequestForArgWithResult(InterpretHostArg.create(host), httpCode, body, count,
			removeRecord);
	}

	/**
	 * 创建空解析结果，指定ttl
	 *
	 * @param host
	 * @param ttl
	 * @return
	 */
	public static InterpretHostResponse createResponseWithEmptyIp(String host, int ttl) {
		return createResponse(host, null, null, ttl, null);
	}

	/**
	 * 创建 解析结果数据
	 *
	 * @param targetHost
	 * @param ips
	 * @param ipv6s
	 * @param ttl
	 * @param extra
	 * @return
	 */
	public static InterpretHostResponse createResponse(String targetHost, String[] ips,
													   String[] ipv6s, int ttl, String extra) {
		return new InterpretHostResponse(targetHost, ips, ipv6s, ttl, extra);
	}
}
