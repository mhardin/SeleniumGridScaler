/*
 * Copyright (C) 2014 RetailMeNot, Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package com.rmn.qa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.junit.Test;
import org.openqa.grid.common.SeleniumProtocol;
import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.TestSlot;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.CapabilityType;

import com.google.common.collect.Maps;

import junit.framework.Assert;

public class AutomationRequestMatcherTest extends BaseTest {

    @Test
    // Tests that a node in the Expired state is not considered as a free resource
    public void testRequestNodeExpiredState() throws IOException, ServletException{
        String browser = "firefox";
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),10);
        node.updateStatus(AutomationDynamicNode.STATUS.EXPIRED);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID, nodeId);
        proxy.setConfig(config);
        List<TestSlot> testSlots = new ArrayList<>();
        Map<String,Object> capabilities = Maps.newHashMap();
        capabilities.put(CapabilityType.BROWSER_NAME,browser);
        testSlots.add(new TestSlot(proxy, SeleniumProtocol.WebDriver, null, capabilities));
        proxy.setTestSlots(testSlots);
        proxySet.add(proxy);
        AutomationContext.getContext().setTotalNodeCount(50);
        AutomationRequestMatcher requestMatcher = new AutomationRequestMatcher();
        int freeThreads = requestMatcher.getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(browser));
        Assert.assertEquals("Thread count should be correct due to an expired node", 0, freeThreads);
    }

    @Test
    // Tests that a node in the Terminated state without an instance id is still considered a valid resource
    public void testRequestNodeTerminatedNoInstanceId() throws IOException, ServletException{
        String browser = "firefox";
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),10);
        node.updateStatus(AutomationDynamicNode.STATUS.TERMINATED);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        proxy.setMaxNumberOfConcurrentTestSessions(5);
        Map<String,Object> config = Maps.newHashMap();
        proxy.setConfig(config);
        List<TestSlot> testSlots = new ArrayList<>();
        Map<String,Object> capabilities = Maps.newHashMap();
        capabilities.put(CapabilityType.BROWSER_NAME,browser);
        testSlots.add(new TestSlot(proxy, SeleniumProtocol.WebDriver, null, capabilities));
        proxy.setTestSlots(testSlots);
        proxySet.add(proxy);
        AutomationContext.getContext().setTotalNodeCount(50);
        AutomationRequestMatcher requestMatcher = new AutomationRequestMatcher();
        int freeThreads = requestMatcher.getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(browser));
        Assert.assertEquals("Node should be available since instance id was not on the node", 1, freeThreads);
    }

    @Test
    // Tests that a node in the Terminated state is not considered as a free resource
    public void testRequestNodeTerminatedState() throws IOException, ServletException{
        String browser = "firefox";
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),10);
        node.updateStatus(AutomationDynamicNode.STATUS.TERMINATED);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(5);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID, nodeId);
        proxy.setConfig(config);
        List<TestSlot> testSlots = new ArrayList<>();
        Map<String,Object> capabilities = Maps.newHashMap();
        capabilities.put(CapabilityType.BROWSER_NAME,browser);
        testSlots.add(new TestSlot(proxy, SeleniumProtocol.WebDriver, null, capabilities));
        proxy.setTestSlots(testSlots);
        proxySet.add(proxy);
        AutomationContext.getContext().setTotalNodeCount(50);
        AutomationRequestMatcher requestMatcher = new AutomationRequestMatcher();
        int freeThreads = requestMatcher.getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(browser));
        Assert.assertEquals("No threads should be available since the node was set to terminated", 0, freeThreads);
    }

    @Test
    // Tests that OS matching on a node works correctly
    public void testRequestMatchingOs() throws IOException, ServletException {
        String browser = "firefox";
        Platform os = Platform.LINUX;
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = Maps.newHashMap();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        capabilities.put(CapabilityType.PLATFORM,os);
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        AutomationContext.getContext().setTotalNodeCount(50);
        int freeThreads = new AutomationRequestMatcher().getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(null,null,browser,null,os));

        Assert.assertEquals("Thread count should be correct due to matching OS", 10, freeThreads);
    }

    @Test
    // Tests that OS NOT matching on a node works correctly
    public void testRequestNonMatchingOs() throws IOException, ServletException {
        String browser = "firefox";
        Platform os = Platform.LINUX;
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = Maps.newHashMap();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        capabilities.put(CapabilityType.PLATFORM,Platform.WINDOWS);
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        AutomationContext.getContext().setTotalNodeCount(50);
        int freeThreads = new AutomationRequestMatcher().getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(null,null,browser,null,os));

        Assert.assertEquals("Thread count should be 0 due to non-matching OS", 0, freeThreads);
    }

    @Test
    // Happy path that browsers matching shows correct free node count
    public void testRequestMatchingBrowsers() throws IOException, ServletException{
        String browser = "firefox";
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = Maps.newHashMap();
        capabilities.put(CapabilityType.BROWSER_NAME,browser);
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        AutomationContext.getContext().setTotalNodeCount(50);
        int freeThreads = new AutomationRequestMatcher().getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(browser));

        Assert.assertEquals("Thread count should be correct due to matching browser", 10, freeThreads);
    }

    @Test
    // Test that non-matching browsers do not contribute to the free node count
    public void testRequestNonMatchingBrowsers() throws IOException, ServletException{
        String browser = "firefox";
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = Maps.newHashMap();
        capabilities.put(CapabilityType.BROWSER_NAME,"doesntMatch");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        AutomationContext.getContext().setTotalNodeCount(50);
        int freeThreads = new AutomationRequestMatcher().getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(browser));

        Assert.assertEquals("Thread count should be correct due to matching OS", 0, freeThreads);
    }

    @Test
    // Makes sure that all matching slots will be included to match
    public void testRequestAllTestSlotsIncluded() throws IOException, ServletException{
        String browser = "firefox";
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(10);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = Maps.newHashMap();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        AutomationContext.getContext().setTotalNodeCount(50);
        int freeThreads = new AutomationRequestMatcher().getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(browser));
        Assert.assertEquals("There should be no matching threads since the node limit was reached",10,freeThreads);
    }

    @Test
    // Tests that the correct number of slots match even when the max session config on node is less than
    // the slot number
    public void testRequestAllTestSlotsIncludedGreaterNodeLimit() throws IOException, ServletException{
        String browser = "firefox";
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(15);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = Maps.newHashMap();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        AutomationContext.getContext().setTotalNodeCount(50);
        int freeThreads = new AutomationRequestMatcher().getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(browser));
        Assert.assertEquals("There should be no matching threads since the node limit was reached",10,freeThreads);
    }

    @Test
    // Tests that the correct number of slots match even when the max session config on node is greater than
    // the slot number
    public void testRequestAllTestSlotsIncludedLessThanNodeLimit() throws IOException, ServletException{
        String browser = "firefox";
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(5);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = Maps.newHashMap();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        AutomationContext.getContext().setTotalNodeCount(50);
        int freeThreads = new AutomationRequestMatcher().getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(browser));
        Assert.assertEquals("There should be no matching threads since the node limit was reached",5,freeThreads);
    }

    @Test
    // Test to make sure an in progress run only counts against the free nodes if the in progress run's browser
    // matches the requested browser
    public void testRequestNewRunNotStartedDifferentBrowser() throws IOException, ServletException{
        String nonMatchingBrowser = "firefox";
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        String runId = "runId";
        AutomationContext.getContext().addRun(new AutomationRunRequest(runId,10,"firefox"));
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy nonMatchingProxy = new MockRemoteProxy();
        nonMatchingProxy.setMaxNumberOfConcurrentTestSessions(50);
        nonMatchingProxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        nonMatchingProxy.setConfig(config);
        Map<String,Object> nonMatchingCapabilities = Maps.newHashMap();
        nonMatchingCapabilities.put(CapabilityType.BROWSER_NAME, nonMatchingBrowser);
        TestSlot nonMatchingTestSlot = new TestSlot(nonMatchingProxy, SeleniumProtocol.WebDriver,null,nonMatchingCapabilities);
        nonMatchingProxy.setMultipleTestSlots(nonMatchingTestSlot, 10);
        proxySet.add(nonMatchingProxy);
        String matchingBrowser = "chrome";

        MockRemoteProxy matchingProxy = new MockRemoteProxy();
        proxySet.add(matchingProxy);
        matchingProxy.setMaxNumberOfConcurrentTestSessions(50);
        matchingProxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        matchingProxy.setConfig(config);
        Map<String,Object> matchingCapabilities = Maps.newHashMap();
        matchingCapabilities.put(CapabilityType.BROWSER_NAME, matchingBrowser);
        TestSlot matchingTestSlot = new TestSlot(nonMatchingProxy, SeleniumProtocol.WebDriver,null,matchingCapabilities);
        matchingProxy.setMultipleTestSlots(matchingTestSlot, 10);

        AutomationContext.getContext().setTotalNodeCount(50);
        int freeThreads = new AutomationRequestMatcher().getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(matchingBrowser));
        Assert.assertEquals("Nodes should be free even though run is in progress as browsers do not match",10,freeThreads);
    }

    @Test
    // Test to make sure an in progress counts against the free node count
    public void testRequestNewRunNotStarted() throws IOException, ServletException{
        String browser = "firefox";
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        String runId = "runId";
        AutomationContext.getContext().addRun(new AutomationRunRequest(runId,10,"firefox"));
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID, nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = Maps.newHashMap();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        AutomationContext.getContext().setTotalNodeCount(50);
        int freeThreads = new AutomationRequestMatcher().getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(browser));
        Assert.assertEquals("No nodes should be free since existing run hasn't started",0,freeThreads);
    }

    @Test
    // Tests that a new run is still calculated with the total run threads and not what is in progress
    public void testRequestNewRunInProgress() throws IOException, ServletException{
        String browser = "firefox";
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        String runId = "runId";
        AutomationContext.getContext().addRun(new AutomationRunRequest(runId,10,"firefox"));
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(15);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = Maps.newHashMap();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        // Associate the run UUID with the test slots to mimic a test run that is partially underway
        capabilities.put(AutomationConstants.UUID,runId);
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        // Assign a session to the test slot
        testSlot.getNewSession(capabilities);
        proxy.setMultipleTestSlots(testSlot,5);
        Map<String,Object> capabilities2 = Maps.newHashMap();
        capabilities2.put(CapabilityType.BROWSER_NAME,"firefox");
        TestSlot testSlot2 = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities2);
        proxy.setMultipleTestSlots(testSlot2, 10);
        proxySet.add(proxy);
        AutomationContext.getContext().setTotalNodeCount(50);
        int freeThreads = new AutomationRequestMatcher().getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(browser));
        Assert.assertEquals("Free nodes should be including total run count",5,freeThreads);
    }

    @Test
    // Test to make sure that an old run that never started is not subtracted from the free node count
    public void testRequestOldRunInProgress() throws IOException, ServletException{
        String browser = "firefox";
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        String runId = "runId";
        AutomationContext.getContext().addRun(new AutomationRunRequest(runId,10,"firefox",null,null,AutomationUtils.modifyDate(new Date(),-2, Calendar.MINUTE)));
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = Maps.newHashMap();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        // Associate the run UUID with the test slots to mimic a test run that is still in progress
        capabilities.put(AutomationConstants.UUID,runId);
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        // Assign a session to the test slot
        testSlot.getNewSession(capabilities);
        proxy.setMultipleTestSlots(testSlot, 5);
        Map<String,Object> capabilities2 = Maps.newHashMap();
        capabilities2.put(CapabilityType.BROWSER_NAME,"firefox");
        TestSlot testSlot2 = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities2);
        proxy.setMultipleTestSlots(testSlot2, 5);
        proxySet.add(proxy);
        AutomationContext.getContext().setTotalNodeCount(50);
        int freeThreads = new AutomationRequestMatcher().getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(browser));
        Assert.assertEquals("Nodes should be considered free since run is considered old at this point ",5,freeThreads);
    }

    @Test
    // Tests when a run is considered old that the threads are not considered in use
    public void testRequestOldRunFinished() throws IOException, ServletException{
        String browser = "firefox";
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        String runId = "runId";
        // Add an old run that will not be included in the available resource logic
        AutomationContext.getContext().addRun(new AutomationRunRequest(runId, 10, "firefox", null,null,AutomationUtils.modifyDate(new Date(), -2, Calendar.MINUTE)));
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = Maps.newHashMap();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        AutomationContext.getContext().setTotalNodeCount(50);
        int freeThreads = new AutomationRequestMatcher().getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(browser));
        Assert.assertEquals("Free nodes should be correct since the run has finished at this point",10,freeThreads);
    }

    @Test
    // 5 firefox, 5 chrome slots, (10 node total) 5 in progress chrome tests, firefox should still show 5 free
    public void testMultipleBrowsersInUse() throws IOException, ServletException{
        String browser = "firefox";
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(10);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> nonMatchingCapabilities = Maps.newHashMap();
        nonMatchingCapabilities.put(CapabilityType.BROWSER_NAME, "chrome");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,nonMatchingCapabilities);
        testSlot.getNewSession(nonMatchingCapabilities);
        proxy.setMultipleTestSlots(testSlot, 5);
        proxySet.add(proxy);

        Map<String,Object> matchingCapabilities = Maps.newHashMap();
        matchingCapabilities.put(CapabilityType.BROWSER_NAME, browser);
        TestSlot testSlot2 = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,matchingCapabilities);
        proxy.setMultipleTestSlots(testSlot2, 5);
        proxySet.add(proxy);
        AutomationContext.getContext().setTotalNodeCount(50);
        int freeThreads = new AutomationRequestMatcher().getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(browser));
        Assert.assertEquals("No nodes should be free since existing run is still new",5,freeThreads);
    }

    @Test
    // Node 1: 1 firefox, 12 chrome slots, (12 node total) 1 in progress chrome tests
    // Node 2: 1 firefox, 6 chrome slots, (6 node total), 6 in progress chrome tests
    // Firefox browser request should still show 1 free
    public void testNew2() throws IOException, ServletException{
        String browser = "firefox";
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(6);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = Maps.newHashMap();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> nonMatchingCapabilities = Maps.newHashMap();
        nonMatchingCapabilities.put(CapabilityType.BROWSER_NAME, "chrome");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,nonMatchingCapabilities);
        testSlot.getNewSession(nonMatchingCapabilities);
        proxy.setMultipleTestSlots(testSlot, 6);
        proxySet.add(proxy);

        Map<String,Object> matchingCapabilities = Maps.newHashMap();
        matchingCapabilities.put(CapabilityType.BROWSER_NAME, browser);
        TestSlot testSlot2 = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,matchingCapabilities);
        proxy.setMultipleTestSlots(testSlot2, 1);
        proxySet.add(proxy);

        MockRemoteProxy proxy2 = new MockRemoteProxy();
        proxy2.setMaxNumberOfConcurrentTestSessions(12);
        proxy2.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config2 = Maps.newHashMap();
        config2.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy2.setConfig(config2);
        Map<String,Object> nonMatchingCapabilities2 = Maps.newHashMap();
        nonMatchingCapabilities2.put(CapabilityType.BROWSER_NAME, "chrome");
        TestSlot testSlot3 = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,nonMatchingCapabilities2);
        testSlot2.getNewSession(nonMatchingCapabilities2);
        proxy2.setMultipleTestSlots(testSlot3, 11);

        TestSlot testSlot4 = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,matchingCapabilities);
        proxy2.setMultipleTestSlots(testSlot4, 1);

        proxySet.add(proxy);

        proxySet.add(proxy2);

        AutomationContext.getContext().setTotalNodeCount(50);
        int freeThreads = new AutomationRequestMatcher().getNumFreeThreadsForParameters(proxySet,new AutomationRunRequest(browser));
        Assert.assertEquals("Free nodes should be correct since the node max has not been reached",1,freeThreads);
    }
}
