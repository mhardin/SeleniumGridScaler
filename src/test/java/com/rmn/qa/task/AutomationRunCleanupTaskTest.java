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

import org.junit.Test;
import org.openqa.grid.internal.ProxySet;
import org.openqa.selenium.Platform;

import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationRunContext;
import com.rmn.qa.AutomationRunRequest;
import com.rmn.qa.AutomationUtils;
import com.rmn.qa.BaseTest;
import com.rmn.qa.MockRemoteProxy;

import junit.framework.Assert;

/**
 * Created by mhardin on 5/1/14.
 */
public class AutomationRunCleanupTaskTest extends BaseTest {

    @Test
    // Tests that an old run not in progress is no longer registered
    public void testCleanup() {
        AutomationRunRequest oldRequest = new AutomationRunRequest("uuid",10,"firefox","10", Platform.LINUX, AutomationUtils.modifyDate(new Date(), -1, Calendar.HOUR));
        AutomationRunContext context = AutomationContext.getContext();
        context.addRun(oldRequest);

        Assert.assertTrue("Run should exist", context.hasRun(oldRequest.getUuid()));
        ProxySet proxySet = new ProxySet(false);
        proxySet.add(new MockRemoteProxy());
        context.cleanUpRunRequests(proxySet);
        MockAutomationRunCleanupTask task = new MockAutomationRunCleanupTask(null);
        task.setProxySet(proxySet);
        task.run();
        Assert.assertFalse("Run request should no longer exist as it should have been removed",context.hasRun(oldRequest.getUuid()));
    }

    @Test
    // Tests that the task hard coded name matches the task name
    public void testTaskName() {
        AutomationRunCleanupTask task = new AutomationRunCleanupTask(null);
        Assert.assertEquals("Name should be the same",AutomationRunCleanupTask.NAME, task.getDescription()  );
    }
}
