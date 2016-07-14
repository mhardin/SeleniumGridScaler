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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.rmn.qa.aws.VmManager;

/**
 * Context object used to keep track of registered runs and dynamic nodes.
 *
 * @author  mhardin
 */
public final class AutomationRunContext {

    private static final Logger log = LoggerFactory.getLogger(AutomationRunContext.class);

    private static final int CLEANUP_LIFE_LENGTH_IN_SECONDS = 420; // 7 minutes
    public static final int PENDING_NODE_EXPIRATION_TIME_IN_MINUTES = 20;
    private Map<String, AutomationRunRequest> requests = Maps.newConcurrentMap();
    private Map<String, AutomationDynamicNode> nodes = Maps.newConcurrentMap();
    private Map<String, AutomationDynamicNode> pendingStartupNodes = Maps.newConcurrentMap(); // Nodes that are currently starting up and have not registered yet

    private volatile int totalNodeCount;

    @VisibleForTesting
    AutomationRunContext() { }

    /**
     * Deletes the run from the requests map.
     *
     * @param  uuid  UUID of the run to delete
     */
    public boolean deleteRun(final String uuid) {
        AutomationRunRequest request;
        synchronized (requests) {
            request = requests.remove(uuid);
        }

        return request != null;
    }

    /**
     * Adds the specified run to the requests map.
     *
     * @param   runRequest  Request object to add to the map
     *
     * @return  Returns false if the request already exists
     */
    public boolean addRun(final AutomationRunRequest runRequest) {
        String uuid = runRequest.getUuid();
        synchronized (requests) {
            if (requests.containsKey(uuid)) {
                return false;
            }

            requests.put(uuid, runRequest);
        }

        return true;
    }

    /**
     * Returns true if the run already exists, false otherwise.
     *
     * @param   uuid  UUID of the run to check
     *
     * @return
     */
    public boolean hasRun(final String uuid) {
        return requests.containsKey(uuid);
    }

    /**
     * Gets the run request for the specified UUID.
     *
     * @param   uuid  UUID of the run to check
     *
     * @return
     */
    public AutomationRunRequest getRunRequest(final String uuid) {
        synchronized (requests) {
            return requests.get(uuid);
        }
    }

    /**
     * Returns the set of currently registered run UUIDs.
     *
     * @return
     */
    public Set<String> getRunUuids() {
        return requests.keySet();
    }

