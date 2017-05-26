package com.rmn.qa.task;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openqa.grid.internal.ProxySet;

import com.rmn.qa.AutomationCapabilityMatcher;
import com.rmn.qa.AutomationConstants;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationDynamicNode;
import com.rmn.qa.AutomationRunContext;
import com.rmn.qa.AutomationUtils;
import com.rmn.qa.BaseTest;
import com.rmn.qa.MockRemoteProxy;
import com.rmn.qa.MockVmManager;

import junit.framework.Assert;

/**
 * Created by matthew on 3/18/16.
 */
public class AutomationPendingNodeRegistryTaskTest extends BaseTest {

	@Test
	public void testPendingNodeRemoved() {
		MockAutomationPendingNodeRegistryTask task = new MockAutomationPendingNodeRegistryTask(null);
		ProxySet proxySet = new ProxySet(false);
		MockRemoteProxy proxy = new MockRemoteProxy();
		proxySet.add(proxy);
		Map<String,String> config = new HashMap<>();
		String instanceId = "instanceId";
		AutomationDynamicNode node = new AutomationDynamicNode(null, instanceId, null, null, null, new Date(), 1);
		AutomationContext.getContext().addPendingNode(node);
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
		Map<String,String> config = new HashMap<>();
		String instanceId = "instanceId";
		String instanceIdDifferent = "differentInstanceId";
		AutomationDynamicNode node = new AutomationDynamicNode(null, instanceIdDifferent, null, null, null, new Date(), 1);
		AutomationContext.getContext().addPendingNode(node);
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
		Map<String,String> config = new HashMap<>();
		String instanceId = "instanceId";
		AutomationDynamicNode node = new AutomationDynamicNode(null, instanceId, null, null, null, new Date(), 1);
		AutomationContext.getContext().addPendingNode(node);
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
		AutomationDynamicNode node = new AutomationDynamicNode(null, instanceId, null, null, null, new Date(), 1);
		AutomationContext.getContext().addPendingNode(node);
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
		AutomationDynamicNode node = new AutomationDynamicNode(null, instanceId, null, null, null, new Date(), 1);
		AutomationContext.getContext().addPendingNode(node);
		task.setProxySet(proxySet);

		Assert.assertTrue("Node should be pending before task runs", AutomationContext.getContext().pendingNodeExists(instanceId));
		task.doWork();
		Assert.assertTrue("Node should still be pending after task runs as there was no instance id in the config", AutomationContext.getContext().pendingNodeExists(instanceId));
	}

	@Test
	// Make sure the pending node gets removed after its been pending for too long
	public void testPendingNodeRemovedAfterTimeout() {
		MockVmManager vmManager = new MockVmManager();
		MockAutomationPendingNodeRegistryTask task = new MockAutomationPendingNodeRegistryTask(null, vmManager);
		String instanceId = "instanceId";
		AutomationDynamicNode node = new AutomationDynamicNode(null, instanceId, null, null, null, AutomationUtils.modifyDate(new Date(), - (AutomationRunContext.PENDING_NODE_EXPIRATION_TIME_IN_MINUTES + 1), Calendar.MINUTE), 1);
		AutomationContext.getContext().addPendingNode(node);
		Assert.assertTrue("Node should be pending before task runs", AutomationContext.getContext().pendingNodeExists(instanceId));
		task.doWork();
		Assert.assertTrue("Instance should be terminated", vmManager.isTerminated());
		Assert.assertFalse("Instance should not show as pending after task has run", AutomationContext.getContext().pendingNodeExists(instanceId));
		Assert.assertEquals("Instance should have a terminated status", AutomationDynamicNode.STATUS.TERMINATED, node.getStatus());
	}

	@Test
	// Make sure the pending node gets removed after its been pending for too long
	public void testPendingNodeNotRemovedBeforeTimeout() {
		MockVmManager vmManager = new MockVmManager();
		MockAutomationPendingNodeRegistryTask task = new MockAutomationPendingNodeRegistryTask(null, vmManager);
		String instanceId = "instanceId";
		AutomationDynamicNode node = new AutomationDynamicNode(null, instanceId, null, null, null, AutomationUtils.modifyDate(new Date(), - (AutomationRunContext.PENDING_NODE_EXPIRATION_TIME_IN_MINUTES - 1), Calendar.MINUTE), 1);
		AutomationContext.getContext().addPendingNode(node);
		Assert.assertTrue("Node should be pending before task runs", AutomationContext.getContext().pendingNodeExists(instanceId));
		task.doWork();
		Assert.assertFalse("Instance should not be terminated as timeout wasn't hit", vmManager.isTerminated());
		Assert.assertTrue("Node should still be in the pending set as it wasn't removed", AutomationContext.getContext().pendingNodeExists(instanceId));
		Assert.assertEquals("Instance should have a terminated status", AutomationDynamicNode.STATUS.RUNNING, node.getStatus());
	}

}
