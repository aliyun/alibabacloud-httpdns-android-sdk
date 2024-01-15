package com.alibaba.sdk.android.httpdns.resolve;

import com.alibaba.sdk.android.httpdns.DegradationFilter;

public class HostFilter {
	DegradationFilter mFilter;

	public boolean isFiltered(String host) {
		return mFilter != null && mFilter.shouldDegradeHttpDNS(host);
	}

	public void setFilter(DegradationFilter filter) {
		this.mFilter = filter;
	}
}
