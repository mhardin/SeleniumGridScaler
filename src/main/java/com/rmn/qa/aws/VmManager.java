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

import java.util.List;

import org.openqa.selenium.Platform;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.rmn.qa.NodesCouldNotBeStartedException;

public interface VmManager {

    /**
     * Launches the specified instances
     * @param uuid UUID of the requesting test run
     * @param platform Platform of the requesting test run
     * @param browser Browser of the requesting test run
     * @param hubHostName Hub host name for the nodes to register with
     * @param nodeCount Number of nodes to be started
     * @param maxSessions Number of max sessions per node
     * @return
     */
    // TODO Refactor into AutomationRunRequest
    List<Instance> launchNodes(String uuid, Platform platform, String browser, String hubHostName, int nodeCount, int maxSessions) throws NodesCouldNotBeStartedException;

    /**
     * Terminates the specified instance
     * @param instanceId
     */
    // TODO Rename to be node or instance in the name
    boolean terminateInstance(String instanceId);

    /**
     * Returns a list of reservations as defined in the {@link com.amazonaws.services.ec2.model.DescribeInstancesRequest DescribeInstancesRequest}
     * @param describeInstancesRequest
     * @return
     */
    List<Reservation> describeInstances(DescribeInstancesRequest describeInstancesRequest);
}
