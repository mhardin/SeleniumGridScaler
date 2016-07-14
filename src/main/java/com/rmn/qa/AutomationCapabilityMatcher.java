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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.openqa.grid.internal.utils.DefaultCapabilityMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

/**
 * Custom CapabilityMatcher which will not match a node that is marked as Expired/Terminated, which will happen
 * via any running {@link com.rmn.qa.task.AutomationNodeCleanupTask AutomationNodeCleanupTasks}
 * @author mhardin
 */
public class AutomationCapabilityMatcher extends DefaultCapabilityMatcher {

    private static final Logger log = LoggerFactory.getLogger(AutomationCapabilityMatcher.class);

    // We can add additional capability keys we want to check here
    @VisibleForTesting
    final Set<String> additionalConsiderations = new HashSet<>();

    public AutomationCapabilityMatcher() {
        super();
        String propertyValue = System.getProperty(AutomationConstants.EXTRA_CAPABILITIES_PROPERTY_NAME);
        if(!StringUtils.isEmpty(propertyValue)) {
            if(propertyValue.contains(",")) {
                String[] capabilities = propertyValue.split(",");
                for(String capability : capabilities) {
                    log.info("Adding capability: " + capability);
                    additionalConsiderations.add(capability);
                }
            } else {
                log.info("Adding capability from property value: " + propertyValue);
                additionalConsiderations.add(propertyValue);
            }
        }
    }

    @Override
    public boolean matches(Map<String, Object> nodeCapability,Map<String, Object> requestedCapability) {
        // First we need to check any additional capabilities that may exist in the requested set.  We're iterating over
        // additionalConsiderations as its likely to be a smaller collection for now than the requestedCapabilities
        for(String s : additionalConsiderations) {
            Object requestedCapabilityValue = requestedCapability.get(s);
            if(requestedCapabilityValue != null) {
                if(!requestedCapabilityValue.equals(nodeCapability.get(s))) {
                    return false;
                }
            }
        }
        // If neither expected config value exists, go ahead and default to the default matching behavior
        // as this node is most likely not a dynamically started node
        if(!nodeCapability.containsKey(AutomationConstants.INSTANCE_ID)) {
            return super.matches(nodeCapability,requestedCapability);
        }
        String instanceId = (String)nodeCapability.get(AutomationConstants.INSTANCE_ID);
        AutomationRunContext context = AutomationContext.getContext();
        // If the run that spun up these hubs is still happening, just perform the default matching behavior
        // as that run is the one that requested these nodes.
        AutomationDynamicNode node = context.getNode(instanceId);
        if(node != null && (node.getStatus() != AutomationDynamicNode.STATUS.RUNNING) ) {
            log.debug(String.format("Node [%s] will not be used to match a request as it is expired/terminated",instanceId));
            // If the run that spun these hubs up is not in progress AND this node has been flagged to shutdown,
            // do not match this node up to fulfill a test request
            return false;
        } else {
            // If the node couldn't be retrieved or was not expired/terminated, then we should just use the default matching behavior
            return super.matches(nodeCapability,requestedCapability);
        }
    }
}
