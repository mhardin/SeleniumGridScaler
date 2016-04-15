package com.rmn.qa.task;

import java.util.Date;

import org.openqa.grid.internal.ProxySet;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.rmn.qa.MockVmManager;
import com.rmn.qa.NodesCouldNotBeStartedException;
import com.rmn.qa.RegistryRetriever;
import com.rmn.qa.aws.VmManager;

/**
 * Created by matthew on 3/18/16.
 */
public class MockAutomationScaleNodeTask extends AutomationScaleNodeTask {

	private ProxySet proxySet;
	private Iterable<DesiredCapabilities> desiredCapabilities;
	private boolean nodesStarted = false;
	private boolean nodeOldEnoughToStart = true;

	public MockAutomationScaleNodeTask(RegistryRetriever retriever, MockVmManager vmManager) {
		super(retriever, vmManager);
	}

	@Override
	public ProxySet getProxySet() {
		return proxySet;
	}

	public void setProxySet(ProxySet proxySet) {
		this.proxySet = proxySet;
	}

	@Override
	Iterable<DesiredCapabilities> getDesiredCapabilities() {
		return desiredCapabilities;
	}

	public void setDesiredCapabilities(Iterable<DesiredCapabilities> desiredCapabilities) {
		this.desiredCapabilities = desiredCapabilities;
	}

	@Override
	void startNodes(VmManager vmManager, int browsersToStart, String browser) throws NodesCouldNotBeStartedException {
		nodesStarted = true;
	}

	public boolean isNodesStarted() {
		return nodesStarted;
	}

	@Override
	boolean isNodeOldEnoughToCreateNewNode(Date nowDate, Date nodePendingDate) {
		return nodeOldEnoughToStart;
	}

	public void setNodeOldEnoughToStart(boolean nodeOldEnoughToStart) {
		this.nodeOldEnoughToStart = nodeOldEnoughToStart;
	}
}

