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
package com.rmn.qa.servlet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.selenium.GridLauncher;
import org.openqa.grid.web.servlet.RegistryBasedServlet;
import org.openqa.selenium.remote.BrowserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.Instance;
import com.rmn.qa.AutomationConstants;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationDynamicNode;
import com.rmn.qa.AutomationRequestMatcher;
import com.rmn.qa.AutomationRunRequest;
import com.rmn.qa.AutomationUtils;
import com.rmn.qa.NodesCouldNotBeStartedException;
import com.rmn.qa.RegistryRetriever;
import com.rmn.qa.RequestMatcher;
import com.rmn.qa.aws.AwsVmManager;
import com.rmn.qa.aws.VmManager;
import com.rmn.qa.task.AutomationHubCleanupTask;
import com.rmn.qa.task.AutomationNodeCleanupTask;
import com.rmn.qa.task.AutomationOrphanedNodeRegistryTask;
import com.rmn.qa.task.AutomationPendingNodeRegistryTask;
import com.rmn.qa.task.AutomationReaperTask;
import com.rmn.qa.task.AutomationRunCleanupTask;
import com.rmn.qa.task.AutomationScaleNodeTask;

/**
 * Servlet used to register new {@link com.rmn.qa.AutomationRunRequest runs} as well as delete existing {@link com.rmn.qa.AutomationRunRequest runs}.
 * New {@link com.rmn.qa.AutomationRunRequest runs} will automatically spawn up new {@link com.rmn.qa.AutomationDynamicNode nodes} as needed
 * @author mhardin
 */
public class AutomationTestRunServlet extends RegistryBasedServlet implements RegistryRetriever {

    private static final long serialVersionUID = 8484071790930378855L;
    private static final Logger log = LoggerFactory.getLogger(AutomationTestRunServlet.class);

    // We override these for unit testing
    private VmManager ec2;
    private RequestMatcher requestMatcher;

    /**
     * Constructs a test run servlet with default values
     */
    public AutomationTestRunServlet() {
        this(null, true, new AwsVmManager(), new AutomationRequestMatcher());
    }

    /**
     * Constructs a test run servlet with customized values
     * @param registry Selenium registry object to use from Grid
     * @param initThreads Set to true if you want the cleanup threads initialized
     * @param ec2 EC2 implementation that you wish to use
     * @param requestMatcher RequestMatcher implementation you wish you use
     */
    public AutomationTestRunServlet(Registry registry, boolean initThreads, VmManager ec2, RequestMatcher requestMatcher) {
        super(registry);
        setManageEc2(ec2);
        setRequestMatcher(requestMatcher);
        // Start up our cleanup thread that will cleanup unused runs
        if(initThreads) {
            this.initCleanupThreads();
        }
    }

