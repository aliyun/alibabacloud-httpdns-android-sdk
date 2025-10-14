package com.alibaba.sdk.android.httpdns.observable;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.observable.event.GroupEvent;
import com.alibaba.sdk.android.httpdns.observable.event.LocalDnsEvent;
import com.alibaba.sdk.android.httpdns.observable.event.ObservableEvent;
import com.alibaba.sdk.android.httpdns.observable.event.ReportingRateExceptionEvent;
import com.alibaba.sdk.android.httpdns.request.HttpRequest;
import com.alibaba.sdk.android.httpdns.request.HttpRequestTask;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.request.ResponseParser;
import com.alibaba.sdk.android.httpdns.request.RetryHttpRequest;
import com.alibaba.sdk.android.httpdns.resolve.ResolveHostHelper;
import com.alibaba.sdk.android.httpdns.track.SessionTrackMgr;
import com.alibaba.sdk.android.httpdns.utils.ThreadUtil;

/**
 * utils data upload manager
 */

public class ObservableManager {
	private static final int MESSAGE_REPORT = 101;
	private HttpDnsConfig mHttpDnsConfig;
	private String mSecret = null;
	private int mReportBatchIndex = 0;
	private List<ObservableEvent> mCacheEvents = new ArrayList<>();

	private boolean mPositiveEnableObservable = true;
	private boolean mReportingRateException = false;
	private AtomicBoolean initMessage = new AtomicBoolean(false);
	private ArrayDeque<Long> mReportsTime = new ArrayDeque<>();
	private AtomicBoolean mReporting = new AtomicBoolean(false);

