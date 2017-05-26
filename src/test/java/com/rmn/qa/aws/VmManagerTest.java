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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.Platform;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.rmn.qa.AutomationConstants;
import com.rmn.qa.BaseTest;
import com.rmn.qa.NodesCouldNotBeStartedException;

import junit.framework.Assert;

public class VmManagerTest extends BaseTest {

    @After
    public void clear() {
        System.clearProperty(AutomationConstants.AWS_ACCESS_KEY);
        System.clearProperty(AutomationConstants.AWS_PRIVATE_KEY);
        System.clearProperty("propertyFileLocation");
    }

    @Test
    // Test if an AWS access key is not set, the appropriate exception if thrown
    public void testAccessKeyNotSet() {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        Properties properties = new Properties();
        String region = "east";
        AwsVmManager AwsVmManager = new AwsVmManager(client,properties,region);
        try{
            AwsVmManager.getCredentials();
        } catch(IllegalArgumentException e) {
            Assert.assertTrue("Message should be related to access key", e.getMessage().contains(AutomationConstants.AWS_ACCESS_KEY));
            return;
        }
        Assert.fail("Exception should have been throw for access key");
    }

    @Test
    // Test if an AWS private key is not set, the appropriate exception if thrown
    public void testPrivateKeyNotSet() {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        Properties properties = new Properties();
        properties.setProperty(AutomationConstants.AWS_ACCESS_KEY,"foo");
        String region = "east";
        AwsVmManager AwsVmManager = new AwsVmManager(client,properties,region);
        try{
            AwsVmManager.getCredentials();
        } catch(IllegalArgumentException e) {
            Assert.assertTrue("Message should be related to access key: " + e.getMessage(), e.getMessage().contains(AutomationConstants.AWS_PRIVATE_KEY));
            return;
        }
        Assert.fail("Exception should have been throw for access key");
    }



    @Test
    // Test if the AWS access and private key are set, everything works
    public void testKeysSet() {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        Properties properties = new Properties();
        String accessKey = "foo",privateKey = "bar";

        properties.setProperty(AutomationConstants.AWS_ACCESS_KEY,accessKey);
        properties.setProperty(AutomationConstants.AWS_PRIVATE_KEY,privateKey);
        String region = "east";
        AwsVmManager AwsVmManager = new AwsVmManager(client,properties,region);
        AWSCredentials credentials = AwsVmManager.getCredentials();
        Assert.assertEquals("Access key IDs should match",accessKey,credentials.getAWSAccessKeyId());
        Assert.assertEquals("Access key IDs should match",privateKey,credentials.getAWSSecretKey());
    }

    @Test
    // Test if System property access/private keys have priority over the property value ones
    public void testSystemPropertyPrecedence() {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        Properties properties = new Properties();
        String accessKey = "foo",privateKey = "bar";
        System.setProperty(AutomationConstants.AWS_ACCESS_KEY, accessKey);
        System.setProperty(AutomationConstants.AWS_PRIVATE_KEY, privateKey);
        properties.setProperty(AutomationConstants.AWS_ACCESS_KEY, "gibberish");
        properties.setProperty(AutomationConstants.AWS_PRIVATE_KEY,"moreGibberish");
        String region = "east";
        AwsVmManager AwsVmManager = new com.rmn.qa.aws.AwsVmManager(client,properties,region);
        AWSCredentials credentials = AwsVmManager.getCredentials();
        Assert.assertEquals("Access key IDs should match", accessKey, credentials.getAWSAccessKeyId());
        Assert.assertEquals("Access key IDs should match", privateKey, credentials.getAWSSecretKey());
    }

    @Test
    // Happy path test flow for launching nodes
    public void testLaunchNodes() throws NodesCouldNotBeStartedException{
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        RunInstancesResult runInstancesResult = new RunInstancesResult();
        Reservation reservation = new Reservation();
        reservation.setInstances(Arrays.asList(new Instance()));
        runInstancesResult.setReservation(reservation);
        client.setRunInstances(runInstancesResult);
        Properties properties = new Properties();
        String region = "east", uuid="uuid",browser="chrome";
        Platform os = Platform.WINDOWS;
        Integer threadCount = 5,maxSessions=5;
        MockManageVm manageEC2 = new MockManageVm(client,properties,region);
        String userData = "userData";
        manageEC2.setUserData(userData);
        manageEC2.launchNodes(uuid,os,browser,null,threadCount,maxSessions);
        RunInstancesRequest request = client.getRunInstancesRequest();
        Assert.assertEquals("Min count should match thread count requested", threadCount, request.getMinCount());
        Assert.assertEquals("Max count should match thread count requested", threadCount, request.getMaxCount());
        Assert.assertEquals("User data should match", userData, request.getUserData());
        Assert.assertTrue("No security group should be set", request.getSecurityGroupIds().isEmpty());
        Assert.assertNull("No subnet should be set", request.getSubnetId());
        Assert.assertNull("No key name should be set", request.getKeyName());
    }

