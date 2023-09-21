package com.alibaba.sdk.android.httpdns.track;

import java.util.Random;

import android.util.Log;

public class SessionTrackMgr {

	private static final String TAG = "SessionTrackMgr";

	private String mSid;

	private SessionTrackMgr() {
		try {
			final String sampleAlphabet
				= "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
			final Random random = new Random();
			char[] buf = new char[12];
			for (int i = 0; i < 12; i++) {
				buf[i] = sampleAlphabet.charAt(random.nextInt(sampleAlphabet.length()));
			}
			mSid = new String(buf);
		} catch (Exception e) {
			Log.d(TAG, e.getMessage(), e);
		}
	}

	public static SessionTrackMgr getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private final static class InstanceHolder {
		private final static SessionTrackMgr INSTANCE = new SessionTrackMgr();
	}

	public String getSessionId() {
		return mSid;
	}
}
