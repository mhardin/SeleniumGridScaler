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

import junit.framework.Assert;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.After;
import org.junit.Test;
import org.openqa.grid.common.SeleniumProtocol;
import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.TestSlot;
import org.openqa.grid.internal.utils.CapabilityMatcher;
import org.openqa.selenium.remote.CapabilityType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mhardin on 5/1/14.
 */
public class AutomationRunContextTest {

    @After()
    public void cleanUp() {
        AutomationContext.refreshContext();
    }

    @Test
    // Tests that an old run gets cleaned up (removed)
    public void testOldRun() {
        AutomationRunRequest oldRequest = new AutomationRunRequest("uuid",10,"firefox","10","linux",AutomationUtils.modifyDate(new Date(),-15, Calendar.MINUTE));
         AutomationRunContext context = AutomationContext.getContext();
        context.addRun(oldRequest);

        Assert.assertTrue("Run should exist", context.hasRun(oldRequest.getUuid()));
        ProxySet proxySet = new ProxySet(false);
        proxySet.add(new MockRemoteProxy());
        context.cleanUpRunRequests(proxySet);
        Assert.assertFalse("Run request should no longer exist as it should have been removed", context.hasRun(oldRequest.getUuid()));
    }

    @Test
    // Tests that a new run does not get cleaned up (removed)
    public void testNewRun() {
        AutomationRunRequest oldRequest = new AutomationRunRequest("uuid",10,"firefox");
        AutomationRunContext context = AutomationContext.getContext();
        context.addRun(oldRequest);

        Assert.assertTrue("Run should exist", context.hasRun(oldRequest.getUuid()));
        ProxySet proxySet = new ProxySet(false);
        proxySet.add(new MockRemoteProxy());
        context.cleanUpRunRequests(proxySet);
        Assert.assertTrue("Run request should still exist as the run was new enough", context.hasRun(oldRequest.getUuid()));
    }

    @Test
    // Tests that a new run does not get cleaned up (removed)
    public void testNewRunIE() {
        AutomationRunRequest oldRequest = new AutomationRunRequest("uuid",10,"internetexplorer","10","linux",AutomationUtils.modifyDate(new Date(),-7, Calendar.MINUTE));
        AutomationRunContext context = AutomationContext.getContext();
        context.addRun(oldRequest);


        Assert.assertTrue("Run should exist", context.hasRun(oldRequest.getUuid()));
        ProxySet proxySet = new ProxySet(false);
        proxySet.add(new MockRemoteProxy());
        context.cleanUpRunRequests(proxySet);
        Assert.assertTrue("Run request should still exist as the run was new enough", context.hasRun(oldRequest.getUuid()));
    }

    @Test
    // Tests that a new run does not get cleaned up (removed)
    public void testOldRunIE() {
        AutomationRunRequest oldRequest = new AutomationRunRequest("uuid",10,"internetexplorer","10","linux",AutomationUtils.modifyDate(new Date(),-15, Calendar.MINUTE));
        AutomationRunContext context = AutomationContext.getContext();
        context.addRun(oldRequest);


        Assert.assertTrue("Run should exist", context.hasRun(oldRequest.getUuid()));
        ProxySet proxySet = new ProxySet(false);
        proxySet.add(new MockRemoteProxy());
        context.cleanUpRunRequests(proxySet);
        Assert.assertFalse("Run request should no longer exist as it should have been removed", context.hasRun(oldRequest.getUuid()));
    }

    @Test
    // Tests that a run with slots does not get removed
    public void testActiveSession() {
        String uuid = "uuid";
        AutomationRunRequest request = new AutomationRunRequest(uuid,10,"firefox","10","linux",AutomationUtils.modifyDate(new Date(),-1, Calendar.HOUR));
        AutomationRunContext context = AutomationContext.getContext();
        context.addRun(request);

        Assert.assertTrue("Run should exist", context.hasRun(request.getUuid()));
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        CapabilityMatcher matcher = new AutomationCapabilityMatcher();
        proxy.setCapabilityMatcher(matcher);
        proxySet.add(proxy);
        Map<String,Object> config = new HashMap<>();
        config.put(AutomationConstants.UUID,uuid);
        proxy.setConfig(config);
        List<TestSlot> testSlots = new ArrayList<>();
        TestSlot testSlot = new TestSlot(proxy,null,null,config);
        proxy.setTestSlots(testSlots);
        testSlot.getNewSession(config);
        testSlots.add(testSlot);
        proxySet.add(proxy);
        context.cleanUpRunRequests(proxySet);
        Assert.assertTrue("Run request should still exist as there were active sessions", context.hasRun(request.getUuid()));
    }

    @Test
    // Tests that a run with slots does not get removed
    public void testNoSessions() {
        String uuid = "uuid";
        AutomationRunRequest request = new AutomationRunRequest(uuid,10,"firefox","10","linux",AutomationUtils.modifyDate(new Date(),-1, Calendar.HOUR));
        AutomationRunContext context = AutomationContext.getContext();
        context.addRun(request);

        Assert.assertTrue("Run should exist", context.hasRun(request.getUuid()));
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        CapabilityMatcher matcher = new AutomationCapabilityMatcher();
        proxy.setCapabilityMatcher(matcher);
        proxySet.add(proxy);
        Map<String,Object> config = new HashMap<>();
        config.put(AutomationConstants.UUID,uuid);
        proxy.setConfig(config);
        List<TestSlot> testSlots = new ArrayList<>();
        TestSlot testSlot = new TestSlot(proxy,null,null,config);
        proxy.setTestSlots(testSlots);
        testSlots.add(testSlot);
        proxySet.add(proxy);
        context.cleanUpRunRequests(proxySet);
        Assert.assertFalse("Run request should not exist as there were no active sessions", context.hasRun(request.getUuid()));
    }