    @Test
    // Test the optional fields for launching a node are indeed optional
    public void testLaunchNodesOptionalFieldsSet()  throws NodesCouldNotBeStartedException {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        RunInstancesResult runInstancesResult = new RunInstancesResult();
        Reservation reservation = new Reservation();
        reservation.setInstances(Arrays.asList(new Instance()));
        runInstancesResult.setReservation(reservation);
        client.setRunInstances(runInstancesResult);
        Properties properties = new Properties();
        String region = "east", uuid="uuid",browser="chrome";
        Platform os = null;
        Integer threadCount = 5,maxSessions=5;
        MockManageVm manageEC2 = new MockManageVm(client,properties,region);
        String userData = "userData";
        String securityGroup="securityGroup",subnetId="subnetId",keyName="keyName",linuxImage="linuxImage";
        properties.setProperty(region + "_security_group",securityGroup);
        properties.setProperty(region + "_subnet_id",subnetId);
        properties.setProperty(region + "_key_name", keyName);
        properties.setProperty(region + "_linux_node_ami", linuxImage);
        manageEC2.setUserData(userData);
        manageEC2.launchNodes(uuid,os,browser,null,threadCount,maxSessions);
        RunInstancesRequest request = client.getRunInstancesRequest();
        Assert.assertEquals("Min count should match thread count requested",threadCount,request.getMinCount());
        Assert.assertEquals("Max count should match thread count requested",threadCount,request.getMaxCount());
        Assert.assertEquals("User data should match",userData,request.getUserData());
        Assert.assertEquals("Image id should match",linuxImage,request.getImageId());
        List<String> securityGroups = request.getSecurityGroupIds();
        Assert.assertEquals("Only one security group should be set",1,securityGroups.size());
        Assert.assertEquals("Only one security group should be set", securityGroup, securityGroups.get(0));
        Assert.assertEquals("Subnet ids should match", subnetId, request.getSubnetId());
        Assert.assertEquals("Key names should match", keyName, request.getKeyName());
    }

    @Test
    // Test if multiple security groups can be passed when launching a node
    public void testLaunchNodesMultipleSecurityGroups()  throws NodesCouldNotBeStartedException {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        RunInstancesResult runInstancesResult = new RunInstancesResult();
        Reservation reservation = new Reservation();
        reservation.setInstances(Arrays.asList(new Instance()));
        runInstancesResult.setReservation(reservation);
        client.setRunInstances(runInstancesResult);
        Properties properties = new Properties();
        String region = "east", uuid="uuid",browser="chrome";
        Platform os = Platform.ANY;
        Integer threadCount = 5,maxSessions=5;
        MockManageVm manageEC2 = new MockManageVm(client,properties,region);
        String userData = "userData";
        String securityGroup="securityGroup1,securityGroup2,securityGroup3",subnetId="subnetId",keyName="keyName",linuxImage="linuxImage";
        String[] splitSecurityGroupdIds = securityGroup.split(",");
        List securityGroupIdsAryLst = new ArrayList();

        if (securityGroup != null) {
            for (int i = 0; i < splitSecurityGroupdIds.length; i++) {
                securityGroupIdsAryLst.add(splitSecurityGroupdIds[i]);
            }
        }
        properties.setProperty(region + "_security_group",securityGroup);
        properties.setProperty(region + "_subnet_id",subnetId);
        properties.setProperty(region + "_key_name", keyName);
        properties.setProperty(region + "_linux_node_ami", linuxImage);
        manageEC2.setUserData(userData);
        manageEC2.launchNodes(uuid,os,browser,null,threadCount,maxSessions);
        RunInstancesRequest request = client.getRunInstancesRequest();
        request.setSecurityGroupIds(securityGroupIdsAryLst);
        List<String> securityGroups = request.getSecurityGroupIds();
        List<String> expectedSecurityGroups = Arrays.asList("securityGroup1,securityGroup2,securityGroup3".split(","));
        Assert.assertTrue("Security groups should match all given security groups", securityGroups.containsAll(expectedSecurityGroups));

        List<String> invalidSecurityGroups = Arrays.asList("securityGroup1,securityGroup2,securityGroup7".split(","));
        Assert.assertFalse("Security groups should match only the set security groups", securityGroups.containsAll(invalidSecurityGroups));

        Assert.assertFalse("Security group should not be empty", request.getSecurityGroupIds().isEmpty());
        Assert.assertEquals("More than 1 security group should be set",3,securityGroups.size());
    }

