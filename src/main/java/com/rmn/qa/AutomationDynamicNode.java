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

import java.util.Calendar;
import java.util.Date;

/**
 * Represents a dynamically started node that is used to run tests
 */
public final class AutomationDynamicNode implements Comparable<AutomationDynamicNode> {

    /** <pre>
     *  RUNNING    - node is running and no further action needs to be taken
     *  EXPIRED    - node has passed its expiration date and needs to be terminated.  A node will not
     *               be marked expired if there is sufficient load on the system to require the node resources
     *  TERMINATED - node has been successfully terminated through the EC2 API
     * </pre>
     **/
    public enum STATUS { RUNNING,EXPIRED,TERMINATED };

    // This is used to set how far past the created date that the node will
    // be marked for termination
    private static final int NODE_INTERVAL_LIFETIME = 55; // 55 minutes

    // TODO: Refactor this to be AutomationRunRequest
    private final String uuid, instanceId, browser, os, ipAddress;
    private Date startDate,endDate;
    private final int nodeCapacity;
    private STATUS status;

    /**
     * Constructor to create a new node representing instance
     * @param uuid UUID of the test run that created this node
     * @param instanceId Instance ID of the instance this node represents
     * @param browser Requested browser of the test run that created this node
     * @param os Requested OS of the test run that created this node
     * @param startDate Date that this node was started
     * @param nodeCapacity Maximum test capacity that this node can run
     */
    public AutomationDynamicNode(String uuid,String instanceId,String browser, String os, Date startDate, int nodeCapacity){
        this(uuid, instanceId, browser, os, null, startDate, nodeCapacity);
    }

    public AutomationDynamicNode(String uuid,String instanceId,String browser, String os, String ipAddress, Date startDate, int nodeCapacity){
        this.uuid = uuid;
        this.instanceId = instanceId;
        this.browser = browser;
        this.os = os;
        this.ipAddress = ipAddress;
        this.startDate = startDate;
        this.endDate = computeEndDate(startDate);
        this.nodeCapacity = nodeCapacity;
        this.status = STATUS.RUNNING;
    }

    /**
     * Updates the status of this node.
     * @param status
     */
    public void updateStatus(STATUS status) {
        this.status = status;
    }

    /**
     * Computes the end date for this node based on the pre configured
     * end time
     * @param dateStarted
     * @return
     */
    private Date computeEndDate(Date dateStarted) {
        Calendar c = Calendar.getInstance();
        c.setTime(dateStarted);
        c.add(Calendar.MINUTE, AutomationDynamicNode.NODE_INTERVAL_LIFETIME);  // number of days to add
        return c.getTime();
    }

    /**
     * Returns the UUID for this node (will be the UUID of the run that
     * resulted in this node being started)
     * @return
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Returns the instance id of this node
     * @return
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Returns the browser of this node.  Will be the browser of the
     * run that resulted in this node being started
     * @return
     */
    public String getBrowser() {
        return browser;
    }

    /**
     * Returns the OS of this node
     * @return
     */
    public String getOs() {
        return os;
    }

    /**
     * Returns the private IP address of this node
     * @return
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Returns the date that his node was started.  This will
     * be the time that the node was requested and not necessarily started
     * @return
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Returns the currently scheduled end date of this node.  Note
     * that this can change as this node end date gets pushed back.
     * @return
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Sets the end date for this node.
     * @param endDate Date which will be set for the end date
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Increments the end date by an hour.  Useful for moving the end date into the next Amazon billing cycle
     */
    public void incrementEndDateByOneHour() {
        // Add 60 minutes so we're as close to the hour as we can be instead of adding 55 again
        setEndDate(AutomationUtils.modifyDate(getEndDate(),60,Calendar.MINUTE));
    }

    /**
     * Returns the total node capacity of this node (total number of browsers
     * that can run simultaneously)
     * @return
     */
    public int getNodeCapacity() {
        return nodeCapacity;
    }

    /**
     * Returns the current status of this node.
     * @return
     */
    public STATUS getStatus(){
        return status;
    }

    @Override
    public int compareTo(AutomationDynamicNode o) {
        return this.startDate.compareTo(getStartDate());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AutomationDynamicNode that = (AutomationDynamicNode) o;

        if (!instanceId.equals(that.instanceId)) {
            return false;
        }
        if (!uuid.equals(that.uuid)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = uuid.hashCode();
        result = 31 * result + instanceId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AutomationDynamicNode{" +
                "uuid='" + uuid + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", startDate=" + startDate +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}
