package com.rmn.qa.task;

import java.util.Arrays;

import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.rmn.qa.AutomationContext;
import com.rmn.qa.MockRequestMatcher;
import com.rmn.qa.MockVmManager;

import junit.framework.Assert;

/**
 * Created by matthew on 3/18/16.
 */
public class AutomationScaleNodeTaskTest {

	@After
	public void cleanUp() {
		AutomationContext.refreshContext();
	}

	@Test
	public void testUnsupportedBrowserNoNodesStarted() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "UNSUPPORTED_BROWSER");
		task.setDesiredCapabilities(Arrays.asList(capabilities));
		task.run();
		Assert.assertFalse("No nodes should have been started", task.isNodesStarted());
	}

	@Test
	public void testSpecificPlatformNoNodesStarted() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities.setPlatform(Platform.MAC);
		task.setDesiredCapabilities(Arrays.asList(capabilities));
		task.run();
		Assert.assertFalse("No nodes should have been started", task.isNodesStarted());
	}

	@Test
	public void testNullPlatformNodeStarted() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities));
		task.run();
		Assert.assertTrue("Nodes should have been started", task.isNodesStarted());
	}

	@Test
	public void testAnyPlatformNodeStarted() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities.setCapability(CapabilityType.PLATFORM, "ANY");
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities));
		task.run();
		Assert.assertTrue("Nodes should have been started", task.isNodesStarted());
	}

	@Test
	public void testStarPlatformNodeStarted() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		capabilities.setCapability(CapabilityType.PLATFORM, "*");
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities));
		task.run();
		Assert.assertTrue("Nodes should have been started", task.isNodesStarted());
	}

	@Test
	public void testNotEnoughTimePassNoNewNodeStarted() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		task.setNodeOldEnoughToStart(false);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities));
		task.run();
		Assert.assertFalse("Nodes should have been started", task.isNodesStarted());
	}

	@Test
	public void testPendingBrowserDoesntMatchNoNewNodes() {
		MockVmManager manageEc2 = new MockVmManager();
		MockRequestMatcher matcher = new MockRequestMatcher();
		matcher.setThreadsToReturn(10);
		MockAutomationScaleNodeTask task = new MockAutomationScaleNodeTask(null, manageEc2);
		task.setNodeOldEnoughToStart(false);
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.BROWSER_NAME, "chrome");
		task.setDesiredCapabilities(Arrays.asList(capabilities, capabilities));
		task.run();
		task.setNodeOldEnoughToStart(true);
		DesiredCapabilities nonMatchingBrowser = new DesiredCapabilities();
		nonMatchingBrowser.setBrowserName("firefox");
		task.setDesiredCapabilities(Arrays.asList(nonMatchingBrowser));
		Assert.assertFalse("Nodes should have been started", task.isNodesStarted());
	}


}