    @Test
    // Test launching an IE node works correctly
    public void testLaunchNodesIe()  throws NodesCouldNotBeStartedException {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        RunInstancesResult runInstancesResult = new RunInstancesResult();
        Reservation reservation = new Reservation();
        reservation.setInstances(Arrays.asList(new Instance()));
        runInstancesResult.setReservation(reservation);
        client.setRunInstances(runInstancesResult);
        Properties properties = new Properties();
        String region = "east", uuid="uuid",browser="internet explorer";
        Platform os = null;
        Integer threadCount = 5,maxSessions=5;
        MockManageVm manageEC2 = new MockManageVm(client,properties,region);
        String userData = "userData";
        String securityGroup="securityGroup",subnetId="subnetId",keyName="keyName",windowsImage="windowsImage";
        properties.setProperty(region + "_security_group",securityGroup);
        properties.setProperty(region + "_subnet_id",subnetId);
        properties.setProperty(region + "_key_name", keyName);
        properties.setProperty(region + "_windows_node_ami", windowsImage);
        manageEC2.setUserData(userData);
        manageEC2.launchNodes(uuid,os,browser,null,threadCount,maxSessions);
        RunInstancesRequest request = client.getRunInstancesRequest();
        Assert.assertEquals("Min count should match thread count requested",threadCount,request.getMinCount());
        Assert.assertEquals("Max count should match thread count requested",threadCount,request.getMaxCount());
        Assert.assertEquals("User data should match",userData,request.getUserData());
        Assert.assertEquals("Image id should match",windowsImage,request.getImageId());
        List<String> securityGroups = request.getSecurityGroupIds();
        Assert.assertEquals("Only one security group should be set",1,securityGroups.size());
        Assert.assertEquals("Only one security group should be set", securityGroup, securityGroups.get(0));
        Assert.assertEquals("Subnet ids should match", subnetId, request.getSubnetId());
        Assert.assertEquals("Key names should match", keyName, request.getKeyName());
    }

    @Test
    // Test if a bad OS is specified, it is handled correctly
    public void testLaunchNodesBadOs()  throws NodesCouldNotBeStartedException{
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        RunInstancesResult runInstancesResult = new RunInstancesResult();
        Reservation reservation = new Reservation();
        reservation.setInstances(Arrays.asList(new Instance()));
        runInstancesResult.setReservation(reservation);
        client.setRunInstances(runInstancesResult);
        Properties properties = new Properties();
        String region = "east", uuid="uuid",browser="chrome";
        Platform os = Platform.MAC;
        Integer threadCount = 5,maxSessions=5;
        MockManageVm manageEC2 = new MockManageVm(client,properties,region);
        String userData = "userData";
        String securityGroup="securityGroup",subnetId="subnetId",keyName="keyName",windowsImage="windowsImage";
        properties.setProperty(region + "_security_group",securityGroup);
        properties.setProperty(region + "_subnet_id", subnetId);
        properties.setProperty(region + "_key_name", keyName);
        properties.setProperty(region + "_windows_node_ami", windowsImage);
        manageEC2.setUserData(userData);
        try{
            manageEC2.launchNodes(uuid,os,browser,null,threadCount,maxSessions);
        } catch(RuntimeException e) {
            Assert.assertTrue("Failure message should be correct",e.getMessage().contains(os.toString()));
            return;
        }
        Assert.fail("Call should fail due to invalid OS");
    }

