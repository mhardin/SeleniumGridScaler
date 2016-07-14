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

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import org.junit.Test;

import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.rmn.qa.BaseTest;

import junit.framework.Assert;

public class AwsTagReporterTest extends BaseTest {

    @Test
         public void testTagsAssociated() {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        Collection<Instance> instances = Arrays.asList(new Instance());
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();
        Reservation reservation = new Reservation();
        describeInstancesResult.setReservations(Arrays.asList(reservation));
        reservation.setInstances(instances);
        client.setDescribeInstances(describeInstancesResult);
        Properties properties = new Properties();
        properties.setProperty("tagAccounting","key,value");
        properties.setProperty("function_tag","foo2");
        properties.setProperty("product_tag","foo3");
        AwsTagReporter reporter = new AwsTagReporter("testUuid",client,instances,properties);
        reporter.run();
    }

    @Test
    public void testExceptionCaught() {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        Collection<Instance> instances = Arrays.asList(new Instance());
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();
        Reservation reservation = new Reservation();
        describeInstancesResult.setReservations(Arrays.asList(reservation));
        reservation.setInstances(instances);
        client.setDescribeInstances(describeInstancesResult);
        Properties properties = new Properties();
        properties.setProperty("tagAccounting","key");
        properties.setProperty("function_tag","foo2");
        properties.setProperty("product_tag","foo3");
        AwsTagReporter reporter = new AwsTagReporter("testUuid",client,instances,properties);
        reporter.run();
    }

    @Test
    public void testClientThrowsErrors() {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        client.setDescribeInstancesToThrowError();
        Collection<Instance> instances = Arrays.asList(new Instance());
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();
        Reservation reservation = new Reservation();
        describeInstancesResult.setReservations(Arrays.asList(reservation));
        reservation.setInstances(instances);
        client.setDescribeInstances(describeInstancesResult);
        Properties properties = new Properties();
        properties.setProperty("accounting_tag","foo");
        properties.setProperty("function_tag","foo2");
        properties.setProperty("product_tag","foo3");
        AwsTagReporter reporter = new AwsTagReporter("testUuid",client,instances,properties) {
            @Override
            void sleep() throws InterruptedException {
                // do nothing
            }
        };
        reporter.run();
    }

    @Test
    public void testSleepThrowsErrors() {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        client.setDescribeInstancesToThrowError();
        Collection<Instance> instances = Arrays.asList(new Instance());
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();
        Reservation reservation = new Reservation();
        describeInstancesResult.setReservations(Arrays.asList(reservation));
        reservation.setInstances(instances);
        client.setDescribeInstances(describeInstancesResult);
        Properties properties = new Properties();
        properties.setProperty("accounting_tag","foo");
        properties.setProperty("function_tag","foo2");
        properties.setProperty("product_tag","foo3");
        AwsTagReporter reporter = new AwsTagReporter("testUuid",client,instances,properties) {
            @Override
            void sleep() throws InterruptedException {
                throw new InterruptedException();
            }
        };
        reporter.run();
    }

    @Test()
    public void testThreadTimesOut() {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        Collection<Instance> instances = Arrays.asList(new Instance());
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();
        Reservation reservation = new Reservation();
        describeInstancesResult.setReservations(Arrays.asList(reservation));
        // Make count mismatch
        reservation.setInstances(Arrays.asList(new Instance(),new Instance()));
        client.setDescribeInstances(describeInstancesResult);
        Properties properties = new Properties();
        properties.setProperty("accounting_tag","foo");
        properties.setProperty("function_tag","foo2");
        properties.setProperty("product_tag","foo3");
        AwsTagReporter reporter = new AwsTagReporter("testUuid",client,instances,properties);
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
