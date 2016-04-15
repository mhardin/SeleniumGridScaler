package com.rmn.qa.task;

import org.openqa.grid.internal.ProxySet;

import com.rmn.qa.RegistryRetriever;

/**
 * Created by matthew on 3/18/16.
 */
public class MockAutomationPendingNodeRegistryTask extends AutomationPendingNodeRegistryTask {

	private ProxySet proxySet;

	public MockAutomationPendingNodeRegistryTask(RegistryRetriever registryRetriever) {
		super(registryRetriever);
	}

	@Override
	protected ProxySet getProxySet() {
		return proxySet;
	}

	public void setProxySet(ProxySet proxySet) {
		this.proxySet = proxySet;
	}
}
