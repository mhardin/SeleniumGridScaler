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

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.utils.configuration.GridHubConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.annotations.VisibleForTesting;
import com.rmn.qa.AutomationConstants;
import com.rmn.qa.AutomationUtils;
import com.rmn.qa.RegistryRetriever;
import com.rmn.qa.aws.AwsVmManager;
import com.rmn.qa.aws.VmManager;

/**
 * Task to shut down the hub if it was dynamically started.  This task should only be started if this hub is a
 * should be terminated via {@link com.rmn.qa.aws.VmManager EC2}
 * @author mhardin
 */
public class AutomationHubCleanupTask extends AbstractAutomationCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(AutomationHubCleanupTask.class);

    private VmManager ec2;
    private final String instanceId;
    @VisibleForTesting
    static final String NAME = "Hub Cleanup Task";
    protected static Date createdDate = null;
    protected static Date endDate = null;
    static boolean errorEncountered = false;

    /**
     * Constructs a hub cleanup task with the specified parameters
     * @param registryRetriever Context retrieval mechanism to use
     * @param ec2 EC2 implementation to use for interaction with the hub
     * @param instanceId Instance ID of the hub to cleanup
     */
    public AutomationHubCleanupTask(RegistryRetriever registryRetriever, VmManager ec2,String instanceId) {
        super(registryRetriever);
        this.ec2 = ec2;
        this.instanceId = instanceId;
    }

    /**
     * Returns the ProxySet to be used for cleanup purposes.
     * @return
     */
    @VisibleForTesting
    protected ProxySet getProxySet() {
        return registryRetriever.retrieveRegistry().getAllProxies();
    }

    @Override
    public String getDescription() {
        return AutomationHubCleanupTask.NAME;
    }

    /**
     * Returns the created date which is pulled from the grid configuration
     * @return
     */
    protected Object getCreatedDate() {
        GridHubConfiguration config = registryRetriever.retrieveRegistry().getConfiguration();
        Object createdDate = getDate(config.custom);
        return createdDate;
    }

    /**
     +     * Attempts to parse the created date of the node from the custom parameter
     +     * @param customConfig
     +     * @return
     +     */
    private Date getDate(Map<String, String> customConfig) {
        String stringDate = (String)customConfig.get(AutomationConstants.CONFIG_CREATED_DATE);
        Date returnDate = null;
        try{
            returnDate = AwsVmManager.NODE_DATE_FORMAT.parse(stringDate);
        } catch (ParseException pe) {
            log.error(String.format("Error trying to parse created date [%s]", stringDate), pe);
        }
        return returnDate;
    }

    // We're going to continuously monitor this hub to see if we can shut it down.  If were approaching the next
    // billing cycle and the hub has no nodes, we will terminate the hub.  Also, if there is an error parsing the created date,
    // we will terminate it at our earliest convenience (no nodes) without regard to the creation time
    @Override
    public void doWork() {
        log.info("Performing cleanup on hub.");
        synchronized (AutomationHubCleanupTask.class) {
            if(createdDate == null) {
                Object createdDate = getCreatedDate();
                try{
                    AutomationHubCleanupTask.createdDate = AwsVmManager.NODE_DATE_FORMAT.parse((String)createdDate);
                } catch(ParseException pe) {
                    String message = "Error parsing created date for hub: " + pe;
                    log.error(message);
                    errorEncountered = true;
                }
                if(!errorEncountered) {
                    // Set the end date to be 55 minutes + creation date
                    AutomationHubCleanupTask.endDate = AutomationUtils.modifyDate(AutomationHubCleanupTask.createdDate, 55, Calendar.MINUTE);
                }
            }
        }
        Date currentTime = new Date();
        if(errorEncountered || currentTime.after(AutomationHubCleanupTask.endDate)) {
            // If we're into the next billing cycle, don't shut the hub down
            if(errorEncountered && getProxySet().isEmpty()) {
                log.info("Error was encountered parsing the created date, so the hub will be shutdown as it is empty.");
                ec2.terminateInstance(instanceId);
            } else if(AutomationUtils.isCurrentTimeAfterDate(AutomationHubCleanupTask.endDate, 6, Calendar.MINUTE)) {
                log.info("Current date: " + new Date());
                log.info("Current end date: " + AutomationHubCleanupTask.endDate);
                log.info(String.format("Hub [%s] ran into the next billing cycle.  Increasing end date.", instanceId));
                AutomationHubCleanupTask.endDate = AutomationUtils.modifyDate(AutomationHubCleanupTask.endDate,60,Calendar.MINUTE);
                return;
            }else if(getProxySet().isEmpty()){
                log.warn("No running nodes found after hub expiration time -- terminating hub: " + instanceId);
                ec2.terminateInstance(instanceId);
            } else {
                // This means tests are still running and we cannot terminate yet
                log.info("Hub could not be shutdown yet.");
                return;
            }
        }

    }
}
