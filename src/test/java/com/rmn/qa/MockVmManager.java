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

import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.Platform;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.rmn.qa.aws.VmManager;

public class MockVmManager implements VmManager {

    private boolean nodesLaunched = false;
    private int numberLaunched;
    private String browser;
    private boolean throwException = false;
    private boolean terminated = false;
    private List<Reservation> reservations;


    @Override
    public List<Instance> launchNodes(String uuid, Platform platform, String browser, String hubHostName, int nodeCount, int maxSessions) {
        if(throwException) {
            throw new RuntimeException("Can't start nodes");
        }
        this.nodesLaunched = true;
        this.numberLaunched = nodeCount;
        this.browser = browser;
        Instance instance = new Instance();
        instance.setInstanceId("instanceId");
        return Arrays.asList(instance);
    }

    @Override
    public boolean terminateInstance(String instanceId) {
        terminated = true;
        return true;
    }

    @Override
    public List<Reservation> describeInstances(DescribeInstancesRequest describeInstancesRequest) {
        return reservations;
    }

    public boolean isNodesLaunched() {
        return nodesLaunched;
    }

    public String getBrowser() {
        return browser;
    }

    public int getNumberLaunched() {
        return numberLaunched;
    }

    public void setThrowException() {
        throwException = true;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }
}
