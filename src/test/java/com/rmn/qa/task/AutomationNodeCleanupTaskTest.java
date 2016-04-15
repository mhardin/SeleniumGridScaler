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

package com.rmn.qa.task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.openqa.grid.common.SeleniumProtocol;
import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.TestSlot;
import org.openqa.selenium.remote.CapabilityType;

import com.rmn.qa.AutomationCapabilityMatcher;
import com.rmn.qa.AutomationConstants;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationDynamicNode;
import com.rmn.qa.AutomationRequestMatcher;
import com.rmn.qa.AutomationRunRequest;
import com.rmn.qa.AutomationUtils;
import com.rmn.qa.MockRemoteProxy;
import com.rmn.qa.MockRequestMatcher;
import com.rmn.qa.MockVmManager;

import junit.framework.Assert;

public class AutomationNodeCleanupTaskTest {

    @After
    public void tearDown() {
        AutomationContext.refreshContext();
    }

    @Test
    // Tests that the hard coded name of the task is correct
    public void testTaskName() {
        AutomationNodeCleanupTask task = new AutomationNodeCleanupTask(null,null,null);
        Assert.assertEquals("Name should be the same",AutomationNodeCleanupTask.NAME, task.getDescription()  );
    }

    @Test
    // Tests that the status of a new node does not change after the task runs.  This should not happen
    // until the node has reached the configured age
    public void testNodeNotOldEnough() {
        MockAutomationNodeCleanupTask task = new MockAutomationNodeCleanupTask(null,new MockVmManager(),new MockRequestMatcher());
        ProxySet proxySet = new ProxySet(false);
        task.setProxySet(proxySet);
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid","dummyId",null,null,new Date(),10);
        AutomationContext.getContext().addNode(node);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        task.run();
        Assert.assertEquals("Status should not be changed as node was not old enough", AutomationDynamicNode.STATUS.RUNNING,node.getStatus());
    }

    @Test
    // Tests that the node is set to expired after the configured amount of time
    public void testNodeSetToExpired() {
        MockAutomationNodeCleanupTask task = new MockAutomationNodeCleanupTask(null,new MockVmManager(),new MockRequestMatcher());
        ProxySet proxySet = new ProxySet(false);
        task.setProxySet(proxySet);
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid","dummyId",null,null,AutomationUtils.modifyDate(new Date(),-56, Calendar.MINUTE),10);
        AutomationContext.getContext().addNode(node);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        task.run();
        Assert.assertEquals("Node should be expired as the end date was old enough", AutomationDynamicNode.STATUS.EXPIRED,node.getStatus());
    }

    @Test
    // Tests that the node is not automatically shutdown (terminated) because its currently running tests
    public void testNodeCantBeShutDown() {
        MockRequestMatcher matcher = new MockRequestMatcher();
        matcher.setThreadsToReturn(4);
        MockAutomationNodeCleanupTask task = new MockAutomationNodeCleanupTask(null,new MockVmManager(),matcher);
        ProxySet proxySet = new ProxySet(false);
        task.setProxySet(proxySet);
        String nodeId = "dummyId";
        AutomationDynamicNode node = new AutomationDynamicNode(nodeId,"dummyId",null,null,AutomationUtils.modifyDate(new Date(),-56, Calendar.MINUTE),10);
        AutomationContext.getContext().addNode(node);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxySet.add(proxy);
        Map<String,Object> config = new HashMap<String, Object>();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> capabilities = new HashMap<String,Object>();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        // Assign a session to the test slot
        testSlot.getNewSession(capabilities);
        proxy.setMultipleTestSlots(testSlot, 5);
        matcher.setInProgressTests("firefox",5);
        task.run();
        Assert.assertEquals("There should not be sufficient free capacity to cause the node to get shut down", AutomationDynamicNode.STATUS.RUNNING,node.getStatus());
    }

    @Test
    // Tests that the node cannot be shutdown because a new run has been registered and just not started yet
    public void testNodeCantBeShutDownDueToNewRun() {
        MockRequestMatcher matcher = new MockRequestMatcher();
        matcher.setThreadsToReturn(4);
        MockAutomationNodeCleanupTask task = new MockAutomationNodeCleanupTask(null,new MockVmManager(),matcher);
        ProxySet proxySet = new ProxySet(false);
        task.setProxySet(proxySet);
        String nodeId = "dummyId";
        AutomationDynamicNode node = new AutomationDynamicNode(nodeId,"dummyId",null,null,AutomationUtils.modifyDate(new Date(),-56, Calendar.MINUTE),10);
        AutomationContext.getContext().addNode(node);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxySet.add(proxy);
        Map<String,Object> config = new HashMap<String, Object>();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> capabilities = new HashMap<String,Object>();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        // Assign a session to the test slot
        testSlot.getNewSession(capabilities);
        proxy.setMultipleTestSlots(testSlot, 5);
        matcher.setInProgressTests("firefox",5);
        task.run();
        Assert.assertEquals("There should not be sufficient free capacity to cause the node to get shut down", AutomationDynamicNode.STATUS.RUNNING,node.getStatus());
    }

