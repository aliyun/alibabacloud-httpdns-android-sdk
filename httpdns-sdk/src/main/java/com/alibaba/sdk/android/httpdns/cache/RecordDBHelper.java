package com.alibaba.sdk.android.httpdns.cache;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 数据库存取操作
 */
public class RecordDBHelper extends SQLiteOpenHelper {

	private static final String DB_NAME = "aliclound_httpdns_v3_";
	private static final int DB_VERSION = 0x03;

	static class HOST {

		static final String TABLE_NAME = "host";

		static final String COL_ID = "id";

		static final String COL_REGION = "region";

		static final String COL_HOST = "host";

		static final String COL_IPS = "ips";

		static final String COL_TYPE = "type";

		static final String COL_TIME = "time";

		static final String COL_TTL = "ttl";

		static final String COL_EXTRA = "extra";

		static final String COL_CACHE_KEY = "cache_key";

		// 旧版本 用于存储网络标识的字段，由于合规的影响，删除了相关代码，此字段变为固定字段，目前已经没有意义
		static final String COL_SP = "sp";

		static final String COL_NO_IP_CODE = "no_ip_code";

		static final String CREATE_HOST_TABLE_SQL = "CREATE TABLE " + TABLE_NAME + " ("
			+ COL_ID + " INTEGER PRIMARY KEY,"
			+ COL_REGION + " TEXT,"
			+ COL_HOST + " TEXT,"
			+ COL_IPS + " TEXT,"
			+ COL_TYPE + " INTEGER,"
			+ COL_TIME + " INTEGER,"
			+ COL_TTL + " INTEGER,"
			+ COL_EXTRA + " TEXT,"
			+ COL_CACHE_KEY + " TEXT,"
			+ COL_SP + " TEXT,"
			+ COL_NO_IP_CODE + " TEXT"
			+ ");";
	}

	private final String mAccountId;
	private final Object mLock = new Object();
	private SQLiteDatabase mDb;

	public RecordDBHelper(Context context, String accountId) {
		super(context, DB_NAME + accountId + ".db", null, DB_VERSION);
		this.mAccountId = accountId;
	}

	private SQLiteDatabase getDB() {
		if (mDb == null) {
			try {
				mDb = getWritableDatabase();
			} catch (Exception e) {
			}
		}
		return mDb;
	}

	@Override
	protected void finalize() throws Throwable {
		if (mDb != null) {
			try {
				mDb.close();
			} catch (Exception ignored) {
			}
		}
		super.finalize();
	}

	/**
	 * 从数据库获取全部数据
	 *
	 * @param region
	 * @return
	 */
	public List<HostRecord> readFromDb(String region) {
		synchronized (mLock) {
			ArrayList<HostRecord> hosts = new ArrayList<>();

			SQLiteDatabase db = null;
			Cursor cursor = null;
			try {
				db = getDB();
				cursor = db.query(HOST.TABLE_NAME, null, HOST.COL_REGION + " = ?",
					new String[] {region}, null, null, null);
				if (cursor != null && cursor.getCount() > 0) {
					cursor.moveToFirst();
					do {
						HostRecord hostRecord = new HostRecord();
						hostRecord.setId(cursor.getLong(cursor.getColumnIndex(HOST.COL_ID)));
						hostRecord.setRegion(
							cursor.getString(cursor.getColumnIndex(HOST.COL_REGION)));
						hostRecord.setHost(cursor.getString(cursor.getColumnIndex(HOST.COL_HOST)));
						hostRecord.setIps(CommonUtil.parseStringArray(
							cursor.getString(cursor.getColumnIndex(HOST.COL_IPS))));
						hostRecord.setType(cursor.getInt(cursor.getColumnIndex(HOST.COL_TYPE)));
						hostRecord.setTtl(cursor.getInt(cursor.getColumnIndex(HOST.COL_TTL)));
						hostRecord.setQueryTime(
							cursor.getLong(cursor.getColumnIndex(HOST.COL_TIME)));
						hostRecord.setExtra(
							cursor.getString(cursor.getColumnIndex(HOST.COL_EXTRA)));
						hostRecord.setCacheKey(
							cursor.getString(cursor.getColumnIndex(HOST.COL_CACHE_KEY)));
						hostRecord.setFromDB(true);
						hostRecord.setNoIpCode(cursor.getString(cursor.getColumnIndex(HOST.COL_NO_IP_CODE)));
						hosts.add(hostRecord);
					} while (cursor.moveToNext());
				}
			} catch (Exception e) {
				HttpDnsLog.w("read from db fail " + mAccountId, e);
			} finally {
				try {
					if (cursor != null) {
						cursor.close();
					}
				} catch (Exception ignored) {
				}
			}
			return hosts;
		}
	}

