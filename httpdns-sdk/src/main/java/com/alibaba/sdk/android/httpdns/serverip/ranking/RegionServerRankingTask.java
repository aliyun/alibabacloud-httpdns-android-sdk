package com.alibaba.sdk.android.httpdns.serverip.ranking;

import android.util.Pair;

import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RegionServerRankingTask implements Runnable {

    private final String mSchema;
    private final String[] mIps;
    private final int[] mPorts;
    private final RegionServerRankingCallback mIPRankingCallback;

    public RegionServerRankingTask(String schema, String[] ips, int[] ports, RegionServerRankingCallback IPRankingCallback) {
        mSchema = schema;
        mIps = ips;
        mPorts = ports;
        mIPRankingCallback = IPRankingCallback;
    }
    @Override
    public void run() {
        int[] speeds = new int[mIps.length];
        for (int i = 0; i < mIps.length; i++) {
            speeds[i] = testConnectSpeed(mIps[i], (mPorts != null && i < mPorts.length) ? mPorts[i] : -1);
        }
        List<Pair<String, Integer>> result = sortIpsWithSpeeds(mIps, mPorts, speeds);

        String[] ips = new String[result.size()];
        int[] ports = new int[result.size()];
        for (int i = 0; i != result.size(); ++i) {
            ips[i] = result.get(i).first;
            ports[i] = result.get(i).second;
        }
        if (mIPRankingCallback != null) {
            mIPRankingCallback.onResult(ips, mPorts == null ? null : ports);
        }
    }

    private int testConnectSpeed(String ip, int port) {
        long start = System.currentTimeMillis();
        long end = Long.MAX_VALUE;
        try (Socket socket = new Socket()) {
            SocketAddress remoteAddress = new InetSocketAddress(ip, CommonUtil.getPort(port, mSchema));

            socket.connect(remoteAddress, 5 * 1000);
            end = System.currentTimeMillis();
        } catch (IOException e) {
        }
        if (end == Long.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) (end - start);
    }

    private List<Pair<String, Integer>> sortIpsWithSpeeds(String[] ips, int[] ports, int[] speeds) {
        List<Pair<Pair<String, Integer>, Integer>> ipSpeedPairList = new ArrayList<>();
        for (int i = 0; i < ips.length; i++) {
            ipSpeedPairList.add(new Pair<>(new Pair<>(ips[i], (ports != null && i < ports.length) ? ports[i] : -1), speeds[i]));
        }
        Collections.sort(ipSpeedPairList, new Comparator<Pair<Pair<String, Integer>, Integer>>() {
            @Override
            public int compare(Pair<Pair<String, Integer>, Integer> o1, Pair<Pair<String, Integer>, Integer> o2) {
                return o1.second - o2.second;
            }
        });

        List<Pair<String, Integer>> result = new ArrayList<>();
        for (int i = 0; i < ipSpeedPairList.size(); i++) {
            result.add(ipSpeedPairList.get(i).first);
        }
        return result;
    }

}