    @Test
    // Tests that if new registered run does not match the node capabilities, it can be shut down
    public void testNodeShutDownNoMatchingInProgressTests() {
        MockAutomationNodeCleanupTask task = new MockAutomationNodeCleanupTask(null,new MockVmManager(),new MockRequestMatcher());
        ProxySet proxySet = new ProxySet(false);
        task.setProxySet(proxySet);
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid","dummyId",null,null,AutomationUtils.modifyDate(new Date(),-56, Calendar.MINUTE),10);
        AutomationContext.getContext().addNode(node);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        AutomationRunRequest newRequest = new AutomationRunRequest("uuid",10,"firefox");
        AutomationContext.getContext().addRun(newRequest);
        task.run();
        Assert.assertEquals("Node should NOT be expired as a new run has started", AutomationDynamicNode.STATUS.RUNNING,node.getStatus());
    }

    @Test
    // Tests that a node can be terminated if it is empty
    public void testNodeSetToTerminatedEmpty() {
        MockAutomationNodeCleanupTask task = new MockAutomationNodeCleanupTask(null,new MockVmManager(),new MockRequestMatcher());
        ProxySet proxySet = new ProxySet(false);
        task.setProxySet(proxySet);
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid","dummyId",null,null,AutomationUtils.modifyDate(new Date(),-56, Calendar.MINUTE),10);
        AutomationContext.getContext().addNode(node);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        task.run();
        Assert.assertEquals("Status should change to expired first", AutomationDynamicNode.STATUS.EXPIRED, node.getStatus());
        task.run();
        Assert.assertEquals("Node should be terminated as it was empty", AutomationDynamicNode.STATUS.TERMINATED, node.getStatus());
    }

    @Test
    // Tests that if a node still has tests running against it will only get set to expired and not terminated
    public void testNodeNotSetToTerminatedNotEmpty() {
        String nodeId = "nodeId";
        MockRequestMatcher matcher = new MockRequestMatcher();
        matcher.setThreadsToReturn(10);
        MockAutomationNodeCleanupTask task = new MockAutomationNodeCleanupTask(null,new MockVmManager(),matcher);
        ProxySet proxySet = new ProxySet(false);
        task.setProxySet(proxySet);
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,AutomationUtils.modifyDate(new Date(),-56, Calendar.MINUTE),10);

