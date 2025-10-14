package com.alibaba.sdk.android.httpdns.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

public final class NetworkUtil {
    private static final int UNKNOWN = 5;
    private static final int OTHERS = 6;
    private static final int WIFI = 1;
    private static final int G2 = OTHERS;
    private static final int G3 = 4;
    private static final int G4 = 3;
    private static final int G5 = 2;

    public static int getNetworkType(Context context) {
        if (context == null) {
            return UNKNOWN;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return UNKNOWN;
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            //是否已经连接到网络(连接上但不代表可以访问网络)
            if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return UNKNOWN;
            }

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return WIFI;
            }
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo == null) {
                return UNKNOWN;
            }

            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return WIFI;
            }
        }

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return getMobileNetworkType(context, telephonyManager, connectivityManager);
    }

    @SuppressLint("MissingPermission")
    private static int getMobileNetworkType(Context context, TelephonyManager telephonyManager,
                                            ConnectivityManager connectivityManager) {
        // Mobile network
        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        if (telephonyManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkHasPermission(context, Manifest.permission.READ_BASIC_PHONE_STATE))
                    || checkHasPermission(context, Manifest.permission.READ_PHONE_STATE)
                    || telephonyManager.hasCarrierPrivileges())) {
                networkType = telephonyManager.getDataNetworkType();
            } else {
                if (checkHasPermission(context, Manifest.permission.READ_PHONE_STATE)) {
                    try {
                        networkType = telephonyManager.getNetworkType();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        if (networkType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // 在 Android 11 平台上，没有 READ_PHONE_STATE 权限时
                return UNKNOWN;
            }

            if (connectivityManager != null) {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null) {
                    networkType = networkInfo.getSubtype();
                }
            }
        }

        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return G2;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return G3;
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_IWLAN:
            case 19:  //目前已知有车机客户使用该标记作为 4G 网络类型 TelephonyManager.NETWORK_TYPE_LTE_CA:
                return G4;
            case TelephonyManager.NETWORK_TYPE_NR:
                return G5;
        }
        return OTHERS;
    }

    private static boolean checkHasPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }

        //6.0之前的版本，不做处理
        return false;
    }
}
