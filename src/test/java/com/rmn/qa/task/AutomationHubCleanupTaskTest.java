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

import java.util.Calendar;
import java.util.Date;

import org.junit.After;
import org.junit.Test;
import org.openqa.grid.internal.ProxySet;

import com.rmn.qa.AutomationUtils;
import com.rmn.qa.BaseTest;
import com.rmn.qa.MockRemoteProxy;
import com.rmn.qa.MockVmManager;
import com.rmn.qa.aws.AwsVmManager;

import junit.framework.Assert;

public class AutomationHubCleanupTaskTest extends BaseTest {

    @After
    public void afterTest() {
        // Make sure we clear the statically set start and end dates for the hub
        MockAutomationHubCleanupTask.clearDates();
        AutomationHubCleanupTask.errorEncountered = false;
    }

    @Test
    // Tests that the hardcoded name of the task is correct
    public void testTaskName() {
        AutomationHubCleanupTask task = new AutomationHubCleanupTask(null,null,null);
        Assert.assertEquals("Name should be the same",AutomationHubCleanupTask.NAME, task.getDescription()  );
    }

    @Test
    // Tests that the hub terminates after the correct amount of time
    public void testHubTerminated() {
        MockVmManager ec2 = new MockVmManager();
        MockAutomationHubCleanupTask task = new MockAutomationHubCleanupTask(null,ec2,"dummyId");
        ProxySet proxySet = new ProxySet(false);
        task.setProxySet(proxySet);
        String createdDate = AwsVmManager.NODE_DATE_FORMAT.format(AutomationUtils.modifyDate(new Date(), -56, Calendar.MINUTE));
        task.setCreatedDate(createdDate);

        task.run();
        Assert.assertTrue("Hub should be terminated as it was empty",ec2.isTerminated());
    }

    @Test
    // Tests that the cleanup task can handle an incorrectly formatted date (should shut down)
    public void testHubTerminatedBadCreatedDate() {
        MockVmManager ec2 = new MockVmManager();
        MockAutomationHubCleanupTask task = new MockAutomationHubCleanupTask(null,ec2,"dummyId");
        ProxySet proxySet = new ProxySet(false);
        task.setProxySet(proxySet);
        // Set the date to a format that will not be parseable
        String createdDate = "Mon Feb 17 20:56:48 UTC 2014";
        task.setCreatedDate(createdDate);

        task.run();
        Assert.assertTrue("Error should have been encountered",AutomationHubCleanupTask.errorEncountered);
        Assert.assertTrue("Hub should be terminated even though the date format parsing failed",ec2.isTerminated());
    }

    @Test
    // Tests that the error field defaults to false
    public void testHubErrorDefaultsToFalse() {
        Assert.assertFalse("Error not have been configured by default",AutomationHubCleanupTask.errorEncountered);
    }


    @Test
    // Tests that if the hub still has nodes registered to it, it will not terminate
    public void testHubNotTerminatedExistingNodes() {
        MockVmManager ec2 = new MockVmManager();
        MockAutomationHubCleanupTask task = new MockAutomationHubCleanupTask(null,ec2,"dummyId");
        ProxySet proxySet = new ProxySet(false);
        proxySet.add(new MockRemoteProxy());
        task.setProxySet(proxySet);
        String createdDate = AwsVmManager.NODE_DATE_FORMAT.format(AutomationUtils.modifyDate(new Date(),-56, Calendar.MINUTE));
        task.setCreatedDate(createdDate);

        task.run();
        Assert.assertFalse("Hub should not be terminated as nodes are still registered", ec2.isTerminated());
    }

    @Test
    // Tests that if the hub is in the next billing cycle, its end date will reset
    // instead of terminating
    public void testHubNotTerminatedNextBillingCycle() {
        MockVmManager ec2 = new MockVmManager();
        MockAutomationHubCleanupTask task = new MockAutomationHubCleanupTask(null,ec2,"dummyId");
        ProxySet proxySet = new ProxySet(false);
        proxySet.add(new MockRemoteProxy());
        task.setProxySet(proxySet);
        Date newStartDate = AutomationUtils.modifyDate(new Date(),-61, Calendar.MINUTE);
        String createdDate = AwsVmManager.NODE_DATE_FORMAT.format(newStartDate);
        task.setCreatedDate(createdDate);

        task.run();
        Assert.assertFalse("Hub should not be terminated as its in the next billing cycle",ec2.isTerminated());
        Assert.assertTrue("End date should have been increased", AutomationUtils.modifyDate(newStartDate, 116, Calendar.MINUTE).after(task.getEndDate())); // 60 minutes + 55
        Assert.assertTrue("End date should have been increased",AutomationUtils.modifyDate(newStartDate,114,Calendar.MINUTE).before(task.getEndDate())); // 60 minutes + 55
    }
}
