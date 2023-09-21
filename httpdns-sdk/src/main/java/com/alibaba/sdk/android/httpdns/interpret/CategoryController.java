package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.serverip.ScheduleService;

/**
 * 域名解析策略控制
 */
public class CategoryController implements StatusControl {

	private Status mStatus = Status.NORMAL;
	private final NormalCategory mNormal;
	private final SniffCategory mSniff;

	public CategoryController(ScheduleService scheduleService) {
		mNormal = new NormalCategory(scheduleService, this);
		mSniff = new SniffCategory(scheduleService, this);
	}

	public InterpretHostCategory getCategory() {
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