        AutomationContext.getContext().addNode(node);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxySet.add(proxy);
        Map<String,Object> config = new HashMap<String, Object>();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> capabilities = new HashMap<String,Object>();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        // Assign a session to the test slot
        testSlot.getNewSession(capabilities);
        proxy.setMultipleTestSlots(testSlot, 5);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        task.run();
        Assert.assertEquals("Status should change to expired first", AutomationDynamicNode.STATUS.EXPIRED,node.getStatus());
        task.run();
        Assert.assertEquals("Node should be expired as it was not empty", AutomationDynamicNode.STATUS.EXPIRED,node.getStatus());
    }

    @Test
    // Tests that the node is not terminated if it is in the next billing cycle
    public void testNodeNotTerminatedNextBillingCycle() {
        MockAutomationNodeCleanupTask task = new MockAutomationNodeCleanupTask(null,new MockVmManager(),new MockRequestMatcher());
        ProxySet proxySet = new ProxySet(false);
        task.setProxySet(proxySet);
        Date startDate = new Date();
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid","dummyId",null,null,AutomationUtils.modifyDate(startDate,-56, Calendar.MINUTE),10);
        AutomationContext.getContext().addNode(node);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        task.run();
        Assert.assertEquals("Status should change to expired first", AutomationDynamicNode.STATUS.EXPIRED, node.getStatus());
        Date newEndDate = AutomationUtils.modifyDate(new Date(),-61, Calendar.MINUTE);
        node.setEndDate(newEndDate);
        task.run();
        Assert.assertEquals("Node should be running as its in the next billing cycle", AutomationDynamicNode.STATUS.RUNNING, node.getStatus());
        Assert.assertTrue("End date should be increased",node.getEndDate().after(AutomationUtils.modifyDate(newEndDate,54,Calendar.MINUTE)));
    }

    @Test
    // Tests that with an even number of tests (4 tests) among 6 (3 nodes, 2 tests each) capable nodes, that only
    // 1 node will be terminated
    public void testMultipleNodesNotSufficientLoad() {
        AutomationRequestMatcher matcher = new AutomationRequestMatcher();
        MockAutomationNodeCleanupTask task = new MockAutomationNodeCleanupTask(null,new MockVmManager(),matcher);
        ProxySet proxySet = new ProxySet(false);
        task.setProxySet(proxySet);
        String nodeId = "dummyId";
        String nodeId2 = "dummyId2";
        String nodeId3 = "dummyId3";
        List<AutomationDynamicNode> nodes = new ArrayList<>();
        AutomationDynamicNode node = new AutomationDynamicNode("dummyUuid",nodeId,null,null,AutomationUtils.modifyDate(new Date(),-56, Calendar.MINUTE),10);
        AutomationDynamicNode node2 = new AutomationDynamicNode("dummyUuid2",nodeId2,null,null,AutomationUtils.modifyDate(new Date(),-56, Calendar.MINUTE),10);
        AutomationDynamicNode node3 = new AutomationDynamicNode("dummyUuid3",nodeId3,null,null,AutomationUtils.modifyDate(new Date(),-56, Calendar.MINUTE),10);
        nodes.add(node);
        nodes.add(node2);
        nodes.add(node3);
        AutomationContext.getContext().addNode(node);
        AutomationContext.getContext().addNode(node2);
        AutomationContext.getContext().addNode(node3);
        MockRemoteProxy proxy = new MockRemoteProxy();
        MockRemoteProxy proxy2 = new MockRemoteProxy();
        MockRemoteProxy proxy3 = new MockRemoteProxy();
        proxySet.add(proxy);
        proxySet.add(proxy2);
        proxySet.add(proxy3);
        Map<String,Object> config = new HashMap<>();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        proxy.setMaxNumberOfConcurrentTestSessions(4);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());

        Map<String,Object> config2 = new HashMap<>();
        config2.put(AutomationConstants.INSTANCE_ID,nodeId2);
        proxy2.setMaxNumberOfConcurrentTestSessions(4);
        proxy2.setConfig(config2);
        proxy2.setCapabilityMatcher(new AutomationCapabilityMatcher());


        Map<String,Object> config3 = new HashMap<>();
        config3.put(AutomationConstants.INSTANCE_ID,nodeId3);
        proxy3.setMaxNumberOfConcurrentTestSessions(4);
        proxy3.setConfig(config3);
        proxy3.setCapabilityMatcher(new AutomationCapabilityMatcher());

        Map<String,Object> capabilities = new HashMap<>();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        TestSlot testSlotUsed = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        TestSlot testSlotNotUsed = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        // Assign a session to the test slot
        testSlotUsed.getNewSession(capabilities);
        proxy.setMultipleTestSlots(testSlotUsed, 2);
        proxy.setMultipleTestSlots(testSlotNotUsed, 2);

        TestSlot testSlotUsed2 = new TestSlot(proxy2, SeleniumProtocol.WebDriver,null,capabilities);
        TestSlot testSlotNotUsed2 = new TestSlot(proxy2, SeleniumProtocol.WebDriver,null,capabilities);
        // Assign a session to the test slot
        testSlotUsed2.getNewSession(capabilities);
        proxy2.setMultipleTestSlots(testSlotUsed2, 2);
        proxy2.setMultipleTestSlots(testSlotNotUsed2, 2);

        TestSlot testSlotUsed3 = new TestSlot(proxy3, SeleniumProtocol.WebDriver,null,capabilities);
        TestSlot testSlotNotUsed3 = new TestSlot(proxy3, SeleniumProtocol.WebDriver,null,capabilities);
        // Assign a session to the test slot
        testSlotUsed3.getNewSession(capabilities);
        proxy3.setMultipleTestSlots(testSlotUsed3, 2);
        proxy3.setMultipleTestSlots(testSlotNotUsed3, 2);
        task.run();


        int numRunning=0;
        int numExpired=0;
        for(int i =0;i<nodes.size();i++) {
            if(nodes.get(i).getStatus() == AutomationDynamicNode.STATUS.RUNNING) {
                numRunning++;
            } else if(nodes.get(i).getStatus() == AutomationDynamicNode.STATUS.EXPIRED) {
                numExpired++;
            }
        }
        Assert.assertEquals("Number of nodes still running is incorrect", 2,numRunning);
        Assert.assertEquals("Only one node should have been marked expired", 1,numExpired);
    }

    @Test
    // Tests that a terminated node is removed from tracking after the configured amount of time
    public void testNodeRemovedAfterTime() {
        MockAutomationNodeCleanupTask task = new MockAutomationNodeCleanupTask(null,new MockVmManager(),new MockRequestMatcher());
        ProxySet proxySet = new ProxySet(false);
        task.setProxySet(proxySet);
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid","dummyId",null,null,AutomationUtils.modifyDate(new Date(),-56, Calendar.MINUTE),10);
        AutomationContext.getContext().addNode(node);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        task.run();
        Assert.assertEquals("Status should change to expired first", AutomationDynamicNode.STATUS.EXPIRED, node.getStatus());
        task.run();
        Assert.assertEquals("Node should be terminated as it was empty", AutomationDynamicNode.STATUS.TERMINATED, node.getStatus());
        Assert.assertNull("Node should no longer be tracked",AutomationContext.getContext().getNode(node.getInstanceId()));
        node.setEndDate(AutomationUtils.modifyDate(new Date(), -45, Calendar.MINUTE));
        task.run();
        Assert.assertNull("Node should not be tracked after its been terminated for 30 minutes", AutomationContext.getContext().getNode(node.getInstanceId()));
    }
}
