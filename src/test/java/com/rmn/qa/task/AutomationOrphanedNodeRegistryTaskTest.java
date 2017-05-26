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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openqa.grid.internal.ProxySet;
import org.openqa.selenium.Platform;

import com.rmn.qa.AutomationCapabilityMatcher;
import com.rmn.qa.AutomationConstants;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationDynamicNode;
import com.rmn.qa.BaseTest;
import com.rmn.qa.MockRemoteProxy;
import com.rmn.qa.aws.AwsVmManager;

import junit.framework.Assert;

/**
 * Created by mhardin on 4/24/14.
 */
public class AutomationOrphanedNodeRegistryTaskTest extends BaseTest {

    @Test
    // Tests that the hardcoded name of the task is correct
    public void testTaskName() {
        AutomationOrphanedNodeRegistryTask task = new AutomationOrphanedNodeRegistryTask(null);
        Assert.assertEquals("Name should be the same", AutomationOrphanedNodeRegistryTask.NAME, task.getDescription());
    }

    @Test
    // Test that a node not in the context gets picked up and registered by the task
    public void testRegisterNewNode() {
        MockAutomationOrphanedNodeRegistryTask task = new MockAutomationOrphanedNodeRegistryTask(null);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxySet.add(proxy);
        Map<String,String> config = new HashMap<>();
        String instanceId = "instanceId";
        String uuid="testUuid";
        int threadCount = 10;
        String browser = "firefox";
        Platform os = Platform.LINUX;
        config.put(AutomationConstants.INSTANCE_ID,instanceId);
        config.put(AutomationConstants.UUID,uuid);
        //config.put(AutomationConstants.CONFIG_MAX_SESSION, threadCount);
        proxy.getConfig().maxSession = threadCount;
        config.put(AutomationConstants.CONFIG_BROWSER, browser);
        config.put(AutomationConstants.CONFIG_OS, os.toString());
        config.put(AutomationConstants.CONFIG_CREATED_DATE, AwsVmManager.NODE_DATE_FORMAT.format(new Date()));
        proxy.setConfig(config);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        task.setProxySet(proxySet);
        Assert.assertNull("Node should not be registered before the task runs",AutomationContext.getContext().getNode(instanceId));
        task.run();
        AutomationDynamicNode existingNode = AutomationContext.getContext().getNode(instanceId);
        Assert.assertNotNull("Node should exist after the task has run",existingNode);
        Assert.assertEquals("UUID should match", uuid,existingNode.getUuid());
        Assert.assertEquals("Browser should match", browser,existingNode.getBrowser());
        Assert.assertEquals("Thread count should match", threadCount,existingNode.getNodeCapacity());
        Assert.assertEquals("OS should match", os, existingNode.getPlatform());
    }

    @Test
    // Test if a node already exists in the context that the task does not override that node
    public void testNodeAlreadyExists() {
        MockAutomationOrphanedNodeRegistryTask task = new MockAutomationOrphanedNodeRegistryTask(null);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxySet.add(proxy);
        Map<String,String> config = new HashMap<>();
        String instanceId = "instanceId";
        String uuid="testUuid";
        int threadCount = 10;
        String browser = "firefox";
        Platform os = Platform.LINUX;
        config.put(AutomationConstants.INSTANCE_ID,instanceId);
        config.put(AutomationConstants.UUID,"fake");
        //config.put(AutomationConstants.CONFIG_MAX_SESSION, 1);
        proxy.getConfig().maxSession = 1;
        config.put(AutomationConstants.CONFIG_BROWSER, "fake");
        config.put(AutomationConstants.CONFIG_OS, "fake");
        config.put(AutomationConstants.CONFIG_CREATED_DATE, AwsVmManager.NODE_DATE_FORMAT.format(new Date()));
        proxy.setConfig(config);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        task.setProxySet(proxySet);
        AutomationDynamicNode existingNode = new AutomationDynamicNode(uuid,instanceId,browser,os,new Date(),threadCount);
        AutomationContext.getContext().addNode(existingNode);
        Assert.assertNotNull("Node should be registered before the task runs",AutomationContext.getContext().getNode(instanceId));
        task.run();
        AutomationDynamicNode newNode = AutomationContext.getContext().getNode(instanceId);
        Assert.assertNotNull("Node should still exist after the task has run",newNode);
        Assert.assertEquals("UUID should match the previously existing node", uuid, newNode.getUuid());
        Assert.assertEquals("Browser should match the previously existing node", browser,newNode.getBrowser());
        Assert.assertEquals("Thread count should match the previously existing node", threadCount, newNode.getNodeCapacity());
        Assert.assertEquals("OS should match the previously existing node", os, newNode.getPlatform());
    }

