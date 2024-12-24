package com.alibaba.sdk.android.httpdns.test.server;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.resolve.BatchResolveHostResponse;
import com.alibaba.sdk.android.httpdns.test.server.base.BaseDataServer;
import com.alibaba.sdk.android.httpdns.test.server.base.RequestRecord;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.TestLogger;

import org.json.JSONException;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;

/**
 * @author zonglin.nzl
 * @date 2020/12/9
 */
public class BatchResolveHostServer extends
	BaseDataServer<BatchResolveHostServer.BatchResolveRequestArg, BatchResolveHostResponse> {

	private SecretService secretService;

	public BatchResolveHostServer(SecretService secretService) {
		this.secretService = secretService;
	}

	@Override
	public BatchResolveRequestArg getRequestArg(RecordedRequest recordedRequest) {
		return BatchResolveRequestArg.create(recordedRequest);
	}

	@Override
	public String convert(BatchResolveHostResponse batchResolveResponse) {
		ArrayList<ResolveItem> items = new ArrayList<>();
		for (BatchResolveHostResponse.HostItem item : batchResolveResponse.getItems()) {
			if (item.getIpType() == RequestIpType.v4) {
				items.add(new ResolveItem(item.getHost(), 1, item.getTtl(), item.getIps()));
			}
			if (item.getIpType() == RequestIpType.v6) {
				items.add(new ResolveItem(item.getHost(), 28, item.getTtl(), item.getIps()));
			}
		}
		return constructResolveHostResultBody(items);
	}

	@Override
	public BatchResolveHostResponse convert(String body) {
		try {
			return BatchResolveHostResponse.fromResponse(body);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public BatchResolveHostResponse randomData(BatchResolveRequestArg arg) {
		return randomResolveHostResponse(arg);
	}

	@Override
	public boolean isMyBusinessRequest(RecordedRequest request) {
		return BatchResolveHostServer.BatchResolveRequestArg.create(request) != null
			&& SecretService.checkSign(secretService, request);
	}

	/**
	 * 获取历史请求中，包含host的预解析请求结果
	 *
	 * @param host
	 * @param type
	 * @return
	 */
	public BatchResolveHostResponse getResponseForHost(String host, RequestIpType type) {
		synchronized (records) {
			for (RequestRecord record : records) {
				String hosts = record.getRecordedRequest().getRequestUrl().queryParameter("host");
				String query = record.getRecordedRequest().getRequestUrl().queryParameter("query");
				if (hosts.contains(host) && isRequestType(query, type)) {
					return convert(
						record.getMockResponse().getBody().readString(Charset.forName("UTF-8")));
				} else {
					TestLogger.log(
						"getResponseForHost host " + host + " type " + type + " hosts " + hosts
							+ " query " + query);
				}
			}
		}
		TestLogger.log("getResponseForHost host " + host + " type " + type + " return null!!");
		return null;
	}

	public boolean hasRequestForHost(String host, RequestIpType type, int count,
									 boolean removeRecord) {
		synchronized (records) {
			ArrayList<RequestRecord> targetRecords = new ArrayList<>();
			for (RequestRecord record : records) {
				String hosts = record.getRecordedRequest().getRequestUrl().queryParameter("host");
				String query = record.getRecordedRequest().getRequestUrl().queryParameter("query");
				if (hosts.contains(host) && isRequestType(query, type)) {
					targetRecords.add(record);
				} else {
					TestLogger.log(
						"hasRequestForHost host " + host + " type " + type + " hosts " + hosts
							+ " query " + query);
				}
			}
			if (removeRecord) {
				records.removeAll(targetRecords);
			}

			if (count >= 0) {
				return targetRecords.size() == count;
			} else {
				return targetRecords.size() > 0;
			}
		}
	}

	private boolean isRequestType(String query, RequestIpType type) {
		return (type == RequestIpType.v4 && (query == null || query.equals("4")))
			|| (type == RequestIpType.v6 && (query.equals("6")))
			|| (type == RequestIpType.both && (query.equals("4,6") || query.equals("6,4")));
	}

	/**
	 * 根据 自定义请求参数 构建批量解析结果数据，数据随机
	 *
	 * @param resolveServerArg
	 * @return
	 */
	public static BatchResolveHostResponse randomResolveHostResponse(
		BatchResolveRequestArg resolveServerArg) {
		return randomResolveHostResponse(resolveServerArg.hosts, resolveServerArg.type);
	}

	/**
	 * 根据 域名列表和解析类型 构建批量解析结果数据，数据随机
	 *
	 * @param hostList
	 * @param type
	 * @return
	 */
	public static BatchResolveHostResponse randomResolveHostResponse(List<String> hostList,
																RequestIpType type) {
		ArrayList<BatchResolveHostResponse.HostItem> hostItems = new ArrayList<>();
		for (String host : hostList) {
			switch (type) {
				case v4:
					hostItems.add(new BatchResolveHostResponse.HostItem(host, RequestIpType.v4,
						RandomValue.randomIpv4s(), RandomValue.randomInt(300)));
					break;
				case v6:
					hostItems.add(new BatchResolveHostResponse.HostItem(host, RequestIpType.v6,
						RandomValue.randomIpv6s(), RandomValue.randomInt(300)));
					break;
				default:
					hostItems.add(new BatchResolveHostResponse.HostItem(host, RequestIpType.v4,
						RandomValue.randomIpv4s(), RandomValue.randomInt(300)));
					hostItems.add(new BatchResolveHostResponse.HostItem(host, RequestIpType.v6,
						RandomValue.randomIpv6s(), RandomValue.randomInt(300)));
					break;
			}
		}
		return new BatchResolveHostResponse(hostItems);
	}

	/**
	 * 根据 域名列表和解析类型 构建批量解析结果数据，数据随机, ttl指定
	 *
	 * @param hostList
	 * @param type
	 * @param ttl
	 * @return
	 */
	public static BatchResolveHostResponse randomResolveHostResponse(List<String> hostList,
																RequestIpType type, int ttl) {
		ArrayList<BatchResolveHostResponse.HostItem> hostItems = new ArrayList<>();
		for (String host : hostList) {
			switch (type) {
				case v4:
					hostItems.add(new BatchResolveHostResponse.HostItem(host, RequestIpType.v4,
						RandomValue.randomIpv4s(), ttl));
					break;
				case v6:
					hostItems.add(new BatchResolveHostResponse.HostItem(host, RequestIpType.v6,
						RandomValue.randomIpv6s(), ttl));
					break;
				default:
					hostItems.add(new BatchResolveHostResponse.HostItem(host, RequestIpType.v4,
						RandomValue.randomIpv4s(), ttl));
					hostItems.add(new BatchResolveHostResponse.HostItem(host, RequestIpType.v6,
						RandomValue.randomIpv6s(), ttl));
					break;
			}
		}
		return new BatchResolveHostResponse(hostItems);
	}

	private static String constructResolveHostResultBody(ArrayList<ResolveItem> items) {
		// {"dns":[{"host":"www.taobao.com","ips":["124.239.239.235","124.239.159.105"],"type":1,
		// "ttl":31},{"host":"www.taobao.com","ips":["240e:b1:9801:400:3:0:0:3fa",
		// "240e:b1:a820:0:3:0:0:3f6"],"type":28,"ttl":60}]}
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("{\"dns\":[");
		for (int i = 0; i < items.size(); i++) {
			ResolveItem item = items.get(i);
			if (i != 0) {
				stringBuilder.append(",");
			}
			stringBuilder.append("{");
			stringBuilder.append("\"host\":\"").append(item.host).append("\"");
			if (item.ips != null) {
				stringBuilder.append(",\"ips\":[");
				for (int j = 0; j < item.ips.length; j++) {
					if (j != 0) {
						stringBuilder.append(",");
					}
					stringBuilder.append("\"").append(item.ips[j]).append("\"");
				}
				stringBuilder.append("]");
			}
			stringBuilder.append(",\"type\":").append(item.type);
			stringBuilder.append(",\"ttl\":").append(item.ttl);
			stringBuilder.append("}");
		}
		stringBuilder.append("]}");
		return stringBuilder.toString();
	}

	/**
	 * 预解析请求参数
	 */
	public static class BatchResolveRequestArg {
		public List<String> hosts;
		public RequestIpType type;

		public static BatchResolveRequestArg create(List<String> hosts, RequestIpType type) {
			BatchResolveRequestArg arg = new BatchResolveRequestArg();
			arg.hosts = hosts;
			arg.type = type;
			return arg;
		}

		public static BatchResolveRequestArg create(RecordedRequest recordedRequest) {
			List<String> pathSegments = recordedRequest.getRequestUrl().pathSegments();
			if (pathSegments.size() == 2 && (pathSegments.contains("resolve")
				|| pathSegments.contains("sign_resolve"))) {
				String hosts = recordedRequest.getRequestUrl().queryParameter("host");
				List<String> hostList = Arrays.asList(hosts.split(","));
				RequestIpType type = getQueryType(recordedRequest);
				return create(hostList, type);
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

		@Override
		public boolean equals(Object o) {
			if (this == o) {return true;}
			if (o == null || getClass() != o.getClass()) {return false;}

			BatchResolveRequestArg arg = (BatchResolveRequestArg)o;

			return formResolveHostArgStr(hosts, type).equals(
				formResolveHostArgStr(arg.hosts, arg.type));
		}

		@Override
		public int hashCode() {
			return formResolveHostArgStr(hosts, type).hashCode();
		}

		private static String formResolveHostArgStr(List<String> hostList, RequestIpType type) {
			ArrayList<String> hosts = new ArrayList<>();
			hosts.addAll(hostList);
			Collections.sort(hosts);
			StringBuilder stringBuilder = new StringBuilder();
			for (int i = 0; i < hosts.size(); i++) {
				if (i != 0) {
					stringBuilder.append(",");
				}
				stringBuilder.append(hosts.get(i));
			}
			switch (type) {
				case v6:
					stringBuilder.append("&v6");
					break;
				case both:
					stringBuilder.append("&v4v6");
					break;
			}
			return stringBuilder.toString();
		}
	}

	private static class ResolveItem {
		public String host;
		public int type;
		public int ttl;
		public String[] ips;

		public ResolveItem(String host, int type, int ttl, String[] ips) {
			this.host = host;
			this.type = type;
			this.ttl = ttl;
			this.ips = ips;
		}
	}
}