    @Test
    // Test terminating instances works correctly
    public void testTerminateInstance() {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        String instanceId="foo";
        TerminateInstancesResult terminateInstancesResult = new TerminateInstancesResult();
        client.setTerminateInstancesResult(terminateInstancesResult);
        InstanceStateChange stateChange = new InstanceStateChange();
        stateChange.withInstanceId(instanceId);
        stateChange.setCurrentState(new InstanceState().withCode(32));
        terminateInstancesResult.setTerminatingInstances(Arrays.asList(stateChange));
        Properties properties = new Properties();
        String region = "east";
        MockManageVm manageEC2 = new MockManageVm(client,properties,region);

        boolean success = manageEC2.terminateInstance(instanceId);
        TerminateInstancesRequest request = client.getTerminateInstancesRequest();
        Assert.assertEquals("Instance id size should match", 1, request.getInstanceIds().size());
        Assert.assertEquals("Instance ids should match", instanceId, request.getInstanceIds().get(0));
        Assert.assertTrue("Termination call should have been successful", success);
    }

    @Test
    // Tests terminating an invalid instance is handled correctly
    public void testTerminateInstanceInvalidRunningCode() {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        String instanceId="foo";
        TerminateInstancesResult terminateInstancesResult = new TerminateInstancesResult();
        client.setTerminateInstancesResult(terminateInstancesResult);
        InstanceStateChange stateChange = new InstanceStateChange();
        stateChange.withInstanceId(instanceId);
        stateChange.setCurrentState(new InstanceState().withCode(8));
        terminateInstancesResult.setTerminatingInstances(Arrays.asList(stateChange));
        Properties properties = new Properties();
        String region = "east";
        MockManageVm manageEC2 = new MockManageVm(client,properties,region);

        boolean success = manageEC2.terminateInstance(instanceId);
        TerminateInstancesRequest request = client.getTerminateInstancesRequest();
        Assert.assertEquals("Instance id size should match", 1, request.getInstanceIds().size());
        Assert.assertEquals("Instance ids should match", instanceId, request.getInstanceIds().get(0));
        Assert.assertFalse("Termination call should have not been successful", success);
    }

    @Test
    // Test terminating a valid but not matching instances is handled correctly
    public void testTerminateInstanceNoMatchingInstance() {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        String instanceId="foo";
        TerminateInstancesResult terminateInstancesResult = new TerminateInstancesResult();
        client.setTerminateInstancesResult(terminateInstancesResult);
        InstanceStateChange stateChange = new InstanceStateChange();
        stateChange.withInstanceId("notMatching");
        stateChange.setCurrentState(new InstanceState().withCode(8));
        terminateInstancesResult.setTerminatingInstances(Arrays.asList(stateChange));
        Properties properties = new Properties();
        String region = "east";
        MockManageVm manageEC2 = new MockManageVm(client,properties,region);

        boolean success = manageEC2.terminateInstance(instanceId);
        TerminateInstancesRequest request = client.getTerminateInstancesRequest();
        Assert.assertEquals("Instance id size should match", 1, request.getInstanceIds().size());
        Assert.assertEquals("Instance ids should match", instanceId, request.getInstanceIds().get(0));
        Assert.assertFalse("Termination call should have not been successful", success);
    }

    @Test
    // Tests that the terminate code works when no matching results are returned by the client
    public void testTerminateInstanceNoInstanceEmpty() {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        String instanceId="foo";
        TerminateInstancesResult terminateInstancesResult = new TerminateInstancesResult();
        client.setTerminateInstancesResult(terminateInstancesResult);
        terminateInstancesResult.setTerminatingInstances(Collections.unmodifiableList(new ArrayList<InstanceStateChange>(0)));
        Properties properties = new Properties();
        String region = "east";
        MockManageVm manageEC2 = new MockManageVm(client,properties,region);

        boolean success = manageEC2.terminateInstance(instanceId);
        TerminateInstancesRequest request = client.getTerminateInstancesRequest();
        Assert.assertEquals("Instance id size should match",1,request.getInstanceIds().size());
        Assert.assertEquals("Instance ids should match", instanceId, request.getInstanceIds().get(0));
        Assert.assertFalse("Termination call should have not been successful", success);
    }

    @Test
    // Tests that the s3 config file gets injected with the correct values
    public void testS3Config() {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        Properties properties = new Properties();
        String accessKey = "foo",privateKey = "bar";

        properties.setProperty(AutomationConstants.AWS_ACCESS_KEY, accessKey);
        properties.setProperty(AutomationConstants.AWS_PRIVATE_KEY, privateKey);

        String region = "east";
        MockManageVm manageEC2 = new MockManageVm(client,properties,region);
        AWSCredentials credentials = manageEC2.getCredentials();
        manageEC2.setCredentials(credentials);
        String s3Config = manageEC2.getS3Config();
        Assert.assertTrue("Access key should have been injected into the file", s3Config.contains(accessKey));
        Assert.assertTrue("Private key should have been injected into the file", s3Config.contains(privateKey));
    }

