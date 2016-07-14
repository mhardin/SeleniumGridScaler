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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.rmn.qa.AutomationConstants;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationDynamicNode;
import com.rmn.qa.AutomationRunContext;
import com.rmn.qa.AutomationRunRequest;
import com.rmn.qa.AutomationUtils;
import com.rmn.qa.RegistryRetriever;
import com.rmn.qa.RequestMatcher;
import com.rmn.qa.aws.VmManager;

/**
 * Cleanup task which moves {@link com.rmn.qa.AutomationDynamicNode nodes} into the correct status depending on their lifecycle.  The purpose of this
 * task is to terminate nodes once current load is sufficient to allow for the node to be safely shutdown
 * @author mhardin
 */
public class AutomationNodeCleanupTask extends AbstractAutomationCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(AutomationNodeCleanupTask.class);

    private VmManager ec2;
    private RequestMatcher requestMatcher;
    @VisibleForTesting
    static final String NAME = "Node Cleanup Task";

    /**
     * Constructs a cleanup task with the specified options
     * @param registryRetriever Context retrieval mechanism
     * @param ec2 EC2 implementation
     * @param requestMatcher Request matcher implementation
     */
    public AutomationNodeCleanupTask(RegistryRetriever registryRetriever, VmManager ec2, RequestMatcher requestMatcher) {
        super(registryRetriever);
        this.ec2 = ec2;
        this.requestMatcher = requestMatcher;
    }

    @Override
    public String getDescription() {
        return AutomationNodeCleanupTask.NAME;
    }

    /**
     * Returns the ProxySet to be used for cleanup purposes.
     * @return
     */
    @VisibleForTesting
    protected ProxySet getProxySet() {
        return registryRetriever.retrieveRegistry().getAllProxies();
    }

    @VisibleForTesting
    protected void removeProxy(RemoteProxy proxy) {
        registryRetriever.retrieveRegistry().removeIfPresent(proxy);
    }

    // We're going to continuously iterate over registered nodes with the hub.  If they're expired, we're going to mark them for removal.
    // If nodes marked for removal are used into the next billing cycle, then we're going to move their end date back again and put them back
    // into the running queue
    @Override
    public void doWork() {
        log.info("Performing cleanup on nodes.");
        AutomationRunContext context = AutomationContext.getContext();
        Map<String,AutomationDynamicNode> nodes = context.getNodes();
        synchronized (nodes) {
            Iterator<String> iterator = nodes.keySet().iterator();
            Date nowDate = new Date();
            while(iterator.hasNext()) {
                String instanceId = iterator.next();
                AutomationDynamicNode node = nodes.get(instanceId);
                AutomationDynamicNode.STATUS nodeStatus = node.getStatus();
                // If the current time is after the scheduled end time for this node and the node is still running, go ahead and queue it to be removed
                if(nodeStatus == AutomationDynamicNode.STATUS.RUNNING && nowDate.after(node.getEndDate())) {
                    if(canNodeShutDown(node)) {
                        log.info(String.format("Updating node %s to 'EXPIRED' status.  Start date [%s] End date [%s]",instanceId,node.getStartDate(),node.getEndDate()));
                        node.updateStatus(AutomationDynamicNode.STATUS.EXPIRED);
                    }
                } else if (nodeStatus == AutomationDynamicNode.STATUS.EXPIRED) {
                    // See if we're in the next billing cycle (create + 55 + 6, which should equal 61 minutes and would safely be in the next billing cycle)
                    if (AutomationUtils.isCurrentTimeAfterDate(node.getEndDate(), 6, Calendar.MINUTE)) {
                        node.incrementEndDateByOneHour();
                        log.info(String.format("Node [%s] was still running after initial allotted time.  Resetting status and increasing end date to %s.", instanceId, node.getEndDate()));
                        node.updateStatus(AutomationDynamicNode.STATUS.RUNNING);
                    } else if (isNodeCurrentlyEmpty(instanceId)) {
                        log.info(String.format("Terminating node %s and updating status to 'TERMINATED'", instanceId));
                        // Delete node
                        ec2.terminateInstance(instanceId);
                        // Also remove the node from Selenium's tracking set as there have been cases where the node sticks around
                        // and slows down the console as the node can on longer be pinged
                        removeFromProxy(getProxySet(), instanceId);
                        node.updateStatus(AutomationDynamicNode.STATUS.TERMINATED);
                    }
                } else if (nodeStatus == AutomationDynamicNode.STATUS.TERMINATED) {
                    // If the current time is more than 30 minutes after the node end date, we should remove it from being tracked
                    if (System.currentTimeMillis() > node.getEndDate().getTime() + (30 * 60 * 1000)) {
                        // Remove it, and this will remove from tracking since we're referencing the collection
                        log.info(String.format("Removing node [%s] from internal tracking set", instanceId));
                        iterator.remove();
                    }
                }
            }
        }
    }

    /**
     * Removes the specified instance from the ProxySet, if it exists
     * @param proxySet
     * @param instanceId
     */
    private void removeFromProxy(ProxySet proxySet, String instanceId) {
        Iterator<RemoteProxy> iterator = proxySet.iterator();
        while(iterator.hasNext()) {
            RemoteProxy proxy = iterator.next();
            Map<String,Object> config = proxy.getConfig();
            if(config.containsKey(AutomationConstants.INSTANCE_ID)) {
                String nodeInstanceId = (String)config.get(AutomationConstants.INSTANCE_ID);
                if (instanceId.equals(nodeInstanceId)) {
                    log.info("Node found to remove from proxy set: " + instanceId);
                    removeProxy(proxy);
                    return;
                }
            }
        }
        log.warn("Node not found to remove from proxy set: " + instanceId);
    }

    /**
     * Returns true if the specified node can be safely shut down, false otherwise
     * @param node
     * @return
     */
    // If free slots are greater than OR equal to our node capacity, that means we have enough wiggle room to go ahead and delete this node.
    // We also need to check to make sure there are no registered runs that have not yet started up
    private boolean canNodeShutDown(AutomationDynamicNode node) {
        // If a new run is queued up, we cannot shut down nodes until it starts, so short circuit out of here
        if(AutomationContext.getContext().isNewRunQueuedUp()) {
            log.warn(String.format("Node %s cannot be shutdown yet as a new run is queued up.", node.getInstanceId()));
            return false;
        }
        ProxySet proxySet = getProxySet();
        // Iterate over our set of proxies until we find the proxy which represents the node we're trying to shut down
        Set<AutomationRunRequest> existingSlots = new HashSet<>();
        Map<String,RemoteProxy> proxies = new HashMap<>();
        for(RemoteProxy proxy : proxySet) {
            Object instanceId = proxy.getConfig().get(AutomationConstants.INSTANCE_ID);
            if(node.getInstanceId().equals(instanceId)) {
                // Associate this proxy with the instance id for use later
                proxies.put((String)instanceId,proxy);
                // Once we've found our target proxy, go ahead and compile a list of browsers this node uses
                for(TestSlot testSlot : proxy.getTestSlots()) {
                    AutomationRunRequest requestRepresentation = AutomationRunRequest.requestFromCapabilities(testSlot.getCapabilities());
                    existingSlots.add(requestRepresentation);
                }
            }
        }
        if(existingSlots.size() == 0) {
            log.error("No browsers found for node: " + node.getInstanceId());
        }
        for(AutomationRunRequest request : existingSlots) {
            int freeSlotsForBrowser = requestMatcher.getNumFreeThreadsForParameters(getProxySet(), request);
            if(freeSlotsForBrowser == 0) {
                log.info(String.format("No free slots exist so node will not be shutdown. Node: %s Request: %s Browser: %s",node.getInstanceId(),request,request.getBrowser()));
                return false;
            }
            // Once we find at least one browser that does not have enough capacity to shut this node down
            // we can return false to say we cannot shut this node down.
            RemoteProxy proxy = proxies.get(node.getInstanceId());
            if(proxy == null) {
                log.error(String.format("Proxy was not found for node %s", node.getInstanceId()));
                return false;
            }
            int matchingSlots = 0;
            // Go over the test slots and get the number of browsers this node can run
            for(TestSlot testSlot : proxy.getTestSlots()) {
                // If this test slot matches the browser, increment our count
                if(request.matchesCapabilities(testSlot.getCapabilities())) {
                    matchingSlots ++;
                }
            }
            log.info(String.format("%d matching slots were found for node %s",matchingSlots,node.getInstanceId()));
            // Get the lesser number between the total node capacity and browser specific capacity
            int finalNum = (matchingSlots < node.getNodeCapacity()) ? matchingSlots : node.getNodeCapacity();
            if(freeSlotsForBrowser < finalNum) {
                // If there are no running tests which match the browser, we don't need to honor this browser for shutdown logic
                // as it is not currently needed
                int inProgressTests = requestMatcher.getNumInProgressTests(getProxySet(),request);
                if(inProgressTests != 0) {
                    log.info(String.format("Current load will not allow for node to shutdown right now. Node: %s Request: %s Free Slots: %s Node Slots: %s",node.getInstanceId(),request,freeSlotsForBrowser,finalNum));
                    return false;
                } else {
                    log.info(String.format("Tests are not in progress. Node: %s Request: %s Free Slots: %s Node Slots: %s",node.getInstanceId(),request,freeSlotsForBrowser,finalNum));
                }
            } else {
                log.info(String.format("Load suitable for node shutdown. Request [%s]. Node [%s] Free slots [%s] Node slots [%s]",request, node, freeSlotsForBrowser,finalNum));
            }
        }
        // If we iterated over every browser for the node and load was not heavy enough, we can safely shut this node down
        return true;
    }

    /**
     * Returns true if the specified node is empty and has no runs on it, and false otherwise
     * @param instanceToFind
     * @return
     */
    public boolean isNodeCurrentlyEmpty(String instanceToFind) {
        ProxySet proxySet = getProxySet();
        boolean nodeEmpty = true;
        for (RemoteProxy proxy : proxySet) {
            List<TestSlot> slots = proxy.getTestSlots();
            Object instanceId = proxy.getConfig().get(AutomationConstants.INSTANCE_ID);
            // If the instance id's do not match, this means this is not the node we are looking for
            // and we should continue on to the next one
            if(!instanceToFind.equals(instanceId)) {
                continue;
            }
            // Now that we found the matching node, iterate over all its test slots to see if any sessions are running
            for (TestSlot testSlot : slots) {
                // If we find a running session, this means the node is occupied, so we should return false
                if(testSlot.getSession() != null) {
                    nodeEmpty = false;
                    break;
                }
            }
            if (!nodeEmpty) {
                return checkNodeForHungSessions(instanceToFind);
            } else {
                // If we reached this point, this means we found our target node AND it had no sessions, meaning the node was empty
                return true;
            }
        }
        // If we didn't find a matching node, we're going to say the nodes is empty so we can terminate it
        log.warn("No matching node was found in the proxy set.  Instance id: " + instanceToFind);
        return true;
    }

    /**
     * Checks the instance in question to see if any browser slots are 'hung' that would otherwise block terminating the instance
     * @param instanceToFind
     * @return
     */
    private boolean checkNodeForHungSessions(String instanceToFind) {
        AutomationDynamicNode node = AutomationContext.getContext().getNode(instanceToFind);
        // If the IP is null, there isn't anything we can do with the node and we have to treat it as not empty
        if (node.getIpAddress() == null) {
            return false;
        } else {
            String url = String.format("http://%s:5555/wd/hub/sessions", node.getIpAddress());
            log.info("Orphaned nodes URL: " + url);
            try {
                log.info("Attempting to retrieve in progress sessions before termination for node: " + node.getInstanceId());
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setConnectTimeout(10000);
                con.setReadTimeout(10000);

                int responseCode = con.getResponseCode();

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer responseBuffer = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    responseBuffer.append(inputLine);
                }
                in.close();
                String response = responseBuffer.toString();
                if  (response != null && !response.contains("capabilities")) {
                    log.info("Node had hung sessions but will be terminated anyways.");
                    return true;
                }
            } catch(SocketTimeoutException ste) {
                log.warn("Timeout attempting to retrieve in progress sessions for node: " + node.getInstanceId(), ste);
            } catch (Exception e) {
                log.warn("Error retrieving sessions from node", e);
                // We don't need an explicit return here as we can just reuse the one below
            }
            return false;
        }
    }
}
