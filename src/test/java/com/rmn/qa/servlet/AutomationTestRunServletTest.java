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

package com.rmn.qa.servlet;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.openqa.grid.common.SeleniumProtocol;
import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.TestSlot;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;

import com.rmn.qa.AutomationCapabilityMatcher;
import com.rmn.qa.AutomationConstants;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationDynamicNode;
import com.rmn.qa.AutomationRunRequest;
import com.rmn.qa.BaseTest;
import com.rmn.qa.MockHttpServletRequest;
import com.rmn.qa.MockHttpServletResponse;
import com.rmn.qa.MockRemoteProxy;
import com.rmn.qa.MockRequestMatcher;
import com.rmn.qa.MockVmManager;

import junit.framework.Assert;

public class AutomationTestRunServletTest extends BaseTest {

    @Test
    // Makes sure that the query string parameter 'uuid' is required
    public void testUuidRequired() throws IOException, ServletException{
        MockAutomationTestRunServlet servlet = new MockAutomationTestRunServlet(null,false, new MockVmManager(), new MockRequestMatcher());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.doGet(request,response);
        Assert.assertEquals("Response code should be an error since the uuid was not passed in",
                HttpServletResponse.SC_BAD_REQUEST,response.getErrorCode());

        Assert.assertEquals("Response message should be an error since the uuid was not passed in",
                "Parameter 'uuid' must be passed in as a query string parameter", response.getErrorMessage());
    }

