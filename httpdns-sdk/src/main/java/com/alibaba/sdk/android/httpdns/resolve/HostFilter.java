package com.alibaba.sdk.android.httpdns.resolve;

import com.alibaba.sdk.android.httpdns.DegradationFilter;
import com.alibaba.sdk.android.httpdns.NotUseHttpDnsFilter;

public class HostFilter {
	DegradationFilter mFilter;
	NotUseHttpDnsFilter mNotUseHttpDnsFilter;

	public boolean isFiltered(String host) {
		return (mNotUseHttpDnsFilter != null && mNotUseHttpDnsFilter.notUseHttpDns(host)) || (mFilter != null && mFilter.shouldDegradeHttpDNS(host));
	}

	@Deprecated
	public void setFilter(DegradationFilter filter) {
		this.mFilter = filter;
	}

	public void setFilter(NotUseHttpDnsFilter filter) {
		mNotUseHttpDnsFilter = filter;
	}
}