    private void initCleanupThreads() {
        // Wrapper to lazily fetch the Registry object as this is not populated at instantiation time
        // Spin up a scheduled thread to poll for unused test runs and clean up them
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new AutomationRunCleanupTask(this),
                60L, 60L, TimeUnit.SECONDS);
        // Spin up a scheduled thread to clean up and terminate nodes that were spun up
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new AutomationNodeCleanupTask(this,ec2,requestMatcher),
                60L, 15L, TimeUnit.SECONDS);
        // Spin up a scheduled thread to register unregistered dynamic nodes (will happen if hub gets shut down)
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new AutomationOrphanedNodeRegistryTask(this),
                1L, 5L, TimeUnit.MINUTES);
        // Spin up a scheduled thread to track nodes that are pending startup
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new AutomationPendingNodeRegistryTask(this),
                60L, 15L, TimeUnit.SECONDS);
        // Spin up a scheduled thread to analyzed queued requests to scale up capacity
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new AutomationScaleNodeTask(this, ec2),
                60L, 15L, TimeUnit.SECONDS);
        String instanceId = System.getProperty(AutomationConstants.INSTANCE_ID);
        if(instanceId != null && instanceId.length() > 0) {
            log.info("Instance ID detected.  Hub termination thread will be started.");
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new AutomationHubCleanupTask(this,ec2,instanceId),
                    5L, 1L, TimeUnit.MINUTES);
        } else {
            log.info("Hub is not a dynamic hub -- termination logic will not be started");
        }
        String runReaperThread = System.getProperty(AutomationConstants.REAPER_THREAD_CONFIG);
        // Reaper thread defaults to on unless specified not to run
        if(!"false".equalsIgnoreCase(runReaperThread)) {
            // Spin up a scheduled thread to terminate orphaned instances
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new AutomationReaperTask(this,ec2),
                    1L, 15L, TimeUnit.MINUTES);
        } else {
            log.info("Reaper thread not running due to config flag.");
        }
    }

    void setManageEc2(VmManager ec2) {
        this.ec2 = ec2;
    }

    void setRequestMatcher(RequestMatcher requestMatcher) {
        this.requestMatcher = requestMatcher;
    }

    protected ProxySet getProxySet() {
        return super.getRegistry().getAllProxies();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request,response);
    }


    /**
     * Attempts to register a new run request with the server.
     * Returns a 201 if the request can be fulfilled but AMIs must be started
     * Returns a 202 if the request can be fulfilled
     * Returns a 400 if the required parameters are not passed in.
     * Returns a 409 if the server is at full node capacity
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        String browserRequested = request.getParameter("browser");
        String browserVersion = request.getParameter("browserVersion");
        String osRequested = request.getParameter("os");
        String threadCount = request.getParameter("threadCount");
        String uuid = request.getParameter(AutomationConstants.UUID);
        // Return a 400 if any of the required parameters are not passed in
        // Check for uuid first as this is the most important variable
        if (uuid == null) {
            String msg = "Parameter 'uuid' must be passed in as a query string parameter";
            log.error(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }
        if (browserRequested == null) {
            String msg = "Parameter 'browser' must be passed in as a query string parameter";
            log.error(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }
        if (threadCount == null) {
            String msg = "Parameter 'threadCount' must be passed in as a query string parameter";
            log.error(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }
        Integer threadCountRequested = Integer.valueOf(threadCount);
        AutomationRunRequest runRequest = new AutomationRunRequest(uuid, threadCountRequested, browserRequested, browserVersion, osRequested);
        log.info(String.format("Server request received.  Browser [%s] - Requested Node Count [%s] - Request UUID [%s]", browserRequested, threadCountRequested, uuid));
        boolean amisNeeded;
        int amiThreadsToStart=0;
        int currentlyAvailableNodes;
        // Synchronize this block until we've added the run to our context for other potential threads to see
        synchronized (AutomationTestRunServlet.class) {
            int remainingNodesAvailable = AutomationContext.getContext().getTotalThreadsAvailable(getProxySet());
            // If the number of nodes this grid hub can actually run is less than the number requested, this hub can not fulfill this run at this time
            if(remainingNodesAvailable < runRequest.getThreadCount()) {
                log.error(String.format("Requested node count of [%d] could not be fulfilled due to hub limit. [%d] nodes available - Request UUID [%s]", threadCountRequested, remainingNodesAvailable, uuid));
                response.sendError(HttpServletResponse.SC_CONFLICT, "Server cannot fulfill request due to configured node limit being reached.");
                return;
            }
            // Get the number of matching, free nodes to determine if we need to start up AMIs or not
            currentlyAvailableNodes = requestMatcher.getNumFreeThreadsForParameters(getProxySet(),runRequest);
            // If the number of available nodes is less than the total number requested, we will have to spin up AMIs in order to fulfill the request
            amisNeeded = currentlyAvailableNodes < threadCountRequested;
            if(amisNeeded) {
                // Get the difference which will be the number of additional nodes we need to spin up to supplement existing nodes
                amiThreadsToStart = threadCountRequested - currentlyAvailableNodes;
            }
            // If the browser requested is not supported by AMIs, we need to not unnecessarily spin up AMIs
            if(amisNeeded && !browserSupportedByAmis(browserRequested)) {
                response.sendError(HttpServletResponse.SC_GONE,"Request cannot be fulfilled and browser is not supported by AMIs");
                return;
            }
            // Add the run to our context so we can track it
            AutomationRunRequest newRunRequest = new AutomationRunRequest(uuid, threadCountRequested, browserRequested, browserVersion, osRequested);
            boolean addSuccessful = AutomationContext.getContext().addRun(newRunRequest);
            if(!addSuccessful) {
                log.warn(String.format("Test run already exists for the same UUID [%s]", uuid));
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Test run already exists with the same UUID.");
                return;
            }
        }
        if (amisNeeded) {
            // Start up AMIs as that will be required
            log.warn(String.format("Insufficient nodes to fulfill request. New AMIs will be queued up. Requested [%s] - Available [%s] - Request UUID [%s]", threadCountRequested, currentlyAvailableNodes, uuid));
            try{
                AutomationTestRunServlet.startNodes(ec2, uuid, amiThreadsToStart, browserRequested, osRequested);
            } catch(NodesCouldNotBeStartedException e) {
                // Make sure and de-register the run if the AMI startup was not successful
                AutomationContext.getContext().deleteRun(uuid);
                String throwableMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                String msg = "Nodes could not be started: " + throwableMessage;
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,msg);
                return;
            }
            // Return a 201 to let the caller know AMIs will be started
            response.setStatus(HttpServletResponse.SC_CREATED);
            return;
        } else {
            // Otherwise just return a 202 letting the caller know the requested resources are available
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            return;
        }
    }

    /**
     * Starts up AMIs
     * @param threadCountRequested
     * @return
     */
    public static void startNodes(VmManager ec2, String uuid,int threadCountRequested, String browser, String os) throws NodesCouldNotBeStartedException {
        log.info(String.format("%d threads requested",threadCountRequested));
        try{
            String localhostname;
            // Try and get the IP address from the system property
            String runTimeHostName = System.getProperty(AutomationConstants.IP_ADDRESS);
            try{
                if(runTimeHostName == null) {
                    log.warn("Host name could not be determined from system property.");
                }
                localhostname = (runTimeHostName != null ) ? runTimeHostName : InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                log.error("Error parsing out host name",e);
                throw new NodesCouldNotBeStartedException("Host name could not be determined", e);
            }
            // TODO Make matching logic better
            int numThreadsPerMachine;
            if(AutomationUtils.lowerCaseMatch(BrowserType.CHROME,browser)) {
                numThreadsPerMachine = AwsVmManager.CHROME_THREAD_COUNT;
                //TODO Browser Enum replacement here
            } else if (AutomationUtils.lowerCaseMatch(BrowserType.IE,browser) || AutomationUtils.lowerCaseMatch(BrowserType.FIREFOX,browser)) {
                numThreadsPerMachine= AwsVmManager.FIREFOX_IE_THREAD_COUNT;
            } else {
                log.warn("Unsupported browser: " + browser);
                throw new NodesCouldNotBeStartedException("Unsupported browser: " + browser);
            }
            int leftOver = threadCountRequested % numThreadsPerMachine;
            int machinesNeeded = (threadCountRequested / numThreadsPerMachine);
            if(leftOver != 0) {
                // Add the remainder
                machinesNeeded++;
            }
            log.info(String.format("%s nodes will be started for run [%s]",machinesNeeded,uuid));
            List<Instance> instances = ec2.launchNodes(uuid, os, browser, localhostname, machinesNeeded, numThreadsPerMachine);
            log.info(String.format("%d instances started", instances.size()));
            // Reuse the start date since all the nodes were created within the same request
            Date startDate = new Date();
            for(Instance instance : instances) {
                // Add the node as pending startup to our context so we can track it in AutomationPendingNodeRegistryTask
                AutomationContext.getContext().addPendingNode(instance.getInstanceId());
                log.info("Node instance id: " + instance.getInstanceId());
                AutomationContext.getContext().addNode(
                        new AutomationDynamicNode(uuid, instance.getInstanceId(), browser, os, instance.getPrivateIpAddress(), startDate, numThreadsPerMachine));
            }
        } catch(Exception e) {
            log.error("Error trying to start nodes: " + e);
            throw new NodesCouldNotBeStartedException("Error trying to start nodes",e);
        }
    }

    /**
     * Returns true if the requested browser can be used within AMIs, and false otherwise
     * @param browser
     * @return
     */
    public static boolean browserSupportedByAmis(String browser) {
        return AutomationUtils.lowerCaseMatch(BrowserType.CHROME,browser) || AutomationUtils.lowerCaseMatch(BrowserType.FIREFOX,browser) || AutomationUtils.lowerCaseMatch("internetexplorer",browser);
    }

    @Override
    public Registry retrieveRegistry() {
        return getRegistry();
    }

    // Run this for local testing
    public static void main(String args[]) {
        try{
            GridLauncher.main(args);
        }catch(Exception e) {
            log.error("Error starting up grid: " + e);
        }
    }
}