    /**
     * Returns true if there are any registered runs that have started in the last 2 minutes, false otherwise.
     *
     * @return
     */
    public boolean isNewRunQueuedUp() {
        synchronized (requests) {
            Set<String> uuids = AutomationContext.getContext().getRunUuids();
            for (String uuid : uuids) {
                AutomationRunRequest request = AutomationContext.getContext().getRunRequest(uuid);
                if (request.isNewRun()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Adds the node to the internal tracking map.
     *
     * @param  node
     */
    public void addNode(final AutomationDynamicNode node) {
        this.addNode(node.getInstanceId(), node);
    }

    /**
     * Adds the node to the internal tracking map.
     *
     * @param  instanceId  Instance ID to add a node for
     * @param  node        Node to add for the specified instance id
     */
    public void addNode(final String instanceId, final AutomationDynamicNode node) {
        nodes.put(instanceId, node);
    }

    /**
     * Returns the specified node from the internal tracking map.
     *
     * @param   instanceId  Instance ID to retrieve a node for
     *
     * @return
     */
    public AutomationDynamicNode getNode(final String instanceId) {
        return nodes.get(instanceId);
    }

    /**
     * Returns true if the specified instance exists, false otherwise.
     *
     * @param   instanceId  Instance ID of node to check
     *
     * @return
     */
    public boolean nodeExists(final String instanceId) {
        return nodes.containsKey(instanceId);
    }

    /**
     * Returns true if the specified instance exists, false otherwise.
     *
     * @param   instanceId  Instance ID of node to check
     *
     * @return
     */

    /**
     * Returns the internal tracking map. Make sure any operation done on this map is thread safe using each node for
     * synchronization
     *
     * @return
     */
    public Map<String, AutomationDynamicNode> getNodes() {
        return nodes;
    }

    /**
     * Clean up any requests with no remaining running tests.
     *
     * @param  proxySet
     */
    public void cleanUpRunRequests(final ProxySet proxySet) {
        AutomationRunContext context = AutomationContext.getContext();
        Set<String> uuidsToRemove = new HashSet<String>();
        synchronized (requests) {
            Iterator<String> requestsIterator = requests.keySet().iterator();
            if (requestsIterator.hasNext()) {

                // Grab our current date to use on all the requests we check
                while (requestsIterator.hasNext()) {
                    String targetUuid = requestsIterator.next();
                    AutomationRunRequest request = requests.get(targetUuid);
                    if (!isRunOld(request)) {
                        log.info(String.format("Run [%s] is not at least [%d] seconds old.  Will not analyze.",
                                targetUuid, AutomationRunContext.CLEANUP_LIFE_LENGTH_IN_SECONDS));
                        continue;
                    }

                    boolean uuidFound = false;
                    for (RemoteProxy proxy : proxySet) {
                        List<TestSlot> slots = proxy.getTestSlots();

                        // Once we find at least one test run with the given UUID, we want to break out
                        // as we are looking for runs with NO running tests with a matching UUID
                        for (int i = 0; !uuidFound && i < slots.size(); i++) {
                            TestSession testSession = slots.get(i).getSession();
                            if (testSession != null) {

                                // Check the session UUID instead of the node UUID as the node UUID is going to be the
                                // test run UUID that caused
                                // the node to be started, but will not necessarily be the run that is currently
                                // running on the node
                                Object sessionUuid = testSession.getRequestedCapabilities().get(
                                        AutomationConstants.UUID);
                                if (targetUuid.equals(sessionUuid)) {
                                    uuidFound = true;
                                    break;
                                }
                            }
                        }

                        // If we found the UUID on this node, we don't need to check any other nodes as we only need
                        // to know about
                        // at least run test still running in order to know we don't need to remove this run
                        if (uuidFound) {
                            break;
                        }
                    }

                    // If we didn't find a test belonging to this uuid, go ahead and remove the run
                    if (!uuidFound) {
                        log.info(String.format(
                                "Tracked test run [%s] found with no running tests.  Adding to set for removal.",
                                targetUuid));
                        uuidsToRemove.add(targetUuid);
                    }
                    // Otherwise go ahead and continue to look at our other registered runs
                    else {
                        continue;
                    }
                }
            }
        }

        if (uuidsToRemove.size() == 0) {
            log.warn("No runs found to clean up");
        }

        for (String uuidToRemove : uuidsToRemove) {
            log.warn(String.format("Removing run because it has no more running test slots. UUID [%s]", uuidToRemove));
            context.deleteRun(uuidToRemove);
        }
    }

    /**
     * Returns true if the run request is old enough for the configure criteria.
     *
     * @param   runRequest
     *
     * @return
     */
    private boolean isRunOld(final AutomationRunRequest runRequest) {

        // Get the amount of seconds passed
        if (AutomationUtils.lowerCaseMatch("internetexplorer", runRequest.getBrowser())) {
            return System.currentTimeMillis() > runRequest.getCreatedDate().getTime() + (10 * 60 * 1000); // 10 minutes for IE to come online
        } else {
            return System.currentTimeMillis()
                    > runRequest.getCreatedDate().getTime() + (AutomationRunContext.CLEANUP_LIFE_LENGTH_IN_SECONDS
                    * 1000);
        }
    }

    /**
     * Returns the total node count supported for this grid hub.
     *
     * @return
     */
    public int getTotalNodeCount() {
        return totalNodeCount;
    }

    /**
     * Sets the total node count supported for this grid hub.
     *
     * @param  totalNodeCount
     */
    public void setTotalNodeCount(final int totalNodeCount) {
        this.totalNodeCount = totalNodeCount;
    }

    /**
     * Adds the specified node to the pending node collection
     * @param node
     */
    public void addPendingNode(AutomationDynamicNode node) {
        pendingStartupNodes.put(node.getInstanceId(), node);
    }

    /**
     * Removes the specified node from the pending node collection
     * @param amiId
     */
    public void removePendingNode(String amiId) {
        pendingStartupNodes.remove(amiId);
    }

    /**
     * Checks if the specified node exists in the pending node collection
     * @param amiId
     * @return True if the node exists, false otherwise
     */
    public boolean pendingNodeExists(String amiId) {
        return pendingStartupNodes.containsKey(amiId);
    }

    /**
     * Removes nodes from the pending collection if they haven't come online after 20 minutes
     * @param vmManager
     */
    public void removeExpiredPendingNodes(VmManager vmManager) {
        Iterator<Map.Entry<String,AutomationDynamicNode>> iterator = getPendingStartupNodes().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String,AutomationDynamicNode> entry = iterator.next();
            AutomationDynamicNode node = entry.getValue();
            if ((System.currentTimeMillis() - node.getStartDate().getTime()) > PENDING_NODE_EXPIRATION_TIME_IN_MINUTES * 60000) { // 20 minutes
                log.error(String.format("Node %s was pending longer than 20 minutes.  Removing from pending set.", node));
                try {
                    if (!vmManager.terminateInstance(node.getInstanceId())) {
                        log.warn(String.format("Error terminating pending node %s that never came online", node));
                    }
                    node.updateStatus(AutomationDynamicNode.STATUS.TERMINATED);
                } catch (Exception e) {
                    log.warn(String.format("Exception terminating pending node %s that never came online", node), e);
                }
                iterator.remove();
            }
        }
    }

    /**
     * Returns the pending nodes collection
     * @return
     */
    private Map<String, AutomationDynamicNode> getPendingStartupNodes() {
        return pendingStartupNodes;
    }

    /**
     * Returns true if there are no nodes that are pending startup
     * @return
     */
    public boolean noPendingNodesExist() {
        return pendingStartupNodes.isEmpty();
    }

    /**
     * Returns the number of additional threads this hub can support. This considers all test runs that are in progress
     *
     * @return
     */
    public int getTotalThreadsAvailable(final ProxySet proxySet) {
        int currentlyUsedNodes = 0, untrackedTests = 0;
        Map<String, Integer> runsToSessions = new HashMap<>();

        // Go ahead and iterate over all running tests and store them in a map so we can compare all UUIDs
        if (proxySet != null) {
            for (RemoteProxy proxy : proxySet) {
                for (TestSlot testSlot : proxy.getTestSlots()) {

                    // This means a test is running
                    TestSession testSession = testSlot.getSession();
                    if (testSession != null) {

                        // Most test runs should have their test run UUID in the capabilities.  This makes it easy to
                        // line up tests with their run requests
                        if (testSession.getRequestedCapabilities().containsKey(AutomationConstants.UUID)) {
                            String uuid = (String) testSession.getRequestedCapabilities().get(AutomationConstants.UUID);
                            Integer existingRuns = runsToSessions.get(uuid);
                            if (existingRuns == null) {
                                runsToSessions.put(uuid, 1);
                            } else {
                                Integer newRunAmount = existingRuns + 1;
                                runsToSessions.put(uuid, newRunAmount);
                            }
                        } else {

                            // No UUID means we still need to count this against the total count and just not match it
                            // up against the run requests
                            log.info("Untracked test found.  Adding.");
                            untrackedTests++;
                        }
                    }
                }
            }
        }

        // Now process all registered runs to make sure there weren't any requests that just haven't had any started
        // nodes
        for (String uuid : runsToSessions.keySet()) {
            AutomationRunRequest registeredRun = getRunRequest(uuid);

            // If a the registered run exists AND is still new, we should count the original request amount as all
            // tests may not be in flight yet
            if (registeredRun != null && registeredRun.isNewRun()) {
                log.info(String.format("Adding %d for test run [%s]", registeredRun.getThreadCount(), uuid));
                currentlyUsedNodes += registeredRun.getThreadCount();
            } else {

                // Otherwise, count the in progress tests
                Integer runsInProgress = runsToSessions.get(uuid);
                log.info(String.format("Adding %d in progress tests for test run [%s]", runsInProgress, uuid));
                currentlyUsedNodes += runsInProgress;
            }
        }

        // Iterate over all runs currently running and add up their count so we can diff this from the total
        // count this hub can support
        for (AutomationRunRequest request : requests.values()) {
            String uuid = request.getUuid();

            // If we already processed this UUID in the above for loop, go ahead and skip this run
            if (runsToSessions.containsKey(uuid)) {
                continue;
            }

            // At this point, we're dealing with test runs that do not have any in progress tests, so count the
            // original thread count requested
            log.info(String.format("Adding %d tests for test run [%s]", request.getThreadCount(), uuid));
            currentlyUsedNodes += request.getThreadCount();
        }

        if (untrackedTests != 0) {
            log.info(String.format("Adding %d tests to the total in progress count %d", untrackedTests,
                    currentlyUsedNodes));
            currentlyUsedNodes = currentlyUsedNodes + untrackedTests;
        }

        int threadsStillAvailable = totalNodeCount - currentlyUsedNodes;
        log.info(String.format("Returning %d free capacity for the hub", threadsStillAvailable));
        return threadsStillAvailable;
    }

}