    @Test
    // Tests that a newly created run is considered a 'new' run
    public void testIsNewRun() {
        AutomationRunRequest first = new AutomationRunRequest("uuid",3,"firefox");
        AutomationRunContext context = AutomationContext.getContext();
        context.addRun(first);
        Assert.assertTrue("Run should be considered new", context.isNewRunQueuedUp());
    }

    @Test
    // Tests that a newly created run is considered a 'new' run
    public void testNotNewRun() {
        AutomationRunRequest first = new AutomationRunRequest("uuid",null,null,null,null,AutomationUtils.modifyDate(new Date(),-3,Calendar.MINUTE));
        AutomationRunContext context = AutomationContext.getContext();
        context.addRun(first);
        Assert.assertFalse("Run should be considered new", context.isNewRunQueuedUp());
    }

    @Test
    // Tests that equals and hash code are working as expected
    public void equalsContract() {
        EqualsVerifier.forClass(AutomationRunRequest.class).suppress(Warning.NULL_FIELDS).verify();
    }

    @Test
    // Tests that the correct node count is returned when tests don't have a UUID
    public void testNodesFreeNoUuid() {
        AutomationRunContext runContext = new AutomationRunContext();
        runContext.setTotalNodeCount(10);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> capabilities = new HashMap<>();
        capabilities.put(CapabilityType.PLATFORM,"linux");
        capabilities.put(CapabilityType.BROWSER_NAME,"chrome");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        testSlot.getNewSession(capabilities);
        proxy.setMultipleTestSlots(testSlot,5);
        proxySet.add(proxy);
        int freeThreads = runContext.getTotalThreadsAvailable(proxySet);
        Assert.assertEquals(5,freeThreads);
    }

    @Test
    // Tests that the correct node count is returned when tests don't have a UUID
    public void testNodesFreeWithUuid() {
        AutomationRunContext runContext = new AutomationRunContext();
        runContext.setTotalNodeCount(10);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> capabilities = new HashMap<>();
        capabilities.put(CapabilityType.PLATFORM,"linux");
        capabilities.put(CapabilityType.BROWSER_NAME,"chrome");
        capabilities.put(AutomationConstants.UUID,"testUuid");
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        testSlot.getNewSession(capabilities);
        proxy.setMultipleTestSlots(testSlot,5);
        proxySet.add(proxy);
        int freeThreads = runContext.getTotalThreadsAvailable(proxySet);
        Assert.assertEquals(5,freeThreads);
    }

    @Test
    // Tests that a new run is counted instead of tests in progress
    public void testNewRunIsCounted() {
        String uuid = "testUuid";
        AutomationRunContext runContext = new AutomationRunContext();
        runContext.setTotalNodeCount(10);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> capabilities = new HashMap<>();
        capabilities.put(CapabilityType.PLATFORM,"linux");
        capabilities.put(CapabilityType.BROWSER_NAME,"chrome");
        capabilities.put(AutomationConstants.UUID,uuid);
        AutomationRunRequest request = new AutomationRunRequest(uuid,10,"chrome");
        runContext.addRun(request);
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        testSlot.getNewSession(capabilities);
        proxy.setMultipleTestSlots(testSlot,5);
        proxySet.add(proxy);
        int freeThreads = runContext.getTotalThreadsAvailable(proxySet);
        Assert.assertEquals(0,freeThreads);
    }

    @Test
    // Tests that for an old run, the in progress tests are counted
    public void testOldRunInProgress() {
        String uuid = "testUuid";
        AutomationRunContext runContext = new AutomationRunContext();
        runContext.setTotalNodeCount(10);
        ProxySet proxySet = new ProxySet(false);
        MockRemoteProxy proxy = new MockRemoteProxy();
        proxy.setCapabilityMatcher(new AutomationCapabilityMatcher());
        Map<String,Object> capabilities = new HashMap<>();
        capabilities.put(CapabilityType.PLATFORM,"linux");
        capabilities.put(CapabilityType.BROWSER_NAME,"chrome");
        capabilities.put(AutomationConstants.UUID,uuid);
        AutomationRunRequest request = new AutomationRunRequest(uuid,10,"chrome","23","linux", AutomationUtils.modifyDate(new Date(),-5,Calendar.MINUTE));
        runContext.addRun(request);
        TestSlot testSlot = new TestSlot(proxy, SeleniumProtocol.WebDriver,null,capabilities);
        testSlot.getNewSession(capabilities);
        int inProgressTests = 5;
        proxy.setMultipleTestSlots(testSlot,inProgressTests);
        proxySet.add(proxy);
        int freeThreads = runContext.getTotalThreadsAvailable(proxySet);
        Assert.assertEquals("Free threads should reflect in progress test count",inProgressTests,freeThreads);
    }
}
