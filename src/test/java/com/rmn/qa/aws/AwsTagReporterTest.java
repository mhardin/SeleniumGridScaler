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

package com.rmn.qa.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.rmn.qa.BaseTest;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AwsTagReporterTest extends BaseTest {

    AwsTagReporter reporter = mock(AwsTagReporter.class);

    @Test
    public void testTagsAssociated() {
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();
        Reservation reservation = new Reservation();
        describeInstancesResult.setReservations(Arrays.asList(reservation));

        Properties properties = new Properties();
        properties.setProperty("tagAccounting","key,value");
        properties.setProperty("function_tag","foo2");
        properties.setProperty("product_tag","foo3");

        when(reporter.getExistingInstances()).thenReturn(describeInstancesResult);
        reporter.run();
    }

    @Test
    public void testExceptionCaught() {
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();
        Reservation reservation = new Reservation();
        Collection<Instance> instances = Arrays.asList(new Instance());
        reservation.setInstances(instances);
        describeInstancesResult.setReservations(Arrays.asList(reservation));

        Properties properties = new Properties();
        properties.setProperty("tagAccounting","key");
        properties.setProperty("function_tag","foo2");
        properties.setProperty("product_tag","foo3");

        when(reporter.getExistingInstances()).thenReturn(describeInstancesResult);
        reporter.run();
    }

    @Test
    public void testClientThrowsErrors() {
        Collection<Instance> instances = Arrays.asList(new Instance());
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();
        Reservation reservation = new Reservation();
        describeInstancesResult.setReservations(Arrays.asList(reservation));
        reservation.setInstances(instances);

        Properties properties = new Properties();
        properties.setProperty("accounting_tag","foo");
        properties.setProperty("function_tag","foo2");
        properties.setProperty("product_tag","foo3");

        when(reporter.getExistingInstances()).thenThrow(new AmazonClientException("Test error"));
        reporter.run();
    }

    @Test
    public void testSleepThrowsErrors() throws InterruptedException {
        Collection<Instance> instances = Arrays.asList(new Instance());
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();
        Reservation reservation = new Reservation();
        describeInstancesResult.setReservations(Arrays.asList(reservation));
        reservation.setInstances(instances);

        Properties properties = new Properties();
        properties.setProperty("accounting_tag","foo");
        properties.setProperty("function_tag","foo2");
        properties.setProperty("product_tag","foo3");

        when(reporter.getExistingInstances()).thenReturn(describeInstancesResult);
        doThrow(new InterruptedException()).when(reporter).sleep();
        reporter.run();
    }

    @Test
    public void testThreadTimesOut() {
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();
        Reservation reservation = new Reservation();
        describeInstancesResult.setReservations(Arrays.asList(reservation));
        // Make count mismatch
        reservation.setInstances(Arrays.asList(new Instance(),new Instance()));

        Properties properties = new Properties();
        properties.setProperty("accounting_tag","foo");
        properties.setProperty("function_tag","foo2");
        properties.setProperty("product_tag","foo3");

        doThrow(new RuntimeException("Error waiting for instances to exist to add tags")).when(reporter).run();
        AwsTagReporter.TIMEOUT_IN_SECONDS = 1;
        try{
            reporter.run();
        } catch(RuntimeException e) {
            Assert.assertEquals("Error waiting for instances to exist to add tags",e.getMessage());
            return;
        }
        Assert.fail("Exception should have been thrown since tags were never filed");
    }
}