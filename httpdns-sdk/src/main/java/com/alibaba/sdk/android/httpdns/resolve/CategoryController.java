package com.alibaba.sdk.android.httpdns.resolve;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.serverip.RegionServerScheduleService;

/**
 * 域名解析策略控制
 */
public class CategoryController implements StatusControl {

	private Status mStatus = Status.NORMAL;
	private final NormalResolveCategory mNormal;
	private final SniffResolveCategory mSniff;

	public CategoryController(HttpDnsConfig config, RegionServerScheduleService scheduleService) {
		mNormal = new NormalResolveCategory(config, scheduleService, this);
		mSniff = new SniffResolveCategory(config, scheduleService, this);
	}

	public ResolveHostCategory getCategory() {
		if (mStatus == Status.DISABLE) {
			return mSniff;
		}
		return mNormal;
	}

	@Override
	public void turnDown() {
		switch (mStatus) {
			case NORMAL:
				mStatus = Status.PRE_DISABLE;
				break;
			case PRE_DISABLE:
				mStatus = Status.DISABLE;
				break;
			default:
				break;
		}
	}

	@Override
	public void turnUp() {
		mStatus = Status.NORMAL;
	}

	/**
	 * 重置策略
	 */
	public void reset() {
		mStatus = Status.NORMAL;
		mSniff.reset();
	}

	/**
	 * 设置嗅探模式请求间隔
	 *
	 */
	public void setSniffTimeInterval(int timeInterval) {
		mSniff.setInterval(timeInterval);
	}

	/**
	 * 策略状态，只有disable状态会使用嗅探模式
	 */
	enum Status {
		NORMAL,
		PRE_DISABLE,
		DISABLE
	}
}
