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

import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Computes how many free/available resources there are for a given browser, browser version, and OS
 * @author mhardin
 */
public class AutomationRequestMatcher implements RequestMatcher {

    private static final Logger log = LoggerFactory.getLogger(AutomationRequestMatcher.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumFreeThreadsForParameters(ProxySet proxySet, AutomationRunRequest runRequest) {
        // This will keep a count of the number of instances that can run our requested test
        int totalFreeSlots = 0;
        // Current runs registered with the hub.  Make a copy of the set so we don't muck with the original set of registered runs
        Map<String,Integer> currentRuns = new HashMap<>();
        // Add them all into our map so we can keep track of both the runs we need to delete from our free count as well as
        // tests that are in progress for the run.
        for(String key : AutomationContext.getContext().getRunUuids()) {
            currentRuns.put(key,0);
        }
        for(RemoteProxy proxy : proxySet) {
            int matchingCapableSlots = 0;
            int runningSessions = 0;
            int matchingRunningSessions = 0;
            int maxNodeThreadsAvailable = proxy.getMaxNumberOfConcurrentTestSessions();
            String instanceId = proxy.getConfig().custom.get(AutomationConstants.INSTANCE_ID);
            boolean nodeMarkedForTermination = false;
            if(instanceId != null) {
                AutomationDynamicNode node = AutomationContext.getContext().getNode(instanceId);
                // If this node has been spun up and it is no longer in the running state, go to the next test slot
                // as we cannot consider this node to be a free resource
                if(node != null) {// There really shouldn't ever be a null node here but adding the check regardless
                    if(node.getStatus() != AutomationDynamicNode.STATUS.RUNNING) {
                        // If this is a dynamic node and its not in the running state, we should not be calculating its resources as available
                        nodeMarkedForTermination = true;
                    }
                }
            }
            if(instanceId != null) {
                log.info(String.format("Analyzing node %s...",instanceId));
            } else {
                log.info("Analyzing node...");
            }
            for (TestSlot testSlot : proxy.getTestSlots()) {
                //TODO Do selenium flavor of browsers to match here from RMN
                //TODO Better property matching
                TestSession session = testSlot.getSession();
                Map<String,Object> testSlotCapabilities = testSlot.getCapabilities();
                if(session != null) {
                    Map<String,Object> sessionCapabilities = session.getRequestedCapabilities();
                    Object uuid = sessionCapabilities.get(AutomationConstants.UUID);
                    // If the session has a UUID, go ahead and remove it from our runs that we're going to subtract from our available
                    // node count as this means the run is under way
                    if(uuid != null && currentRuns.containsKey(uuid)) {
                        int previousCount = currentRuns.get(uuid);
                        currentRuns.put((String)uuid,previousCount + 1);
                    }
                    if(runRequest.matchesCapabilities(testSlotCapabilities)) {
                        matchingRunningSessions++;
                    }
                    runningSessions++;
                }
                if(runRequest.matchesCapabilities(testSlotCapabilities)) {
                    matchingCapableSlots++;
                }
            }
            log.info(String.format("Node had %d matching running sessions and %d matching capable slots",matchingRunningSessions,matchingCapableSlots));
            int nodeFreeSlots;
            // If the node is marked for termination, we need to subtract matching running sessions from our free count, and make sure to not add
            // any capable slots, as they're really not even 'capable' since the node will be shutdown
            if(nodeMarkedForTermination) {
                log.info(String.format("Node marked for termination.  Subtracting %d sessions from %d total free slots",matchingRunningSessions,totalFreeSlots));
                totalFreeSlots -= matchingRunningSessions;
            } else {
                // Decrement the running sessions only if running + free is more than the total threads the node can handle. This will handle
                // load from a capacity standpoint of a node
                if((runningSessions + matchingCapableSlots) > maxNodeThreadsAvailable ) {
                    if(matchingCapableSlots < maxNodeThreadsAvailable) {
                        log.debug(String.format("Subtracting %d running sessions from %d capable sessions",runningSessions,matchingCapableSlots));
                        nodeFreeSlots = matchingCapableSlots - runningSessions;
                    } else {
                        log.debug(String.format("Subtracting %d running sessions from %d maximum node thread limit",matchingCapableSlots,maxNodeThreadsAvailable));
                        nodeFreeSlots = maxNodeThreadsAvailable - runningSessions;
                    }
                } else {
                    log.debug(String.format("%d free node slots derived from matching capable slots",matchingCapableSlots));
                    nodeFreeSlots = matchingCapableSlots;
                    // If there were any running sessions that match this browser, we need to subtract them from the capable sessions
                    if(matchingRunningSessions != 0) {
                        log.debug(String.format("%d matching running sessions will be subtracted from %d node free slots",matchingRunningSessions,nodeFreeSlots));
                        nodeFreeSlots -= matchingRunningSessions;
                    }
                }
                // If nodeFreeSlots is negative, go ahead and subtract the capable sessions instead
                if(nodeFreeSlots < 0) {
                    log.warn("The number of free node slots was less than 0.  Resetting to 0.");
                    nodeFreeSlots = 0;
                }
                totalFreeSlots += nodeFreeSlots;
            }
        }
        // Any runs still in this set means that run has not started yet, so we should consider this in our math
        for(String uuid : currentRuns.keySet()) {
            AutomationRunRequest request = AutomationContext.getContext().getRunRequest(uuid);
            // If we're not dealing with an old run request that just never started, go ahead and decrement
            // the value from available nodes on this hub
            if(request != null && !AutomationUtils.isCurrentTimeAfterDate(request.getCreatedDate(), 90, Calendar.SECOND) ) {
                if(!runRequest.matchesCapabilities(request)) {
                    log.warn(String.format("Requested run %s did not match pending run %s so count will not be included",runRequest,request));
                    continue;
                }
                int currentlyRunningTestsForRun = currentRuns.get(uuid);
                // If some of the tests are underway, subtract the currently running tests from the number of total tests, and then subtract that
                // from the number of free slots.  This way we're including tests that may not have started yet in our free resource check
                if(currentlyRunningTestsForRun < request.getThreadCount()) {
                    int countToSubtract = request.getThreadCount() - currentlyRunningTestsForRun;
                    log.debug(String.format("In progress run has %d threads that will be subtracted from our total free count %d.",countToSubtract,totalFreeSlots));
                    totalFreeSlots -= countToSubtract;
                }
            }
        }
        // Make sure we don't return a negative number to the caller
        if(totalFreeSlots < 0) {
            log.info("The number of total free node slots was less than 0.  Resetting to 0.");
            totalFreeSlots = 0;
        }
        log.info(String.format("Returning %s free slots for request %s",totalFreeSlots,runRequest));
        return totalFreeSlots;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumInProgressTests(ProxySet proxySet, AutomationRunRequest runRequest) {
        int inProgressTests = 0;
        for(RemoteProxy proxy : proxySet) {
            for(TestSlot testSlot : proxy.getTestSlots() ) {
                TestSession session = testSlot.getSession();
                if(session != null) {
                    if(runRequest.matchesCapabilities(session.getRequestedCapabilities())) {
                        inProgressTests++;
                    }
                }
            }
        }
        return inProgressTests;
    }

}