    @Test
    // Test that you can initialize the AWS properties when the system properties have been set
    public void testInitProperties() {
        String accessKey = "foo",privateKey = "bar";

        System.setProperty(AutomationConstants.AWS_ACCESS_KEY,accessKey);
        System.setProperty(AutomationConstants.AWS_PRIVATE_KEY,privateKey);
        MockManageVm manageEC2 = new MockManageVm();
    }

    @Test
    // Test that you can initialize the AWS properties from a custom properties file
    public void testInitPropertiesFromSystemProperties() {
        String path = "src/main/resources/" + AutomationConstants.AWS_DEFAULT_RESOURCE_NAME;
        path.replace("/", File.separator);
        System.setProperty("propertyFileLocation",path);
        MockManageVm manageEC2 = new MockManageVm(null,null,null);
        Properties retrievedProperties = manageEC2.initAWSProperties();
        File f = new File(path);
        Properties compareProperties = new Properties();
        try {
            InputStream is = new FileInputStream(f);
            compareProperties.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Could not load custom aws.properties", e);
        }
        Assert.assertEquals("Properties should be equal",compareProperties,retrievedProperties);
    }

    @Test
    // Test that an incorrectly set properties file throws the appropriate exception
    public void testInvalidCustomProperty() {
        System.setProperty("propertyFileLocation","bogus");
        MockManageVm manageEC2 = new MockManageVm(null,null,null);
        try{

            manageEC2.initAWSProperties();
        } catch(RuntimeException e) {
            Assert.assertEquals("Could not load custom aws.properties",e.getMessage());
            return;
        }
        Assert.fail("Non loading properties should throw exception");
    }

    @Test
    // Test that the correct node config file is generated for a windows os
    public void testGetNodeConfigWindows() {
        MockManageVm manageEC2 = new MockManageVm(null,null,null);
        String uuid="uuid",hostName="hostName",browser="chrome";
        Platform os = Platform.WINDOWS;
        int maxSessions = 5;
        String nodeConfig = manageEC2.getNodeConfig(uuid,hostName,browser,os,maxSessions);
        Assert.assertTrue("Max sessions should have been passed in",nodeConfig.contains(String.valueOf(maxSessions)));
        Assert.assertTrue("UUID should have been passed in",nodeConfig.contains(uuid));
        Assert.assertTrue("Browser should have been passed in",nodeConfig.contains(browser));
        Assert.assertTrue("OS should have been passed in",nodeConfig.contains(os.toString()));
        Assert.assertTrue("Host name should have been passed in",nodeConfig.contains(hostName));
        Assert.assertTrue("IE thread count should have been passed in", nodeConfig.contains(String.valueOf(AwsVmManager.FIREFOX_IE_THREAD_COUNT)));
    }

    @Test
    // Test that the correct node config file is generated for a linux os
    public void testGetNodeConfigLinux() {
        MockManageVm manageEC2 = new MockManageVm(null,null,null);
        String uuid="uuid",hostName="hostName",browser="chrome";
        Platform os = Platform.LINUX;
        int maxSessions = 5;
        String nodeConfig = manageEC2.getNodeConfig(uuid,hostName,browser,os,maxSessions);
        Assert.assertTrue("Max sessions should have been passed in",nodeConfig.contains(String.valueOf(maxSessions)));
        Assert.assertTrue("UUID should have been passed in",nodeConfig.contains(uuid));
        Assert.assertTrue("Browser should have been passed in",nodeConfig.contains(browser));
        Assert.assertTrue("OS should have been passed in",nodeConfig.contains(os.toString()));
        Assert.assertTrue("Host name should have been passed in",nodeConfig.contains(hostName));
        Assert.assertTrue("IE thread count should have been passed in", nodeConfig.contains(String.valueOf(AwsVmManager.CHROME_THREAD_COUNT)));
    }

