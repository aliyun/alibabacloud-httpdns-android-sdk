package com.aliyun.ams.httpdns.demo.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SpUtil {

	public static void readSp(Context context, String name, OnGetSp onGetSp) {
		SharedPreferences sp = context.getSharedPreferences(name, Context.MODE_PRIVATE);
		onGetSp.onGetSp(sp);
	}

	public static void writeSp(Context context, String name, OnGetSpEditor onGetSpEditor) {
		SharedPreferences sp = context.getSharedPreferences(name, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		onGetSpEditor.onGetSpEditor(editor);
		editor.commit();
	}

	public interface OnGetSp {
		void onGetSp(SharedPreferences sp);
	}

	public interface OnGetSpEditor {
		void onGetSpEditor(SharedPreferences.Editor editor);
	}
}
