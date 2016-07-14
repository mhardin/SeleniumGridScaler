package com.rmn.qa.task;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.Test;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationDynamicNode;
import com.rmn.qa.BaseTest;
import com.rmn.qa.MockRequestMatcher;
import com.rmn.qa.MockVmManager;

import junit.framework.Assert;

/**
 * Created by matthew on 3/18/16.
 */
public class AutomationScaleNodeTaskTest extends BaseTest {


	@Test
	// Handle
	public void testNoQueuedRequests() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		task.setDesiredCapabilities(Collections.emptyList());
		task.run();
		Assert.assertEquals("No nodes should have been started", 0, task.getNodesStarted().size());
	}

	@Test
	// Unsupported browser on a queued request should not result in any scaled capacity
	public void testUnsupportedBrowserNoNodesStarted() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "safari");
		task.setDesiredCapabilities(Arrays.asList(capabilities));
		task.run();
		Assert.assertEquals("No nodes should have been started", 0, task.getNodesStarted().size());
	}

	@Test
	// If an unsupported platform is queued, no node should be started
	public void testUnsupportedPlatformNoNodesStarted() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities.setPlatform(Platform.MAC);
		task.setDesiredCapabilities(Arrays.asList(capabilities));
		task.run();
		Assert.assertEquals("No nodes should have been started", 0, task.getNodesStarted().size());
	}

	@Test
	// Not sure if this will ever happen in the wild, but if no platform is specified, make sure we're defaulting to ANY
	public void testNullPlatformNodeStarted() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		task.setDesiredCapabilities(Arrays.asList(capabilities));
		task.run();
		Assert.assertEquals("Nodes should have been started", 1, task.getNodesStarted().size());
		Assert.assertNotNull("Nodes should have been started", task.getNodesStarted().get(0));
		Assert.assertEquals("Started browser should be correct", "chrome", task.getBrowserStarted().get(0));
		Assert.assertEquals("Started platform should be correct", Platform.ANY, task.getPlatformStarted().get(0));
		Assert.assertEquals("Number of nodes started should be correct", 1, task.getNumThreadsStarted().get(0).intValue());
	}

	@Test
	// If a queued request specifies ANY, we should handle starting up a new node
	public void testAnyPlatformNodeStarted() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities.setCapability(CapabilityType.PLATFORM, Platform.ANY);
		task.setDesiredCapabilities(Arrays.asList(capabilities));
		task.run();
		Assert.assertEquals("Nodes should have been started", 1, task.getNodesStarted().size());
		Assert.assertNotNull("Nodes should have been started", task.getNodesStarted().get(0));
		Assert.assertEquals("Started browser should be correct", "chrome", task.getBrowserStarted().get(0));
		Assert.assertEquals("Started platform should be correct", Platform.ANY, task.getPlatformStarted().get(0));
		Assert.assertEquals("Number of nodes started should be correct", 1, task.getNumThreadsStarted().get(0).intValue());
	}

	@Test
	// If multiple requests have different platforms from the same family, only one type of platform should start
	public void testMultiplePlatformsStartOneTypeOfNode() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities.setCapability(CapabilityType.PLATFORM, Platform.LINUX);
		DesiredCapabilities capabilities2 = new DesiredCapabilities();
		capabilities2.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities2.setCapability(CapabilityType.PLATFORM, Platform.UNIX);
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities2));
		task.run();
		Assert.assertEquals("Number of threads should be correct", 1, task.getNumThreadsStarted().size());
		Assert.assertEquals("Nodes should have been started", 2, task.getNumThreadsStarted().get(0).intValue());
		Assert.assertEquals("Started browser should be correct", "chrome", task.getBrowserStarted().get(0));
		Assert.assertEquals("Started platform should be correct", Platform.UNIX, task.getPlatformStarted().get(0));
	}
	@Test
	// If multiple requests have different platforms from different families, multiple types of nodes should start
	public void testDifferentPlatformsStartDifferentNodes() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities.setCapability(CapabilityType.PLATFORM, Platform.LINUX);
		DesiredCapabilities capabilities2 = new DesiredCapabilities();
		capabilities2.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities2.setCapability(CapabilityType.PLATFORM, Platform.WINDOWS);
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities2));
		task.run();
		Assert.assertEquals("Nodes should have been started", 2, task.getNodesStarted().size());
		Assert.assertNotNull("Nodes should have been started", task.getNodesStarted().get(0));
		Assert.assertNotNull("Nodes should have been started", task.getNodesStarted().get(1));
		Assert.assertEquals("Started browser should be correct", 2, task.getBrowserStarted().stream().filter(browser -> "chrome".equals(browser)).count());
		Assert.assertEquals("Started platform should be correct", 1, task.getPlatformStarted().stream().filter(platform -> platform == Platform.WINDOWS).count());
		Assert.assertEquals("Started platform should be correct", 1, task.getPlatformStarted().stream().filter(platform -> platform == Platform.UNIX).count());
		Assert.assertEquals("Number of nodes started should be correct", 1, task.getNumThreadsStarted().get(0).intValue());
		Assert.assertEquals("Number of nodes started should be correct", 1, task.getNumThreadsStarted().get(1).intValue());
	}

	@Test
	// Happy path, make sure queued requests for linux result in started instances
	public void testLinuxPlatformNodeStarted() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities.setCapability(CapabilityType.PLATFORM, Platform.LINUX);
		task.setDesiredCapabilities(Arrays.asList(capabilities));
		task.run();
		Assert.assertEquals("Nodes should have been started", 1, task.getNodesStarted().size());
		Assert.assertNotNull("Nodes should have been started", task.getNodesStarted().get(0));
		Assert.assertEquals("Started browser should be correct", "chrome", task.getBrowserStarted().get(0));
		Assert.assertEquals("Started platform should be correct", Platform.UNIX, task.getPlatformStarted().get(0));
		Assert.assertEquals("Number of nodes started should be correct", 1, task.getNumThreadsStarted().get(0).intValue());
	}

	@Test
	// Test that if there are nodes pending startup that haven't come online, their load is correctly factored into future
	// scale activity
	public void testPendingLoadSubtractsFromQueuedRequests() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities.setCapability(CapabilityType.PLATFORM, Platform.LINUX);
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities));
		task.run();
		Assert.assertEquals("Nodes should have been started", 2, task.getNodesStarted().size());
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities, capabilities, capabilities));
		// Clear out previous counts from the mock task so they don't interfere with the new run
		task.clear();
		task.run();
		Assert.assertEquals("Only 2 nodes should start as pending count should be subtracted from count", 2, task.getNodesStarted().size());
	}

	@Test
	// Test that if there are nodes pending startup that haven't come online, their load is correctly factored into future
	// scale activity
	public void testUnrelatedPendingLoadDoesntSubtractsFromQueuedRequests() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities.setCapability(CapabilityType.PLATFORM, Platform.LINUX);
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities));
		// Add 2 nodes to our pending context list and make sure this doesn't affect our scale node logic.  Only pending nodes that
		// originate from AutomationScaleNodeTask should be included in the count
		AutomationContext.getContext().addPendingNode(new AutomationDynamicNode(null, "foo", null, null, null, new Date(), 1));
		AutomationContext.getContext().addPendingNode(new AutomationDynamicNode(null, "bar", null, null, null, new Date(), 1));
		task.run();
		Assert.assertEquals("Nodes should have been started", 2, task.getNodesStarted().size());
	}

	@Test
	// Make sure after a node has registers with the hub, that it is no longer subtracted from scale activity
	public void testPendingLoadDoesntSubtractsFromQueuedRequestsNodesStarted() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities.setCapability(CapabilityType.PLATFORM, Platform.LINUX);
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities));
		task.run();
		Assert.assertEquals("Nodes should have been started", 2, task.getNodesStarted().size());
		// Clear all nodes from pending to emulate the nodes starting up
		task.getNodesStarted().stream().forEach(node -> AutomationContext.getContext().removePendingNode(node.getInstanceId()));
		// Clear out previous counts from the mock task so they don't interfere with the new run
		task.clear();
		task.run();
		Assert.assertEquals("Only 2 nodes should start as pending count should be subtracted from count", 2, task.getNodesStarted().size());
	}

	@Test
	// Make sure that nodes pending startup don't count towards other non-matching queued requests
	public void testPendingLoadDoesntSubtractsFromQueuedRequests() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities.setCapability(CapabilityType.PLATFORM, Platform.LINUX);
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities));
		task.run();
		Assert.assertEquals("Nodes should have been started", 2, task.getNodesStarted().size());
		// Change the platform so we have different requests coming in
		capabilities.setPlatform(Platform.WINDOWS);
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities, capabilities, capabilities));
		// Clear out previous counts from the mock task so they don't interfere with the new run
		task.clear();
		task.run();
		Assert.assertEquals("Only 2 nodes should start as pending count should be subtracted from count", 4, task.getNodesStarted().size());
	}

	@Test
	// Make sure a higher specified platform dumbs down to its correct family (e.g. LINUX -> UNIX)
	public void testFamilyPlatformNodeStarted() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities.setCapability(CapabilityType.PLATFORM, Platform.LINUX);
		task.setDesiredCapabilities(Arrays.asList(capabilities));
		task.run();
		Assert.assertEquals("Nodes should have been started", 1, task.getNodesStarted().size());
		Assert.assertNotNull("Nodes should have been started", task.getNodesStarted().get(0));
		Assert.assertEquals("Started browser should be correct", "chrome", task.getBrowserStarted().get(0));
		Assert.assertEquals("Started platform should be correct", Platform.UNIX, task.getPlatformStarted().get(0));
		Assert.assertEquals("Number of nodes started should be correct", 1, task.getNumThreadsStarted().get(0).intValue());
	}

	@Test
	// Tests that multiple queued requests result in multiple nodes started in one task pass
	public void testMultipleNodesStartedAtOnce() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities.setCapability(CapabilityType.PLATFORM, Platform.ANY);
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities));
		task.run();
		Assert.assertEquals("Number of threads should be correct", 1, task.getNumThreadsStarted().size());
		Assert.assertEquals("Nodes should have been started", 2, task.getNumThreadsStarted().get(0).intValue());
		Assert.assertEquals("Started browser should be correct", "chrome", task.getBrowserStarted().get(0));
		Assert.assertEquals("Started platform should be correct", Platform.ANY, task.getPlatformStarted().get(0));
	}

	@Test
	// Unsupported platform should not result in any scaled nodes
	public void testUnsupportedPlatformNodeNotStarted() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities.setCapability(CapabilityType.PLATFORM, Platform.MAC);
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities));
		task.run();
		Assert.assertEquals("No nodes should have been started", 0, task.getNodesStarted().size());
	}

	@Test
	// If a certain browser/platform hasn't been queued long enough, no nodes should be scaled up
	public void testNotEnoughTimePassedNoNewNodeStarted() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		task.setNodeOldEnoughToStart(false);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities));
		task.run();
		Assert.assertEquals("No nodes should have been started", 0, task.getNodesStarted().size());
	}

	@Test
	// Make sure if a previously tracked browser was seen by the task, that nothing starts up for that browser if there
	// aren't any matching queued test requests
	public void testPreviouslyTrackedBrowserDoesntStart() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		task.setNodeOldEnoughToStart(false);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities));
		task.run();
		Assert.assertEquals("No nodes should have been started", 0, task.getNodesStarted().size());
		task.setNodeOldEnoughToStart(true);
		DesiredCapabilities nonMatchingBrowser = new DesiredCapabilities();
		nonMatchingBrowser.setBrowserName("firefox");
		task.setDesiredCapabilities(Arrays.asList(nonMatchingBrowser, nonMatchingBrowser));
		task.run();
		// Firefox, not chrome, should start up
		Assert.assertEquals("Number of threads should be correct", 1, task.getNumThreadsStarted().size());
		Assert.assertEquals("Nodes should have been started", 2, task.getNumThreadsStarted().get(0).intValue());
		Assert.assertEquals("Started browser should be correct", "firefox", task.getBrowserStarted().get(0));
		Assert.assertEquals("Started platform should be correct", Platform.ANY, task.getPlatformStarted().get(0));
	}


}
