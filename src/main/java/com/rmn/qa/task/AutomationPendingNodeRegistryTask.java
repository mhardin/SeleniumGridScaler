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

import java.util.Map;

import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.RemoteProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.rmn.qa.AutomationConstants;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationDynamicNode;
import com.rmn.qa.AutomationRunContext;
import com.rmn.qa.RegistryRetriever;
import com.rmn.qa.aws.VmManager;

/**
 * Registry task which registers dynamic {@link AutomationDynamicNode nodes} as they come online
 *
 * @author mhardin
 */
public class AutomationPendingNodeRegistryTask extends AbstractAutomationCleanupTask {

	private static final Logger log = LoggerFactory.getLogger(AutomationPendingNodeRegistryTask.class);
	@VisibleForTesting
	static final String NAME = "Pending Node Registry Task";
	private VmManager vmManager;

	/**
	 * Constructs a registry task with the specified context retrieval mechanism
	 *
	 * @param registryRetriever Represents the retrieval mechanism you wish to use
	 */
	public AutomationPendingNodeRegistryTask(RegistryRetriever registryRetriever, VmManager vmManager) {
		super(registryRetriever);
		this.vmManager = vmManager;
	}

	/**
	 * Returns the ProxySet to be used for cleanup purposes.
	 *
	 * @return
	 */
	protected ProxySet getProxySet() {
		return registryRetriever.retrieveRegistry().getAllProxies();
	}

	@Override
	public String getDescription() {
		return AutomationPendingNodeRegistryTask.NAME;
	}

	// We're going to continuously iterate over registered nodes with the hub.  If they're expired, we're going to mark them for removal.
	// If nodes marked for removal are used into the next billing cycle, then we're going to move their end date back again and put them back
	// into the running queue
	@Override
	public void doWork() {
		ProxySet proxySet = getProxySet();
		if (proxySet != null && !proxySet.isEmpty()) {
			for (RemoteProxy proxy : proxySet) {
				Map<String, Object> config = proxy.getConfig();
				// If the config has an instanceId in it, this means this node was dynamically started and we should
				// track it if we are not already
				if (config.containsKey(AutomationConstants.INSTANCE_ID)) {
					String instanceId = (String) config.get(AutomationConstants.INSTANCE_ID);
					AutomationRunContext context = AutomationContext.getContext();
					// If this node is already in our context, that means we are already tracking this node to terminate
					if (context.pendingNodeExists(instanceId)) {
						log.info(String.format("Pending node %s found in the running state.  Removing from pending set", instanceId));
						context.removePendingNode(instanceId);
					}
				}
			}
		}
		// Remove any pending nodes that haven't come online after a configured amount of time
		AutomationContext.getContext().removeExpiredPendingNodes(vmManager);
	}
}
