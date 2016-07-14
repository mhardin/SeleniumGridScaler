package com.rmn.qa.task;

import java.util.Date;
import java.util.List;

import org.openqa.grid.internal.ProxySet;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.beust.jcommander.internal.Lists;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationDynamicNode;
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
	private List<AutomationDynamicNode> nodesStarted = Lists.newArrayList();
	private List<String> browserStarted = Lists.newArrayList();
	private List<Platform> platformStarted = Lists.newArrayList();
	private List<Integer> numThreadsStarted = Lists.newArrayList();
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
	List<AutomationDynamicNode> startNodes(VmManager vmManager, int browsersToStart, String browser, Platform platform) throws NodesCouldNotBeStartedException {
		List<AutomationDynamicNode> createdNodes = Lists.newArrayList();
		numThreadsStarted.add(browsersToStart);
		browserStarted.add(browser);
		platformStarted.add(platform);
		for (int i=0;i<browsersToStart;i++) {
			String instance = "foo" + i;
			AutomationDynamicNode createdNode = new AutomationDynamicNode(instance, instance, browser, platform, new Date(), 1);
			AutomationContext.getContext().addPendingNode(createdNode);
			createdNodes.add(createdNode);
		}
		nodesStarted.addAll(createdNodes);
		return createdNodes;
	}

	@Override
	boolean haveTestRequestsBeenQueuedForLongEnough(Date nowDate, Date nodePendingDate) {
		return nodeOldEnoughToStart;
	}

	public void setNodeOldEnoughToStart(boolean nodeOldEnoughToStart) {
		this.nodeOldEnoughToStart = nodeOldEnoughToStart;
	}

	public List<AutomationDynamicNode> getNodesStarted() {
		return nodesStarted;
	}

	public List<String> getBrowserStarted() {
		return browserStarted;
	}

	public List<Platform> getPlatformStarted() {
		return platformStarted;
	}

	public List<Integer> getNumThreadsStarted() {
		return numThreadsStarted;
	}

	public void clear() {
		nodesStarted = Lists.newArrayList();
		browserStarted = Lists.newArrayList();
		platformStarted = Lists.newArrayList();
		numThreadsStarted = Lists.newArrayList();
	}
}

