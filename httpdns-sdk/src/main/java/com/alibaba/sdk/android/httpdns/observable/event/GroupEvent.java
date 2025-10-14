package com.alibaba.sdk.android.httpdns.observable.event;

/**
 * 需要聚合的事件
 */
public interface GroupEvent {
    boolean isSameGroup(ObservableEvent event);

    void groupWith(ObservableEvent event);
}
