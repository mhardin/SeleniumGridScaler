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

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openqa.grid.internal.ProxySet;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.rmn.qa.AutomationDynamicNode;
import com.rmn.qa.AutomationUtils;
import com.rmn.qa.BrowserPlatformPair;
import com.rmn.qa.NodesCouldNotBeStartedException;
import com.rmn.qa.RegistryRetriever;
import com.rmn.qa.aws.VmManager;
import com.rmn.qa.servlet.AutomationTestRunServlet;

/**
 * Registry task which registers unregistered dynamic {@link AutomationDynamicNode nodes}.  This can happen if the hub process restarts for whatever reason
 * and loses track of previously registered nodes
 * @author mhardin
 */
public class AutomationScaleNodeTask extends AbstractAutomationCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(AutomationScaleNodeTask.class);
    private static final int QUEUED_REQUEST_THRESHOLD_IN_MS = 5000;
    private Map<BrowserPlatformPair, Date> queuedBrowsersPlatforms = Maps.newHashMap();
    private Map<BrowserPlatformPair, ScaleCapacityContext> pendingStartupCapacity = Maps.newHashMap();
    private VmManager vmManager;
    @VisibleForTesting
    static final String NAME = "Automation Scale Node Task";

    /**
     * Constructs a registry task with the specified context retrieval mechanism
     * @param registryRetriever Represents the retrieval mechanism you wish to use
     */
    public AutomationScaleNodeTask(RegistryRetriever registryRetriever, VmManager vmManager) {
        super(registryRetriever);
        this.vmManager = vmManager;
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
        return AutomationScaleNodeTask.NAME;
    }

    @VisibleForTesting
    Iterable<DesiredCapabilities> getDesiredCapabilities() {
        return registryRetriever.retrieveRegistry().getDesiredCapabilities();
    }

    @VisibleForTesting
    List<AutomationDynamicNode> startNodes(VmManager vmManager, int browsersToStart, String browser, Platform platform) throws NodesCouldNotBeStartedException {
        return AutomationTestRunServlet.startNodes(vmManager, "AD-HOC", browsersToStart, browser, platform);
    }

    /**
     * Returns true if nodePendingDate is more than 5 seconds older than the current time
     */
    @VisibleForTesting
    boolean haveTestRequestsBeenQueuedForLongEnough(Date nowDate, Date nodePendingDate) {
        return nowDate.getTime() - nodePendingDate.getTime() > QUEUED_REQUEST_THRESHOLD_IN_MS;
    }

    /**
     * Returns true if the platform is not specified, is all platforms, or is linux
     * @param platformPair
     * @return
     */
    private boolean isEligibleToScale(BrowserPlatformPair platformPair) {
        return AutomationUtils.browserAndPlatformSupported(platformPair);
    }

    // We're going to continuously iterate over queued test requests with the hub.  If there are no nodes pending startup, we're basically going to calculate load
    // over time to see if we need to spin up new instances ad-hoc
    @Override
    public void doWork() {
        log.warn("Doing node scale work");
        // Iterate over all queued requests and track browser/platform combinations that are eligible to scale capacity for
        Iterator<DesiredCapabilities> pendingCapabilities = getDesiredCapabilities().iterator();
        if (pendingCapabilities.hasNext()) {
            log.info("Analyzing currently queued requests");
            while (pendingCapabilities.hasNext()) {
                DesiredCapabilities capabilities = pendingCapabilities.next();
                String browser = (String) capabilities.getCapability(CapabilityType.BROWSER_NAME);
                Object platformObject = capabilities.getCapability(CapabilityType.PLATFORM);
                Platform platform = AutomationUtils.getPlatformFromObject(platformObject);
                // If a valid platform wasn't able to be parsed from the queued test request, go ahead and default to Platform.ANY,
                // as Platform is not required for this plugin
                if (platform == null) {
                    // Default to ANY here and let AwsVmManager dictate what ANY translates to
                    platform = Platform.ANY;
                }
                // Group all platforms by their underlying family
                platform = AutomationUtils.getUnderlyingFamily(platform);
                BrowserPlatformPair desiredPair = new BrowserPlatformPair(browser, platform);

                // Don't attempt to calculate load for browsers & platform families we cannot start
                if (!isEligibleToScale(desiredPair)) {
                    log.warn("Unsupported browser and platform pair, browser:  " + browser + " platform family: " + platform.family());
                    continue;
                }

                // Handle requests for specific browser platforms.
                queuedBrowsersPlatforms.computeIfAbsent(new BrowserPlatformPair(browser, platform), s -> new Date());
            }
        }
        // Now, iterate over eligible browser/platform combinations that we're tracking and attempt to scale up
        Iterator<BrowserPlatformPair> queuedBrowsersIterator = queuedBrowsersPlatforms.keySet().iterator();
        while(queuedBrowsersIterator.hasNext()) {
            BrowserPlatformPair originalBrowserPlatformRequest = queuedBrowsersIterator.next();
            Date currentTime = new Date();
            Date timeBrowserPlatformQueued = queuedBrowsersPlatforms.get(originalBrowserPlatformRequest);
            if (haveTestRequestsBeenQueuedForLongEnough(currentTime, timeBrowserPlatformQueued)) { // If we've had pending queued requests for this browser for at least 5 seconds
                pendingCapabilities = getDesiredCapabilities().iterator();
                if (pendingCapabilities.hasNext()) {
                    int browsersToStart = 0;
                    while (pendingCapabilities.hasNext()) {
                        DesiredCapabilities currentlyQueuedCapabilities = pendingCapabilities.next();
                        String currentlyQueuedBrowser = (String) currentlyQueuedCapabilities.getCapability(CapabilityType.BROWSER_NAME);
                        Object platformObject = currentlyQueuedCapabilities.getCapability(CapabilityType.PLATFORM);
                        Platform currentlyQueuedPlatform = AutomationUtils.getPlatformFromObject(platformObject);
                        // If a valid platform wasn't able to be parsed from the queued test request, go ahead and default to Platform.ANY,
                        // as Platform is not required for this plugin
                        if (currentlyQueuedPlatform == null) {
                            currentlyQueuedPlatform = Platform.ANY;
                        }
                        // Group all platforms by their underlying family
                        currentlyQueuedPlatform = AutomationUtils.getUnderlyingFamily(currentlyQueuedPlatform);
                        if (originalBrowserPlatformRequest.equals(new BrowserPlatformPair(currentlyQueuedBrowser, currentlyQueuedPlatform))) {
                            browsersToStart++;
                        }
                    }
                    this.startNodesForBrowserPlatform(originalBrowserPlatformRequest, browsersToStart);
                }
                // Regardless of if we spun up browsers or not, clear this count out
                queuedBrowsersIterator.remove();
            }
        }
    }

    /**
     * Starts up the specified number of browsers for the specified browser/platform pair.  Takes into account nodes that are pending startup
     * @param browserPlatform
     * @param browsersToStart
     */
    private void startNodesForBrowserPlatform(BrowserPlatformPair browserPlatform, int browsersToStart) {
        // If there are queued up browser requests, go ahead and subtract nodes pending startup from the count to account for the pending capacity
        if (browsersToStart > 0 && pendingStartupCapacity.containsKey(browserPlatform)) {
            ScaleCapacityContext pendingCapacityContext = pendingStartupCapacity.get(browserPlatform);
            // Go ahead and clear out any nodes that have started up
            pendingCapacityContext.clearPendingNodes();
            // This represents capacity that is pending startup and we need to subtract it from the total amount of nodes that we want to start
            // so we do not get an excess of capacity
            int pendingCapacity = pendingCapacityContext.getTotalCapacityCount();
            if (pendingCapacity > 0) {
                log.warn(String.format("Subtracting %d capacity from queued load %s for browser/platform %s", pendingCapacity, browsersToStart, browserPlatform));
            }
            browsersToStart = browsersToStart - pendingCapacity;
        }
        if (browsersToStart > 0) {
            log.info(String.format("Spinning up %d threads for browser/platform %s based on current test load", browsersToStart, browserPlatform));
            try {
                List<AutomationDynamicNode> createdNodes = this.startNodes(vmManager, browsersToStart, browserPlatform.getBrowser(), browserPlatform.getPlatform());
                // Grab the scale context object for this browser/platform pair
                ScaleCapacityContext contextForBrowserPair = pendingStartupCapacity.computeIfAbsent(browserPlatform, browserPlatformPair -> new ScaleCapacityContext());
                // Add all the created nodes to the context object so we can compute load programmatically for pending browsers
                contextForBrowserPair.addAll(createdNodes);
            } catch (NodesCouldNotBeStartedException e) {
                throw new RuntimeException("Error scaling up nodes", e);
            }
        }

    }
}