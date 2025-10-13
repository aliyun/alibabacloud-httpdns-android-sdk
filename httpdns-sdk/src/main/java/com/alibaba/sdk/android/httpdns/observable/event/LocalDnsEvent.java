package com.alibaba.sdk.android.httpdns.observable.event;

import com.alibaba.sdk.android.httpdns.observable.ObservableConstants;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public interface LocalDnsEvent {

    default void localDns(String host) {
        try {
            long localDnsStart = System.currentTimeMillis();
            InetAddress[] addresses = InetAddress.getAllByName(host);
            long localDnsCost = System.currentTimeMillis() - localDnsStart;
            if (addresses != null && addresses.length > 0) {
                List<String> ips = new ArrayList<>();
                List<String> ipv6s = new ArrayList<>();
                for (InetAddress address : addresses) {
                    if (address instanceof Inet4Address) {
                        ips.add(address.getHostAddress());
                    } else if (address instanceof Inet6Address) {
                        ipv6s.add(address.getHostAddress());
                    }
                }

                StringBuilder sb = new StringBuilder();
                int ipType = getRequestIpType();
                if ((ipType & ObservableConstants.REQUEST_IP_TYPE_V4) == ObservableConstants.REQUEST_IP_TYPE_V4) {
                    for (int i = 0; i != ips.size(); ++i) {
                        sb.append(ips.get(i));
                        if (i != ips.size() - 1) {
                            sb.append(",");
                        }
                    }
                }

                if ((ipType & ObservableConstants.REQUEST_IP_TYPE_V6) ==ObservableConstants.REQUEST_IP_TYPE_V6) {
                    if (!ipv6s.isEmpty() && sb.length() > 0) {
                        sb.append(";");
                    }
                    for (int i = 0; i != ipv6s.size(); ++i) {
                        sb.append(ipv6s.get(i));
                        if (i != ipv6s.size() - 1) {
                            sb.append(",");
                        }
                    }
                }

                if (this instanceof ObservableEvent) {
                    ((ObservableEvent) this).setLocalDnsIps(sb.toString());
                    ((ObservableEvent) this).setLocalDnsCost(localDnsCost);
                }
            }
        } catch (Exception e) {

        }
    }

    /**
     * 返回值和tag中的请求类型保持一致
     * @return
     */
    abstract int getRequestIpType();
}
