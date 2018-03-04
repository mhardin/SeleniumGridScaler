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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.annotations.VisibleForTesting;

public class AwsTagReporter extends Thread {

    private static final Logger log = LoggerFactory.getLogger(AwsTagReporter.class);
    static int TIMEOUT_IN_SECONDS = 10 * 1000;

    private AmazonEC2 ec2Client;
    private Collection<Instance> instances;
    private Properties awsProperties;

    public AwsTagReporter(String testRunUuid, AmazonEC2 ec2Client, Collection<Instance> instances, Properties awsProperties) {
        this.ec2Client = ec2Client;
        this.instances = instances;
        this.awsProperties = awsProperties;
        this.setName("TagReporter-" + testRunUuid);
    }

    @Override
    public void run() {
        log.info("AwsTagReporter thread initialized");
        long startTime = System.currentTimeMillis();
        boolean instancesFound = false;
        do{
            // Wait up to 10 seconds for the instances to exist with AWS
            if(System.currentTimeMillis() > startTime + AwsTagReporter.TIMEOUT_IN_SECONDS) {
                throw new RuntimeException("Error waiting for instances to exist to add tags");
            }
            try{
                DescribeInstancesResult existingInstances = getExistingInstances();
                if(existingInstances.getReservations().get(0).getInstances().size() == instances.size()) {
                    log.info("Correct instances were found to add tags to!");
                    instancesFound = true;
                }
            } catch(Throwable t) {
                log.warn("Error finding instances.  Sleeping for 500ms.");
                try {
                    sleep();
                } catch (InterruptedException e) {
                    log.error("Error sleeping for adding tags", e);
                }
            }
        } while(!instancesFound);
        associateTags(instances);
        log.info("AwsTagReporter thread completed successfully");
    }

    @VisibleForTesting
    void sleep() throws InterruptedException{
        Thread.sleep(500);
    }

    /**
     * Associates the correct tags for each instance passed in
     * @param instances
     */
    protected void associateTags(Collection<Instance> instances) {
        try{
            for(Instance instance : instances) {
                log.info("Associating tags to instance: " + instance.getInstanceId());
                String instanceId = instance.getInstanceId();
                setTagsForInstance(instanceId);
            }
            log.info("Tags added!");
        } catch(IndexOutOfBoundsException | ClassCastException | AmazonServiceException e) {
            log.error("Error adding tags.  Please make sure your tag syntax is correct (refer to the readme)",e);
        }
    }

    /**
     * Sets tags for the specified instance
     * @param instanceId
     * @return
     */
    private void setTagsForInstance(String instanceId) {
        Set<Object> keys = awsProperties.keySet();
        List<Tag> tags = new ArrayList<>();
        for(Object o : keys) {
            if(o instanceof String && ((String)o).startsWith("tag")) {
                String values = (String)awsProperties.get(o);
                String[] splitValues = values.split(",");
                String key = splitValues[0];
                String value = splitValues[1];
                Tag tagToAdd = new Tag(key,value);
                log.info("Adding tag: " + tagToAdd);
                tags.add(tagToAdd);
            }
        }
        // Including a hard coded tag here so we can track which resources originate from this plugin
        Tag nodeTag = new Tag("LaunchSource","SeleniumGridScalerPlugin");
        log.info("Adding hard-coded tag: " + nodeTag);
        tags.add(nodeTag);
        CreateTagsRequest ctr = new CreateTagsRequest(Arrays.asList(instanceId),tags);
        ec2Client.createTags(ctr);
    }


    /**
     * Get existing EC2 instances.
     * @return DescribeInstancesResult
     */
    protected DescribeInstancesResult getExistingInstances() {
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        Collection<String> instanceIds = new ArrayList<>();
        for(Instance instance : instances) {
            instanceIds.add(instance.getInstanceId());
        }
        request.withInstanceIds(instanceIds);
        return ec2Client.describeInstances(request);
    }
}
