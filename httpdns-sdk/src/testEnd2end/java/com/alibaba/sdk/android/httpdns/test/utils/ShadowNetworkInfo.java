package com.alibaba.sdk.android.httpdns.test.utils;

import android.net.NetworkInfo;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

/**
 * robolectric 的NetworkInfo shadow 类
 * @author zonglin.nzl
 * @date 2020/9/8
 */
@Implements(NetworkInfo.class)
public class ShadowNetworkInfo extends org.robolectric.shadows.ShadowNetworkInfo {
    private boolean isAvailable;
    private boolean isConnected;
    private int connectionType;
    private int connectionSubType;
    private NetworkInfo.DetailedState detailedState;

    @Implementation
    public static void __staticInitializer__() {
    }

    public static NetworkInfo newInstance(NetworkInfo.DetailedState detailedState, int type, int subType, boolean isAvailable, boolean isConnected) {
        NetworkInfo networkInfo = Shadow.newInstanceOf(NetworkInfo.class);
        final ShadowNetworkInfo info = Shadow.extract(networkInfo);
        info.setConnectionType(type);
        info.setSubType(subType);
        info.setDetailedState(detailedState);
        info.setAvailableStatus(isAvailable);
        info.setConnectionStatus(isConnected);
        return networkInfo;
    }

    @Implementation
    public boolean isConnected() {
        return isConnected;
    }

    @Implementation
    public boolean isConnectedOrConnecting() {
        return isConnected;
    }

    @Implementation
    public NetworkInfo.State getState() {
        return isConnected ? NetworkInfo.State.CONNECTED :
                NetworkInfo.State.DISCONNECTED;
    }

    @Implementation
    public NetworkInfo.DetailedState getDetailedState() {
        return detailedState;
    }

    @Implementation
    public int getType() {
        return connectionType;
    }

    @Implementation
    public int getSubtype() {
        return connectionSubType;
    }

    @Implementation
    public boolean isAvailable() {
        return isAvailable;
    }

    @Implementation
    public String getTypeName() {
        switch (connectionType) {
            case 0:
                return "MOBILE";
            case 1:
                return "WIFI";
            default:
                return null;
        }
    }

    /**
     * Sets up the return value of {@link #isAvailable()}.
     *
     * @param isAvailable the value that {@link #isAvailable()} will return.
     */
    public void setAvailableStatus(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    /**
     * Sets up the return value of {@link #isConnectedOrConnecting()} and {@link #isConnected()}.
     *
     * @param isConnected the value that {@link #isConnectedOrConnecting()} and {@link #isConnected()} will return.
     */
    public void setConnectionStatus(boolean isConnected) {
        this.isConnected = isConnected;
    }

    /**
     * Sets up the return value of {@link #getType()}.
     *
     * @param connectionType the value that {@link #getType()} will return.
     */
    public void setConnectionType(int connectionType) {
        this.connectionType = connectionType;
    }

    public void setSubType(int subType) {
        this.connectionSubType = subType;
    }

    public void setDetailedState(NetworkInfo.DetailedState detailedState) {
        this.detailedState = detailedState;
    }
}