    @Test
    // Makes sure that the query string parameter 'browser' is required
    public void testBrowserRequired() throws IOException, ServletException{
        MockAutomationTestRunServlet servlet = new MockAutomationTestRunServlet(null,false, new MockVmManager(), new MockRequestMatcher());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("uuid", "testUuid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.doGet(request,response);
        Assert.assertEquals("Response code should be an error since browser was not passed in",
                HttpServletResponse.SC_BAD_REQUEST,response.getErrorCode());

        Assert.assertEquals("Response message should be an error since browser was not passed in",
                "Parameter 'browser' must be passed in as a query string parameter", response.getErrorMessage());
    }

    @Test
    // Makes sure that the query string parameter 'threadCount' is required
    public void testThreadCountRequired() throws IOException, ServletException{
        MockAutomationTestRunServlet servlet = new MockAutomationTestRunServlet(null,false, new MockVmManager(), new MockRequestMatcher());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("uuid", "testUuid");
        request.setParameter("browser", "firefox");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.doGet(request,response);
        Assert.assertEquals("Response code should be an error since threadCount was not passed in",
                HttpServletResponse.SC_BAD_REQUEST,response.getErrorCode());

        Assert.assertEquals("Response message should be an error since threadCount was not passed in",
                "Parameter 'threadCount' must be passed in as a query string parameter", response.getErrorMessage());
    }

    @Test
    // Tests if a hub has maxed out its configurable thread count that the hub will not attempt
    // to fulfill the request
    public void testHubThreadCountMaxedOut() throws IOException, ServletException{
        MockAutomationTestRunServlet servlet = new MockAutomationTestRunServlet(null,false, new MockVmManager(), new MockRequestMatcher());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("uuid","testUuid");
        request.setParameter("browser","firefox");
        request.setParameter("os", Platform.ANY.toString());
        request.setParameter("threadCount", "50");
        AutomationContext.getContext().addRun(new AutomationRunRequest("runUUid",50,"firefox"));
        AutomationContext.getContext().setTotalNodeCount(50);
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.doGet(request,response);
        Assert.assertEquals("Response code should be an error since the max thread count was reached",
                HttpServletResponse.SC_CONFLICT,response.getErrorCode());

        Assert.assertEquals("Response code should be an error since the max thread count was reached",
                "Server cannot fulfill request due to configured node limit being reached.", response.getErrorMessage());
    }

    @Test
    // Tests that the configurable maximum thread count works with multiple nodes
    public void testHubThreadCountMaxedOutMultipleRuns() throws IOException, ServletException{
        MockAutomationTestRunServlet servlet = new MockAutomationTestRunServlet(null,false, new MockVmManager(), new MockRequestMatcher());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("uuid","testUuid");
        request.setParameter("browser","firefox");
        request.setParameter("os", Platform.ANY.toString());
        request.setParameter("threadCount","50");
        AutomationContext.getContext().addRun(new AutomationRunRequest("runUUid",25,"firefox"));
        AutomationContext.getContext().addRun(new AutomationRunRequest("runUUid",25,"firefox"));
        AutomationContext.getContext().setTotalNodeCount(50);
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.doGet(request,response);
        Assert.assertEquals("Response code should be an error since the max thread count was reached",
                HttpServletResponse.SC_CONFLICT,response.getErrorCode());

        Assert.assertEquals("Response code should be an error since the max thread count was reached",
                "Server cannot fulfill request due to configured node limit being reached.", response.getErrorMessage());
    }

    @Test
    // Tests the happy path that a hub says a request can be fulfilled when it can be
    public void testRequestCanFulfill() throws IOException, ServletException{
        MockVmManager manageEc2 = new MockVmManager();
        MockRequestMatcher matcher = new MockRequestMatcher();
        matcher.setThreadsToReturn(10);
        MockAutomationTestRunServlet servlet = new MockAutomationTestRunServlet(null,false, manageEc2,matcher);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("uuid","testUuid");
        request.setParameter("browser","firefox");
        request.setParameter("os", Platform.ANY.toString());
        request.setParameter("threadCount","10");
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = new HashMap<String, Object>();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = new HashMap<String,Object>();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        servlet.setProxySet(proxySet);
        AutomationContext.getContext().setTotalNodeCount(50);
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.doGet(request, response);
        Assert.assertEquals("Hub should be able to fulfill request",
                HttpServletResponse.SC_ACCEPTED,response.getStatusCode());
    }

    @Test
    // Tests that a run request with a duplicate uuid of another run is rejected
    public void testDuplicateUuids() throws IOException, ServletException{
        MockVmManager manageEc2 = new MockVmManager();
        MockRequestMatcher matcher = new MockRequestMatcher();
        matcher.setThreadsToReturn(10);
        MockAutomationTestRunServlet servlet = new MockAutomationTestRunServlet(null,false, manageEc2,matcher);
        MockHttpServletRequest request = new MockHttpServletRequest();
        String uuid = "testUUid";
        request.setParameter("uuid",uuid);
        request.setParameter("browser","firefox");
        request.setParameter("os", Platform.ANY.toString());
        request.setParameter("threadCount","10");
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = new HashMap<String, Object>();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = new HashMap<String,Object>();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        servlet.setProxySet(proxySet);
        AutomationContext.getContext().setTotalNodeCount(50);
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.doGet(request, response);
        // Submit a 2nd request with the same UUID
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        servlet.doGet(request, response2);
        Assert.assertEquals("Hub should not able to fulfill request due to duplicate uuid",HttpServletResponse.SC_BAD_REQUEST,response2.getErrorCode());
        Assert.assertEquals("Hub should not able to fulfill request due to duplicate uuid","Test run already exists with the same UUID.",response2.getErrorMessage());
    }

    @Test
    // Tests a request can be fulfilled when nodes have to be spun up
    public void testRequestCanFulfillSpinUpNodes() throws IOException, ServletException{
        MockVmManager manageEc2 = new MockVmManager();
        MockRequestMatcher matcher = new MockRequestMatcher();
        matcher.setThreadsToReturn(0);
        MockAutomationTestRunServlet servlet = new MockAutomationTestRunServlet(null,false, manageEc2,matcher);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("uuid","testUuid");
        request.setParameter("browser","firefox");
        request.setParameter("os", Platform.ANY.toString());
        request.setParameter("threadCount","6");
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = new HashMap<String, Object>();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = new HashMap<String,Object>();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        servlet.setProxySet(proxySet);
        AutomationContext.getContext().setTotalNodeCount(50);
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.doGet(request, response);
        Assert.assertEquals("Hub should be able to fulfill request",
                HttpServletResponse.SC_CREATED,response.getStatusCode());
    }

    @Test
    // Tests a request can be fulfilled when chrome nodes have to be spun up
    public void testRequestCanFulfillSpinUpNodesChrome() throws IOException, ServletException{
        MockVmManager manageEc2 = new MockVmManager();
        MockRequestMatcher matcher = new MockRequestMatcher();
        matcher.setThreadsToReturn(0);
        MockAutomationTestRunServlet servlet = new MockAutomationTestRunServlet(null,false, manageEc2,matcher);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("uuid","testUuid");
        request.setParameter("browser","chrome");
        request.setParameter("os", Platform.LINUX.toString());
        request.setParameter("threadCount","7");
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = new HashMap<String, Object>();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = new HashMap<String,Object>();
        capabilities.put(CapabilityType.BROWSER_NAME,"chrome");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        servlet.setProxySet(proxySet);
        AutomationContext.getContext().setTotalNodeCount(50);
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.doGet(request, response);
        Assert.assertEquals("Hub should be able to fulfill request",
                HttpServletResponse.SC_CREATED,response.getStatusCode());
    }

    @Test
    // Tests a request can be fulfilled when IE nodes have to be spun up
    public void testRequestCanFulfillSpinUpNodesIe() throws IOException, ServletException{
        MockVmManager manageEc2 = new MockVmManager();
        System.setProperty(AutomationConstants.IP_ADDRESS,"192.168.1.1");
        MockRequestMatcher matcher = new MockRequestMatcher();
        matcher.setThreadsToReturn(0);
        MockAutomationTestRunServlet servlet = new MockAutomationTestRunServlet(null,false, manageEc2,matcher);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("uuid","testUuid");
        request.setParameter("browser","internetexplorer");
        request.setParameter("os", Platform.WINDOWS.toString());
        request.setParameter("threadCount","1");
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = new HashMap<String, Object>();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = new HashMap<String,Object>();
        capabilities.put(CapabilityType.BROWSER_NAME,"internetexplorer");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        servlet.setProxySet(proxySet);
        AutomationContext.getContext().setTotalNodeCount(50);
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.doGet(request, response);
        Assert.assertEquals("Hub should be able to fulfill request",
                HttpServletResponse.SC_CREATED,response.getStatusCode());
    }

    @Test
    // Tests a request cannot be fulfilled when the browser is not a supported browser to spin up
    public void testRequestCantFulfillUnsupportedBrowser() throws IOException, ServletException{
        MockVmManager manageEc2 = new MockVmManager();
        MockRequestMatcher matcher = new MockRequestMatcher();
        matcher.setThreadsToReturn(0);
        MockAutomationTestRunServlet servlet = new MockAutomationTestRunServlet(null,false, manageEc2,matcher);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("uuid","testUuid");
        request.setParameter("browser", BrowserType.SAFARI.toString());
        request.setParameter("os", Platform.WINDOWS.toString());
        request.setParameter("threadCount","7");
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = new HashMap<String, Object>();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = new HashMap<String,Object>();
        capabilities.put(CapabilityType.BROWSER_NAME,"chrome");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        servlet.setProxySet(proxySet);
        AutomationContext.getContext().setTotalNodeCount(50);
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.doGet(request, response);
        Assert.assertEquals("Hub should not be able to fulfill request",
                HttpServletResponse.SC_GONE,response.getErrorCode());
    }

    @Test
    // Tests that the cleanup threads are started
    public void testInitCleanupThreads() throws IOException, ServletException{
        System.setProperty(AutomationConstants.INSTANCE_ID,"dummyId");
        MockVmManager manageEc2 = new MockVmManager();
        MockRequestMatcher matcher = new MockRequestMatcher();
        matcher.setThreadsToReturn(0);
        MockAutomationTestRunServlet servlet = new MockAutomationTestRunServlet(null,true, manageEc2,matcher);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("uuid","testUuid");
        request.setParameter("browser","chrome");
        request.setParameter("os", Platform.WINDOWS.toString());
        request.setParameter("threadCount","7");
        request.setParameter("browserVersion","21");
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = new HashMap<String, Object>();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = new HashMap<String,Object>();
        capabilities.put(CapabilityType.BROWSER_NAME,"chrome");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        servlet.setProxySet(proxySet);
        AutomationContext.getContext().setTotalNodeCount(50);
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.doGet(request, response);
        Assert.assertEquals("Hub should be able to fulfill request",
                HttpServletResponse.SC_CREATED,response.getStatusCode());
    }

    @Test
    public void testInitCleanupThreadsNoInstanceId() throws IOException, ServletException{
        MockVmManager manageEc2 = new MockVmManager();
        MockRequestMatcher matcher = new MockRequestMatcher();
        matcher.setThreadsToReturn(0);
        MockAutomationTestRunServlet servlet = new MockAutomationTestRunServlet(null,true, manageEc2,matcher);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("uuid","testUuid");
        request.setParameter("browser","chrome");
        request.setParameter("os", Platform.WINDOWS.toString());
        request.setParameter("threadCount","7");
        request.setParameter("browserVersion","21");
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = new HashMap<String, Object>();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = new HashMap<String,Object>();
        capabilities.put(CapabilityType.BROWSER_NAME,"chrome");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        servlet.setProxySet(proxySet);
        AutomationContext.getContext().setTotalNodeCount(50);
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.doGet(request, response);
        Assert.assertEquals("Hub should be able to fulfill request",
                HttpServletResponse.SC_CREATED,response.getStatusCode());
    }

    @Test
    //TODO Update descriptions
    public void testRequestStartNodesFailed() throws IOException, ServletException{
        MockVmManager manageEc2 = new MockVmManager();
        manageEc2.setThrowException();
        MockRequestMatcher matcher = new MockRequestMatcher();
        matcher.setThreadsToReturn(0);
        MockAutomationTestRunServlet servlet = new MockAutomationTestRunServlet(null,false, manageEc2,matcher);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("uuid","testUuid");
        request.setParameter("browser","firefox");
        request.setParameter("os", Platform.WINDOWS.toString());
        request.setParameter("threadCount","10");
        String nodeId = "nodeId";
        // Add a node that is not running to make sure its not included in the available calculation
        AutomationDynamicNode node = new AutomationDynamicNode("testUuid",nodeId,null,null,new Date(),50);
        AutomationContext.getContext().addNode(node);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setMaxNumberOfConcurrentTestSessions(50);
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> config = new HashMap<String, Object>();
        config.put(AutomationConstants.INSTANCE_ID,nodeId);
        proxy.setConfig(config);
        Map<String,Object> capabilities = new HashMap<String,Object>();
        capabilities.put(CapabilityType.BROWSER_NAME,"firefox");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        proxy.setMultipleTestSlots(testSlot, 10);
        proxySet.add(proxy);
        servlet.setProxySet(proxySet);
        AutomationContext.getContext().setTotalNodeCount(50);
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.doGet(request, response);
        Assert.assertEquals("Hub should be able to fulfill request",
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,response.getErrorCode());
        Assert.assertEquals("Error message should return if nodes can't be started","Nodes could not be started: Can't start nodes",response.getErrorMessage());
    }

}