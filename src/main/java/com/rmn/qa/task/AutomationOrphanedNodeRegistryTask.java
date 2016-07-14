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
import java.util.Date;
import java.util.Map;

import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.selenium.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.rmn.qa.AutomationConstants;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationDynamicNode;
import com.rmn.qa.AutomationRunContext;
import com.rmn.qa.AutomationUtils;
import com.rmn.qa.RegistryRetriever;
import com.rmn.qa.aws.AwsVmManager;

/**
 * Registry task which registers orphaned  dynamic {@link com.rmn.qa.AutomationDynamicNode nodes}.  This can happen if the hub process restarts for whatever reason
 * and loses track of previously registered nodes
 * @author mhardin
 */
public class AutomationOrphanedNodeRegistryTask extends AbstractAutomationCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(AutomationOrphanedNodeRegistryTask.class);
    @VisibleForTesting
    static final String NAME = "Orphaned Node Registry Task";

    /**
     * Constructs a registry task with the specified context retrieval mechanism
     * @param registryRetriever Represents the retrieval mechanism you wish to use
     */
    public AutomationOrphanedNodeRegistryTask(RegistryRetriever registryRetriever) {
        super(registryRetriever);
    }

    /**
     * Returns the ProxySet to be used for cleanup purposes.
     * @return
     */
    protected ProxySet getProxySet() {
        return registryRetriever.retrieveRegistry().getAllProxies();
    }

    @Override
    public String getDescription() {
        return AutomationOrphanedNodeRegistryTask.NAME;
    }

    // We're going to continuously iterate over registered nodes with the hub.  If they're expired, we're going to mark them for removal.
    // If nodes marked for removal are used into the next billing cycle, then we're going to move their end date back again and put them back
    // into the running queue
    @Override
    public void doWork() {
        ProxySet proxySet = getProxySet();
        if(proxySet != null && proxySet.size() > 0) {
            for(RemoteProxy proxy : proxySet) {
                Map<String,Object> config = proxy.getConfig();
                // If the config has an instanceId in it, this means this node was dynamically started and we should
                // track it if we are not already
                if(config.containsKey(AutomationConstants.INSTANCE_ID)) {
                    String instanceId = (String)config.get(AutomationConstants.INSTANCE_ID);
                    AutomationRunContext context = AutomationContext.getContext();
                    // If this node is already in our context, that means we are already tracking this node to terminate
                    if(!context.nodeExists(instanceId)) {
                        Date createdDate = getDate(config);
                        // If we couldn't parse the date out, we are sort of out of luck
                        if(createdDate == null) {
                            break;
                        }
                        proxy.getConfig();
                        String uuid = (String)config.get(AutomationConstants.UUID);
                        int threadCount = (Integer)config.get(AutomationConstants.CONFIG_MAX_SESSION);
                        String browser = (String)config.get(AutomationConstants.CONFIG_BROWSER);
                        String os = (String)config.get(AutomationConstants.CONFIG_OS);
                        Platform platform = AutomationUtils.getPlatformFromObject(os);
                        AutomationDynamicNode node = new AutomationDynamicNode(uuid, instanceId, browser, platform, createdDate, threadCount);
                        log.info("Unregistered dynamic node found: " + node);
                        context.addNode(node);
                    }
                }
            }
        }
    }

    /**
     * Attempts to parse the created date of the node from the capabilities object
     * @param capabilities
     * @return
     */
    private Date getDate(Map<String,Object> capabilities) {
        String stringDate = (String)capabilities.get(AutomationConstants.CONFIG_CREATED_DATE);
        Date returnDate = null;
        try{
            returnDate = AwsVmManager.NODE_DATE_FORMAT.parse(stringDate);
        } catch (ParseException pe) {
            log.error(String.format("Error trying to parse created date [%s]: %s", stringDate, pe));
        }
        return returnDate;
    }
}