	private final Handler mHandler;
	private static final ExecutorService sWorker = ThreadUtil.createObservableExecutorService();
	private static final ExecutorService mReportWorker = ThreadUtil.createObservableReportExecutorService();
	private static final char[] hexChar = {'0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

	public ObservableManager(HttpDnsConfig httpDnsConfig, String secret) {
		mHttpDnsConfig = httpDnsConfig;
		mSecret = secret;

		if (mHttpDnsConfig.getObservableSampleBenchMarks() < 0) {
			mHttpDnsConfig.setObservableSampleBenchMarks(generateSampleBenchMarks());
		}
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("observable: " + mHttpDnsConfig.getObservableSampleBenchMarks());
		}

		HandlerThread handlerThread = new HandlerThread("httpdns_observable");
		handlerThread.start();
		mHandler = new Handler(handlerThread.getLooper()) {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == MESSAGE_REPORT) {
					report();

					if (isEnableObservable() || !mCacheEvents.isEmpty()) {
						mHandler.sendEmptyMessageDelayed(MESSAGE_REPORT, mHttpDnsConfig.getObservableConfig().batchReportIntervalTime * DateUtils.SECOND_IN_MILLIS);
					}
				}
			}
		};
	}

	public void positiveEnableObservable(boolean enable) {
		mPositiveEnableObservable = enable;
	}

	private boolean isEnableReport() {
		return mPositiveEnableObservable && !TextUtils.isEmpty(mHttpDnsConfig.getObservableConfig().endpoint);
	}

	/**
	 * 是否开启可观测，如果关闭，则打点和上报都关闭
	 * @return
	 */
	private boolean isEnableObservable() {
		return mPositiveEnableObservable && mHttpDnsConfig.getObservableConfig().enable
				&& mHttpDnsConfig.getObservableSampleBenchMarks() < mHttpDnsConfig.getObservableConfig().sampleRatio
				&& !mReportingRateException;
	}

	public void addObservableEvent(final ObservableEvent event) {
		if (!isEnableObservable()) {
			return;
		}

		if (event == null) {
			return;
		}

		if (event instanceof LocalDnsEvent) {
			//需要local dns的event，在这里统一执行local dns解析
			try {
				sWorker.execute(new Runnable() {
					@Override
					public void run() {
						((LocalDnsEvent) event).localDns(event.getHostName());
						addObservableEventInner(event);
					}
				});
			} catch (Exception e) {
				//线程池异常，也要添加事件
				addObservableEventInner(event);
			}
		} else {
			addObservableEventInner(event);
		}
	}

	private void addObservableEventInner(final ObservableEvent event) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (event instanceof GroupEvent) {
					ObservableEvent temp;
					GroupEvent targetGroupEvent = null;
					for (int i= 0; i != mCacheEvents.size(); ++i) {
						temp = mCacheEvents.get(i);
						if (temp instanceof GroupEvent) {
							if (((GroupEvent) temp).isSameGroup(event)) {
								targetGroupEvent = (GroupEvent) temp;
								break;
							}
						}
					}

					if (targetGroupEvent != null) {
						targetGroupEvent.groupWith(event);
						if (HttpDnsLog.isPrint()) {
							HttpDnsLog.d("group event: " + targetGroupEvent.toString());
						}
					} else {
						if (HttpDnsLog.isPrint()) {
							HttpDnsLog.d("add group event: " + event.toString());
						}
						addEventInner(event);
						tryReport();
					}
				} else {
					if (HttpDnsLog.isPrint()) {
						HttpDnsLog.d("add event: " + event.toString());
					}
					addEventInner(event);
					tryReport();
				}
			}
		});

		if (initMessage.compareAndSet(false, true)) {
			mHandler.sendEmptyMessageDelayed(MESSAGE_REPORT, mHttpDnsConfig.getObservableConfig().batchReportIntervalTime * DateUtils.SECOND_IN_MILLIS);
		}
	}

	private void addEventInner(ObservableEvent event) {
		mCacheEvents.add(event);

		if (mCacheEvents.size() >= 400) {
			ReportingRateExceptionEvent reportingRateExceptionEvent = new ReportingRateExceptionEvent();
			reportingRateExceptionEvent.setStatusCode(200);

			Map<Integer, Integer> eventNumMap = new HashMap<>();
			for (ObservableEvent event1 : mCacheEvents) {
				Integer num = eventNumMap.get(event1.getEventName());
				if (num == null) {
					num = 0;
				}

				eventNumMap.put(event1.getEventName(), num + 1);
			}

			int eventName = -1;
			int tmpNum = 0;
			for (Map.Entry<Integer, Integer> entry : eventNumMap.entrySet()) {
				if (entry.getValue() > tmpNum) {
					tmpNum = entry.getValue();
					eventName = entry.getKey();
				}
			}
			reportingRateExceptionEvent.setTag(eventName);

			addObservableEvent(reportingRateExceptionEvent);

			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.w("observable report rate exception.");
			}
			//关闭日志采集
			mReportingRateException = true;
		}
	}

	private void tryReport() {
		if (mCacheEvents.size() >= mHttpDnsConfig.getObservableConfig().batchReportMaxSize) {
			//达到上报数量
			mHandler.removeMessages(MESSAGE_REPORT);
			mHandler.sendEmptyMessage(MESSAGE_REPORT);
		}
	}

	private void report() {
		if (TextUtils.isEmpty(mHttpDnsConfig.getObservableConfig().endpoint)) {
			return;
		}

		if (mCacheEvents.isEmpty()) {
			return;
		}

		if (!isEnableReport()) {
			mCacheEvents.clear();
			++mReportBatchIndex;
			return;
		}

		//上报流量控制判断
		long current = System.currentTimeMillis();
		if (reachingReportingLimit(current)) {
			return;
		}

		if (mReporting.compareAndSet(false, true)) {
			String body = buildReportBody();
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("observable: " + body);
			}

			int num = Math.min(mHttpDnsConfig.getObservableConfig().batchReportMaxSize, mCacheEvents.size());
			for (int i = 0; i != num; ++i) {
				mCacheEvents.remove(0);
			}

			Map<String, String> headers = getHeader(mReportBatchIndex++);

			try {
				String url = getUrl(body);
				if (TextUtils.isEmpty(url)) {
					return;
				}

				//添加上报时间点，用于上报流量控制
				mReportsTime.addLast(current);

				HttpRequest<String> request = new ObservableHttpRequest<>(url, new ResponseParser<String>() {
					@Override
					public String parse(String serverIp, String response) throws Throwable {
						return response;
					}
				}, headers, body);
				// 重试2次
				request = new RetryHttpRequest<>(request, 2);

				mReportWorker.execute(new HttpRequestTask<>(request, new RequestCallback<String>() {
					@Override
					public void onSuccess(String response) {
						mReporting.set(false);
						mHandler.removeMessages(MESSAGE_REPORT);
						mHandler.sendEmptyMessage(MESSAGE_REPORT);
					}

					@Override
					public void onFail(Throwable throwable) {
						mReporting.set(false);
					}
				}));
			} catch (Throwable e) {
				mReporting.set(false);
			}
		}
	}

	private String buildReportBody() {
		StringBuilder sb = new StringBuilder();
		int num = Math.min(mHttpDnsConfig.getObservableConfig().batchReportMaxSize, mCacheEvents.size());
		for (int i = 0; i != num; ++i) {
			sb.append(mCacheEvents.get(i).toString());
			sb.append("\n");
		}

		return sb.toString();
	}

	private String getUrl(String content) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		ObservableConfig config = mHttpDnsConfig.getObservableConfig();
		String url = config.endpoint;
		if (TextUtils.isEmpty(url)) {
			return null;
		}

		if (!TextUtils.isEmpty(url) && (!url.startsWith("http://") && !url.startsWith("https://"))) {
			url = "https://" + url;
		}
		if (!url.endsWith("/")) {
			url += "/";
		}

		url += mHttpDnsConfig.getAccountId() + "/metrics";

		long time = System.currentTimeMillis();
		url += "?t=" + time;

		url += "&ssk=" + (TextUtils.isEmpty(mSecret) ? 0 : 1);
		//tags
		url += ResolveHostHelper.getTags(mHttpDnsConfig);

		String sign = md5(md5(content) + "-" + (TextUtils.isEmpty(mSecret) ? mHttpDnsConfig.getAccountId() : mSecret) + "-" + time);
		url += "&s=" + sign;
		return url;
	}

	private Map<String, String> getHeader(int reportBatchIndex) {
		Map<String, String> headers = new HashMap<>();
		headers.put("User-Agent", mHttpDnsConfig.getUA());
		headers.put("X-HTTPDNS-REQUEST-ID", SessionTrackMgr.getInstance().getSessionId() + "-" + reportBatchIndex);

		return headers;
	}

	private String md5(String content) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		byte[] hash;
		hash = MessageDigest.getInstance("MD5").digest(content.getBytes("UTF-8"));

		StringBuilder sb = new StringBuilder(hash.length * 2);
		for (byte aByte : hash) {
			sb.append(hexChar[(aByte & 0xf0) >>> 4]);
			sb.append(hexChar[aByte & 0x0f]);
		}
		return sb.toString();
	}

	private float generateSampleBenchMarks() {
		Random random = new Random(System.currentTimeMillis());
		return random.nextFloat();
	}

	private boolean reachingReportingLimit(long current) {
		int limit = mHttpDnsConfig.getObservableConfig().maxReportsPerMinute;

		if (mReportsTime.size() < limit) {
			return false;
		}

		//先移除超过每分钟上报次数的上报时间点
		while (mReportsTime.size() > limit) {
			mReportsTime.pollFirst();
		}

		Long first = mReportsTime.peekFirst();
		if (first == null) {
			return false;
		}

		return current - first < DateUtils.MINUTE_IN_MILLIS;
	}
}