    @Test
    // Test that a node without an instance id does not get registered
    public void testRegisterNodeWithoutInstanceId() {
        MockAutomationOrphanedNodeRegistryTask task = new MockAutomationOrphanedNodeRegistryTask(null);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxySet.add(proxy);
        Map<String,String> config = new HashMap<>();
        String uuid="testUuid";
        int threadCount = 10;
        String browser = "firefox";
        String os = "linux";
        config.put(AutomationConstants.UUID,uuid);
        //config.put(AutomationConstants.CONFIG_MAX_SESSION, threadCount);
        proxy.getConfig().maxSession = threadCount;
        config.put(AutomationConstants.CONFIG_BROWSER, browser);
        config.put(AutomationConstants.CONFIG_OS, os);
        config.put(AutomationConstants.CONFIG_CREATED_DATE, AwsVmManager.NODE_DATE_FORMAT.format(new Date()));
        proxy.setConfig(config);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        task.setProxySet(proxySet);
        Assert.assertEquals("Node should not be registered before the task runs",0,AutomationContext.getContext().getNodes().size());
        task.run();
        Assert.assertEquals("Node should still not be registered after the task runs",0,AutomationContext.getContext().getNodes().size());
    }

    @Test
    // Test that an empty proxy set does not result in any added nodes after the register task runs
    public void testEmptyProxySet() {
        MockAutomationOrphanedNodeRegistryTask task = new MockAutomationOrphanedNodeRegistryTask(null);
        ProxySet proxySet = new ProxySet(false);
        task.setProxySet(proxySet);
        Assert.assertEquals("Node should not be registered before the task runs",0,AutomationContext.getContext().getNodes().size());
        task.run();
        Assert.assertEquals("Node should still not be registered after the task runs",0,AutomationContext.getContext().getNodes().size());
    }

    @Test
    // Test that a null proxy set does not result in any added nodes
    public void testNullEmptyProxySet() {
        MockAutomationOrphanedNodeRegistryTask task = new MockAutomationOrphanedNodeRegistryTask(null);
        ProxySet proxySet = new ProxySet(false);
        task.setProxySet(proxySet);
        Assert.assertEquals("Node should not be registered before the task runs",0,AutomationContext.getContext().getNodes().size());
        task.run();
        Assert.assertEquals("Node should still not be registered after the task runs",0,AutomationContext.getContext().getNodes().size());
    }

    @Test
    // Test that a node with a bad date does not register successfully after the task runs
    public void testBadDateFormat() {
        MockAutomationOrphanedNodeRegistryTask task = new MockAutomationOrphanedNodeRegistryTask(null);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxySet.add(proxy);
        Map<String,String> config = new HashMap<>();
        String instanceId = "instanceId";
        String uuid="testUuid";
        int threadCount = 10;
        String browser = "firefox";
        String os = "linux";
        config.put(AutomationConstants.INSTANCE_ID,instanceId);
        config.put(AutomationConstants.UUID,uuid);
        //config.put(AutomationConstants.CONFIG_MAX_SESSION, threadCount);
        proxy.getConfig().maxSession = threadCount;
        config.put(AutomationConstants.CONFIG_BROWSER, browser);
        config.put(AutomationConstants.CONFIG_OS, os);

        DateFormat badDateFormat = new SimpleDateFormat("MM HH:mm:ss");
        config.put(AutomationConstants.CONFIG_CREATED_DATE, badDateFormat.format(new Date()));
        proxy.setConfig(config);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        task.setProxySet(proxySet);
        Assert.assertNull("Node should not be registered before the task runs",AutomationContext.getContext().getNode(instanceId));
        task.run();
        Assert.assertNull("Node should still not be registered after the task runs",AutomationContext.getContext().getNode(instanceId));
    }

}
