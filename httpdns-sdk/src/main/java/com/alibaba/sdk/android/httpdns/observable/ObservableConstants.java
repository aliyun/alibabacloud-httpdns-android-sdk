package com.alibaba.sdk.android.httpdns.observable;

public final class ObservableConstants {
    public static final int REQUEST_IP_TYPE_AUTO = 0x00;
    public static final int REQUEST_IP_TYPE_V4 = 0x01;
    public static final int REQUEST_IP_TYPE_V6 = 0x02;
    public static final int REQUEST_IP_TYPE_BOTH = 0x03;

    public static final int RESOLVE_API_UNKNOWN = 0x00;
    public static final int RESOLVE_API_SYNC = 0x04;
    public static final int RESOLVE_API_ASYNC = 0x08;
    public static final int RESOLVE_API_SYN_NON_BLOCKING = 0x0C;

    public static final int REMOTE_CACHE_NULL = 0x00;
    public static final int REMOTE_CACHE_EXPIRED = 0x10;

    public static final int CACHE_NONE = 0x00;
    public static final int CACHE_NOT_EXPIRED = 0x10;
    public static final int CACHE_EXPIRED_NOT_USE = 0x20;
    public static final int CACHE_EXPIRED_USE = 0x30;

    public static final int REQUEST_NOT_RETRY = 0x00;
    public static final int REQUEST_RETRY = 0x10;

    public static final String UNKNOWN = "unknown";

    public static final int HTTP_REQUEST = 0x00;
    public static final int HTTPS_REQUEST = 0x04;

    public static final int NOT_SIGN_MODE_REQUEST = 0x00;
    public static final int SIGN_MODE_REQUEST = 0x08;

    public static final int CLEAN_ALL_HOST_CACHE = 1;
    public static final int CLEAN_SPECIFY_HOST_CACHE = 2;

    public static final int EMPTY_RESULT = 0x00;
    public static final int NOT_EMPTY_RESULT = 0x40;
}
