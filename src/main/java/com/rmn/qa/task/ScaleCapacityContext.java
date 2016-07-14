package com.rmn.qa.task;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.beust.jcommander.internal.Lists;
import com.google.common.annotations.VisibleForTesting;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationDynamicNode;

/**
 * Created by matthew on 6/23/16.
 */
public class ScaleCapacityContext {

	@VisibleForTesting
	List<AutomationDynamicNode> nodesPendingStartup = Lists.newArrayList();

	public int getTotalCapacityCount() {
		return nodesPendingStartup.stream().mapToInt(AutomationDynamicNode::getNodeCapacity).sum();
	}

	public void addAll(Collection<AutomationDynamicNode> nodes) {
		nodesPendingStartup.addAll(nodes);
	}

	/**
	 * Clears out any nodes that are no longer in the 'pending' state
	 */
	public void clearPendingNodes() {
		Iterator<AutomationDynamicNode> iterator = nodesPendingStartup.iterator();
		while(iterator.hasNext()) {
			AutomationDynamicNode pendingNode = iterator.next();
			if (!AutomationContext.getContext().pendingNodeExists(pendingNode.getInstanceId())) {
				iterator.remove();
			}
		}
	}

	@Override
	public String toString() {
		return "ScaleCapacityContext{" +
				"nodesPendingStartup=" + nodesPendingStartup +
				'}';
	}
}