	/**
	 * 从数据库删除数据
	 */
	public void delete(List<HostRecord> records) {
		if (records == null || records.isEmpty()) {
			return;
		}

		synchronized (mLock) {
			SQLiteDatabase db = null;
			try {
				db = getDB();
				db.beginTransaction();
				for (HostRecord record : records) {
					db.delete(HOST.TABLE_NAME, HOST.COL_ID + " = ? ",
						new String[] {String.valueOf(record.getId())});
				}
				db.setTransactionSuccessful();
			} catch (Exception e) {
				HttpDnsLog.w("delete record fail " + mAccountId, e);
			} finally {
				if (db != null) {
					try {
						db.endTransaction();
					} catch (Exception ignored) {
					}
				}
			}
		}
	}

	/**
	 * 更新数据
	 */
	public void insertOrUpdate(List<HostRecord> records) {
		synchronized (mLock) {
			SQLiteDatabase db = null;
			try {
				db = getDB();
				db.beginTransaction();
				for (HostRecord record : records) {
					ContentValues cv = new ContentValues();
					cv.put(HOST.COL_REGION, record.getRegion());
					cv.put(HOST.COL_HOST, record.getHost());
					cv.put(HOST.COL_IPS, CommonUtil.translateStringArray(record.getIps()));
					cv.put(HOST.COL_CACHE_KEY, record.getCacheKey());
					cv.put(HOST.COL_EXTRA, record.getExtra());
					cv.put(HOST.COL_TIME, record.getQueryTime());
					cv.put(HOST.COL_TYPE, record.getType());
					cv.put(HOST.COL_TTL, record.getTtl());
					cv.put(HOST.COL_NO_IP_CODE, record.getNoIpCode());

					if (record.getId() != -1) {
						db.update(HOST.TABLE_NAME, cv, HOST.COL_ID + " = ?",
							new String[] {String.valueOf(record.getId())});
					} else {
						long id = db.insert(HOST.TABLE_NAME, null, cv);
						record.setId(id);
					}
				}
				db.setTransactionSuccessful();
			} catch (Exception e) {
				HttpDnsLog.w("insertOrUpdate record fail " + mAccountId, e);
			} finally {
				if (db != null) {
					try {
						db.endTransaction();
					} catch (Exception ignored) {
					}
				}
			}
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		try {
			db.execSQL(HOST.CREATE_HOST_TABLE_SQL);
		} catch (Exception e) {
			HttpDnsLog.w("create db fail " + mAccountId, e);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion != newVersion) {
			try {
				db.beginTransaction();
				db.execSQL("DROP TABLE IF EXISTS " + HOST.TABLE_NAME + ";");
				db.setTransactionSuccessful();
				db.endTransaction();
				onCreate(db);
			} catch (Exception e) {
				HttpDnsLog.w("upgrade db fail " + mAccountId, e);
			}
		}
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion != newVersion) {
			try {
				db.beginTransaction();
				db.execSQL("DROP TABLE IF EXISTS " + HOST.TABLE_NAME + ";");
				db.setTransactionSuccessful();
				db.endTransaction();
				onCreate(db);
			} catch (Exception e) {
				HttpDnsLog.w("downgrade db fail " + mAccountId, e);
			}
		}
	}
}