    @Test
    // Test that the correct exception is throw when you specified a bad OS for the node config
    public void testGetNodeConfigBadOs() {
        MockManageVm manageEC2 = new MockManageVm(null,null,null);
        String uuid="uuid",hostName="hostName",browser="chrome";
        Platform os = Platform.MAC;
        int maxSessions = 5;
        try{
            manageEC2.getNodeConfig(uuid,hostName,browser,os,maxSessions);
        } catch(RuntimeException e) {
            Assert.assertTrue("Failure message should be correct",e.getMessage().contains(os.toString()));
            return;
        }
        Assert.fail("Node config should not work due to bad OS");
    }

    @Test
    // Tests that if no fallback subnets are specified, the correct exception is thrown
    public void testSubnetNoFallBack() throws NodesCouldNotBeStartedException {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        AmazonServiceException exception = new AmazonServiceException("message");
        exception.setErrorCode("InsufficientInstanceCapacity");
        client.setThrowDescribeInstancesError(exception);
        RunInstancesResult runInstancesResult = new RunInstancesResult();
        Reservation reservation = new Reservation();
        reservation.setInstances(Arrays.asList(new Instance()));
        runInstancesResult.setReservation(reservation);
        client.setRunInstances(runInstancesResult);
        Properties properties = new Properties();
        String region = "east", uuid="uuid",browser="chrome";
        Platform os = Platform.LINUX;
        Integer threadCount = 5,maxSessions=5;
        MockManageVm manageEC2 = new MockManageVm(client,properties,region);
        String userData = "userData";
        String securityGroup="securityGroup",subnetId="subnetId",keyName="keyName",windowsImage="windowsImage";
        properties.setProperty(region + "_security_group",securityGroup);
        properties.setProperty(region + "_subnet_id", subnetId);
        properties.setProperty(region + "_key_name", keyName);
        properties.setProperty(region + "_windows_node_ami", windowsImage);
        manageEC2.setUserData(userData);
        try{
            manageEC2.launchNodes(uuid,os,browser,null,threadCount,maxSessions);
        } catch(NodesCouldNotBeStartedException e) {
            Assert.assertTrue("Failure message should be correct",e.getMessage().contains("Sufficient resources were not available in any of the availability zones"));
            return;
        }
        Assert.fail("Call should fail due to insufficient resources");
    }

    @Test
    // Test that if a fallback subnet is specified, that the request for new nodes will fallback successfully and nodes will be spun up
    public void testSubnetFallsBackSuccessfully() throws NodesCouldNotBeStartedException {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        AmazonServiceException exception = new AmazonServiceException("message");
        exception.setErrorCode("InsufficientInstanceCapacity");
        client.setThrowDescribeInstancesError(exception);
        RunInstancesResult runInstancesResult = new RunInstancesResult();
        Reservation reservation = new Reservation();
        reservation.setInstances(Arrays.asList(new Instance()));
        runInstancesResult.setReservation(reservation);
        client.setRunInstances(runInstancesResult);
        Properties properties = new Properties();
        String region = "east", uuid="uuid",browser="chrome";
        Platform os = Platform.LINUX;
        Integer threadCount = 5,maxSessions=5;
        MockManageVm manageEC2 = new MockManageVm(client,properties,region);
        String userData = "userData";
        String securityGroup="securityGroup",subnetId="subnetId",keyName="keyName",windowsImage="windowsImage",fallBackSubnet="fallback";
        properties.setProperty(region + "_security_group",securityGroup);
        properties.setProperty(region + "_subnet_id", subnetId);
        properties.setProperty(region + "_subnet_fallback_id_1", fallBackSubnet);
        properties.setProperty(region + "_key_name", keyName);
        properties.setProperty(region + "_windows_node_ami", windowsImage);
        manageEC2.setUserData(userData);
        List<Instance> instances = manageEC2.launchNodes(uuid,os,browser,null,threadCount,maxSessions);
        System.out.print("");
    }

