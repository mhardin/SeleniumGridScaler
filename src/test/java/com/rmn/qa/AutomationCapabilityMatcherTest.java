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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.CapabilityType;

import com.google.common.collect.ImmutableMap;

import junit.framework.Assert;

/**
 * Created by mhardin on 4/25/14.
 */
public class AutomationCapabilityMatcherTest extends BaseTest {

    @Test
    public void testMatches() {
        AutomationCapabilityMatcher matcher = new AutomationCapabilityMatcher();
        Map<String,Object> nodeCapability = new HashMap<String,Object>();
        nodeCapability.put(CapabilityType.BROWSER_NAME,"firefox");
        Map<String,Object> testCapability = new HashMap<String,Object>();
        testCapability.put(CapabilityType.BROWSER_NAME,"firefox");
        Assert.assertTrue("Capabilities should match as node is not dynamic",matcher.matches(nodeCapability,testCapability));
    }

    @Test
    public void testNodeNotInContext() {
        AutomationCapabilityMatcher matcher = new AutomationCapabilityMatcher();
        Map<String,Object> nodeCapability = new HashMap<String,Object>();
        nodeCapability.put(CapabilityType.BROWSER_NAME,"firefox");
        nodeCapability.put(AutomationConstants.INSTANCE_ID,"foobar");
        Map<String,Object> testCapability = new HashMap<String,Object>();
        testCapability.put(CapabilityType.BROWSER_NAME,"firefox");
        AutomationDynamicNode node = new AutomationDynamicNode("uuid","foo","browser", Platform.LINUX, new Date(),10);
        node.updateStatus(AutomationDynamicNode.STATUS.EXPIRED);
        Assert.assertTrue("Capabilities should match as node is not in context",matcher.matches(nodeCapability,testCapability));
    }

    @Test
    public void testExpiredNode() {
        AutomationCapabilityMatcher matcher = new AutomationCapabilityMatcher();
        Map<String,Object> nodeCapability = new HashMap<String,Object>();
        nodeCapability.put(CapabilityType.BROWSER_NAME,"firefox");
        nodeCapability.put(AutomationConstants.INSTANCE_ID,"foo");
        Map<String,Object> testCapability = new HashMap<String,Object>();
        testCapability.put(CapabilityType.BROWSER_NAME,"firefox");
        AutomationDynamicNode node = new AutomationDynamicNode("uuid","foo","browser",Platform.LINUX, new Date(),10);
        AutomationContext.getContext().addNode(node);
        node.updateStatus(AutomationDynamicNode.STATUS.EXPIRED);
        Assert.assertFalse("Capabilities should match as node is not in context", matcher.matches(nodeCapability, testCapability));
    }

    @Test
    public void testTerminatedNode() {
        AutomationCapabilityMatcher matcher = new AutomationCapabilityMatcher();
        Map<String,Object> nodeCapability = new HashMap<String,Object>();
        nodeCapability.put(CapabilityType.BROWSER_NAME,"firefox");
        nodeCapability.put(AutomationConstants.INSTANCE_ID,"foo");
        Map<String,Object> testCapability = new HashMap<String,Object>();
        testCapability.put(CapabilityType.BROWSER_NAME,"firefox");
        AutomationDynamicNode node = new AutomationDynamicNode("uuid","foo","browser",Platform.LINUX, new Date(),10);
        AutomationContext.getContext().addNode(node);
        node.updateStatus(AutomationDynamicNode.STATUS.TERMINATED);
        Assert.assertFalse("Capabilities should match as node is not in context",matcher.matches(nodeCapability,testCapability));
    }

    @Test
    public void testSystemPropertyParsed() {
        String soleProperty = "foo";
        System.setProperty(AutomationConstants.EXTRA_CAPABILITIES_PROPERTY_NAME,soleProperty);
        AutomationCapabilityMatcher matcher = new AutomationCapabilityMatcher();
        Assert.assertEquals("Only one property should have been added", 1, matcher.additionalConsiderations.size());
        Assert.assertTrue("Contained property should be correct", matcher.additionalConsiderations.contains(soleProperty));
    }

    @Test
    public void testMultipleSystemPropertiesParsed() {
        String firstProperty = "foo", secondProperty = "bar";
        System.setProperty(AutomationConstants.EXTRA_CAPABILITIES_PROPERTY_NAME,firstProperty + "," + secondProperty);
        AutomationCapabilityMatcher matcher = new AutomationCapabilityMatcher();
        Assert.assertEquals("Only one property should have been added",2,matcher.additionalConsiderations.size());
        Assert.assertTrue("Contained property should be correct", matcher.additionalConsiderations.contains(firstProperty));
        Assert.assertTrue("Contained property should be correct", matcher.additionalConsiderations.contains(secondProperty));
    }

    @Test
    public void testPropertyDoesntMatch() {
        String soleProperty = "foo";
        System.setProperty(AutomationConstants.EXTRA_CAPABILITIES_PROPERTY_NAME,soleProperty);
        AutomationCapabilityMatcher matcher = new AutomationCapabilityMatcher();
        Map<String,Object> nodeCapability = ImmutableMap.of("foo", (Object)"bar");
        Map<String,Object> requestedCapability = ImmutableMap.of("foo", (Object)"doesntMatch");
        Assert.assertFalse("Capabilities should not match due to override", matcher.matches(nodeCapability, requestedCapability));
    }

    @Test
    public void testPropertyNotPresentInCapabilities() {
        String soleProperty = "foo";
        System.setProperty(AutomationConstants.EXTRA_CAPABILITIES_PROPERTY_NAME,soleProperty);
        AutomationCapabilityMatcher matcher = new AutomationCapabilityMatcher();
        Map<String,Object> nodeCapability = ImmutableMap.of("browser", (Object)"bar");
        Map<String,Object> requestedCapability = ImmutableMap.of("browser", (Object)"bar");
        Assert.assertTrue("Capabilities should not match due to override", matcher.matches(nodeCapability, requestedCapability));
    }

    @Test
    public void testPropertyDoesMatch() {
        String soleProperty = "foo";
        System.setProperty(AutomationConstants.EXTRA_CAPABILITIES_PROPERTY_NAME,soleProperty);
        AutomationCapabilityMatcher matcher = new AutomationCapabilityMatcher();
        Map<String,Object> nodeCapability = ImmutableMap.of("foo", (Object)"bar");
        Map<String,Object> requestedCapability = ImmutableMap.of("foo", (Object)"bar");
        Assert.assertTrue("Capabilities should not match due to override",matcher.matches(nodeCapability,requestedCapability));
    }

    @After
    public void clearSystemProperty() {
        System.clearProperty(AutomationConstants.EXTRA_CAPABILITIES_PROPERTY_NAME);
    }
}
