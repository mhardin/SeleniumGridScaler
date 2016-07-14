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

import org.junit.Test;

import com.rmn.qa.task.AbstractAutomationCleanupTask;

import junit.framework.Assert;

/**
 * Created by mhardin on 4/25/14.
 */
public class AbstractAutomationCleanupTaskTest extends BaseTest {

    @Test
    public void testExceptionHandled() {
        TestTask task = new TestTask(null);
        task.run();
        Assert.assertEquals("Correct exception should have been thrown",task.getThrowable(),task.re);
    }

    private static class TestTask extends AbstractAutomationCleanupTask {

        private RuntimeException re = new RuntimeException("test exception");

        public TestTask(RegistryRetriever retriever) {
            super(retriever);
        }

        @Override
        public void doWork() {
            throw re;
        }

        @Override
        public String getDescription() {
            return null;
        }
    }
}