    @Test
    // Tests that if the client fails for an error other than insufficient capacity, subnet fallback logic is not performed
    public void testSubnetFallBackUnknownError() throws NodesCouldNotBeStartedException {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        AmazonServiceException exception = new AmazonServiceException("message");
        client.setThrowDescribeInstancesError(exception);
        RunInstancesResult runInstancesResult = new RunInstancesResult();
        Reservation reservation = new Reservation();
        reservation.setInstances(Arrays.asList(new Instance()));
        runInstancesResult.setReservation(reservation);
        client.setRunInstances(runInstancesResult);
        Properties properties = new Properties();
        String region = "east", uuid="uuid",browser="chrome";
        Platform os = Platform.LINUX;
        Integer threadCount = 5,maxSessions=5;
        MockManageVm manageEC2 = new MockManageVm(client,properties,region);
        String userData = "userData";
        String securityGroup="securityGroup",subnetId="subnetId",keyName="keyName",windowsImage="windowsImage";
        properties.setProperty(region + "_security_group",securityGroup);
        properties.setProperty(region + "_subnet_id", subnetId);
        properties.setProperty(region + "_key_name", keyName);
        properties.setProperty(region + "_windows_node_ami", windowsImage);
        manageEC2.setUserData(userData);
        try{
            manageEC2.launchNodes(uuid,os,browser,null,threadCount,maxSessions);
        } catch(AmazonServiceException e) {
            Assert.assertEquals("Exception should be the same",exception,e);
            return;
        }
        Assert.fail("Call should fail due to other AWS error");
    }

    @Test
    // Tests that the built in guard against an infinite loop in the fallback recursive logic has a working safeguard
    public void testSubnetInfiniteLoop() throws NodesCouldNotBeStartedException {
        MockAmazonEc2Client client = new MockAmazonEc2Client(null);
        client.setThrowExceptionsInRunInstancesIndefinitely();
        AmazonServiceException exception = new AmazonServiceException("message");
        exception.setErrorCode("InsufficientInstanceCapacity");
        client.setThrowDescribeInstancesError(exception);
        RunInstancesResult runInstancesResult = new RunInstancesResult();
        Reservation reservation = new Reservation();
        reservation.setInstances(Arrays.asList(new Instance()));
        runInstancesResult.setReservation(reservation);
        client.setRunInstances(runInstancesResult);
        Properties properties = new Properties();
        String region = "east", uuid="uuid",browser="chrome";
        Platform os = Platform.LINUX;
        Integer threadCount = 5,maxSessions=5;
        MockManageVm manageEC2 = new MockManageVm(client,properties,region);
        String userData = "userData";
        String securityGroup="securityGroup",subnetId="subnetId",keyName="keyName",windowsImage="windowsImage";
        properties.setProperty(region + "_security_group",securityGroup);
        properties.setProperty(region + "_subnet_id", subnetId);
        properties.setProperty(region + "_subnet_fallback_id_1", "foo");
        properties.setProperty(region + "_subnet_fallback_id_2", "foo");
        properties.setProperty(region + "_subnet_fallback_id_3", "foo");
        properties.setProperty(region + "_subnet_fallback_id_4", "foo");
        properties.setProperty(region + "_subnet_fallback_id_5", "foo");
        properties.setProperty(region + "_subnet_fallback_id_6", "foo");
        properties.setProperty(region + "_key_name", keyName);
        properties.setProperty(region + "_windows_node_ami", windowsImage);
        manageEC2.setUserData(userData);
        try{
            manageEC2.launchNodes(uuid,os,browser,null,threadCount,maxSessions);
        } catch(NodesCouldNotBeStartedException e) {
            Assert.assertTrue("Failure message should be correct",e.getMessage().contains("Sufficient resources were not available in any of the availability zones"));
            return;
        }
        Assert.fail("Call should fail due to insufficient resources");
    }

    @Test
    //Tests that if the client is not initialized, an exception with appropriate message is thrown
    public void testClientInitialized(){
        AwsVmManager manageEC2 = new AwsVmManager();
        try{
            manageEC2.launchNodes("foo", "bar", 4, "userData", false);
        } catch(Exception e) {
            Assert.assertFalse("The client should be initialized",e.getMessage().contains("The client is not initialized"));
        }
    }

    @Test
    //Tests that if the client is not initialized, an exception with appropriate message is thrown
    public void testClientNotInitializedError(){
        String accessKey = "foo",privateKey = "bar";
        Properties properties = new Properties();
        properties.setProperty(AutomationConstants.AWS_ACCESS_KEY,accessKey);
        properties.setProperty(AutomationConstants.AWS_PRIVATE_KEY,privateKey);
        String region = "east";

        AwsVmManager manageEC2 = new AwsVmManager(null,properties,region);

        try{
            manageEC2.launchNodes("foo", "bar", 3, "userData", false);
        } catch(Exception e) {
            Assert.assertTrue("The client should be initialized", e.getMessage().contains("The client is not initialized"));
            return;
        }
        Assert.fail("Exception should have been thrown for client not initialized");
    }

}
