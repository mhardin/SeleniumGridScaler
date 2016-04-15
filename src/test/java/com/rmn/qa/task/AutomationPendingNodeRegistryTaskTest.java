package com.rmn.qa.task;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openqa.grid.internal.ProxySet;

import com.rmn.qa.AutomationCapabilityMatcher;
import com.rmn.qa.AutomationConstants;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.MockRemoteProxy;

import junit.framework.Assert;

/**
 * Created by matthew on 3/18/16.
 */
public class AutomationPendingNodeRegistryTaskTest {

	@Test
	public void testPendingNodeRemoved() {
		MockAutomationPendingNodeRegistryTask task = new MockAutomationPendingNodeRegistryTask(null);
		ProxySet proxySet = new ProxySet(false);
		MockRemoteProxy proxy = new MockRemoteProxy();
		proxySet.add(proxy);
		Map<String,Object> config = new HashMap<>();
		String instanceId = "instanceId";
		AutomationContext.getContext().addPendingNode(instanceId);
		config.put(AutomationConstants.INSTANCE_ID, instanceId);
		proxy.setConfig(config);
		proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
		task.setProxySet(proxySet);

		Assert.assertTrue("Node should be pending before task runs", AutomationContext.getContext().pendingNodeExists(instanceId));
		task.doWork();
		Assert.assertFalse("Instance should not show as pending after task has run", AutomationContext.getContext().pendingNodeExists(instanceId));
	}

	@Test
	public void testPendingNodeNotRemovedMismatchInstanceId() {
		MockAutomationPendingNodeRegistryTask task = new MockAutomationPendingNodeRegistryTask(null);
		ProxySet proxySet = new ProxySet(false);
		MockRemoteProxy proxy = new MockRemoteProxy();
		proxySet.add(proxy);
		Map<String,Object> config = new HashMap<>();
		String instanceId = "instanceId";
		String instanceIdDifferent = "differentInstanceId";
		AutomationContext.getContext().addPendingNode(instanceIdDifferent);
		config.put(AutomationConstants.INSTANCE_ID, instanceId);
		proxy.setConfig(config);
		proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
		task.setProxySet(proxySet);

		Assert.assertTrue("Node should be pending before task runs", AutomationContext.getContext().pendingNodeExists(instanceIdDifferent));
		task.doWork();
		Assert.assertTrue("Instance should still be pending as it hasn't come online", AutomationContext.getContext().pendingNodeExists(instanceIdDifferent));
		Assert.assertFalse("Instance should not be pending as it never was pending to begin with", AutomationContext.getContext().pendingNodeExists(instanceId));

	}

	@Test
	public void testPendingNodeNotRemovedNoConfigId() {
		MockAutomationPendingNodeRegistryTask task = new MockAutomationPendingNodeRegistryTask(null);
		ProxySet proxySet = new ProxySet(false);
		MockRemoteProxy proxy = new MockRemoteProxy();
		proxySet.add(proxy);
		Map<String,Object> config = new HashMap<>();
		String instanceId = "instanceId";
		AutomationContext.getContext().addPendingNode(instanceId);
		proxy.setConfig(config);
		proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
		task.setProxySet(proxySet);

		Assert.assertTrue("Node should be pending before task runs", AutomationContext.getContext().pendingNodeExists(instanceId));
		task.doWork();
		Assert.assertTrue("Node should still be pending after task runs as there was no instance id in the config", AutomationContext.getContext().pendingNodeExists(instanceId));
	}

	@Test
	public void testPendingNodeNotRemovedNullProxySet() {
		MockAutomationPendingNodeRegistryTask task = new MockAutomationPendingNodeRegistryTask(null);
		ProxySet proxySet = new ProxySet(false);
		String instanceId = "instanceId";
		AutomationContext.getContext().addPendingNode(instanceId);
		task.setProxySet(proxySet);

		Assert.assertTrue("Node should be pending before task runs", AutomationContext.getContext().pendingNodeExists(instanceId));
		task.doWork();
		Assert.assertTrue("Node should still be pending after task runs as there was no instance id in the config", AutomationContext.getContext().pendingNodeExists(instanceId));
	}

	@Test
	public void testPendingNodeNotRemovedEmptyProxySet() {
		MockAutomationPendingNodeRegistryTask task = new MockAutomationPendingNodeRegistryTask(null);
		ProxySet proxySet = new ProxySet(false);
		String instanceId = "instanceId";
		AutomationContext.getContext().addPendingNode(instanceId);
		task.setProxySet(proxySet);

		Assert.assertTrue("Node should be pending before task runs", AutomationContext.getContext().pendingNodeExists(instanceId));
		task.doWork();
		Assert.assertTrue("Node should still be pending after task runs as there was no instance id in the config", AutomationContext.getContext().pendingNodeExists(instanceId));
	}

}
