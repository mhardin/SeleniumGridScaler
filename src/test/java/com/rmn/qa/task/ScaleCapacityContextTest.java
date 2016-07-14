package com.rmn.qa.task;

import java.util.Date;

import org.junit.Test;

import com.beust.jcommander.internal.Lists;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationDynamicNode;
import com.rmn.qa.BaseTest;

import junit.framework.Assert;

/**
 * Created by matthew on 3/18/16.
 */
public class ScaleCapacityContextTest extends BaseTest {

	@Test
	public void testCapacityCount() {
		ScaleCapacityContext context = new ScaleCapacityContext();
		AutomationDynamicNode firstNode = new AutomationDynamicNode(null, "foo", null, null, new Date(), 1);
		AutomationDynamicNode secondNode = new AutomationDynamicNode(null, "bar", null, null, new Date(), 3);
		int expectedCount = firstNode.getNodeCapacity() + secondNode.getNodeCapacity();
		context.addAll(Lists.newArrayList(firstNode, secondNode));
		Assert.assertEquals("Total capacity count should be correct", expectedCount, context.getTotalCapacityCount());
	}

	@Test
	public void clearPendingNodes() {
		ScaleCapacityContext context = new ScaleCapacityContext();
		AutomationDynamicNode firstNode = new AutomationDynamicNode(null, "foo", null, null, new Date(), 1);
		AutomationDynamicNode secondNode = new AutomationDynamicNode(null, "bar", null, null, new Date(), 1);
		context.addAll(Lists.newArrayList(firstNode, secondNode));
		Assert.assertEquals("Size should be correct as nodes should exist in context", 2, context.nodesPendingStartup.size());
		context.clearPendingNodes();
		Assert.assertEquals("Size should be empty as nodes should be removed", 0, context.nodesPendingStartup.size());
	}

	@Test
	public void nodesDontClear() {
		ScaleCapacityContext context = new ScaleCapacityContext();
		AutomationDynamicNode firstNode = new AutomationDynamicNode(null, "foo", null, null, new Date(), 1);
		AutomationDynamicNode secondNode = new AutomationDynamicNode(null, "bar", null, null, new Date(), 1);
		context.addAll(Lists.newArrayList(firstNode, secondNode));
		Assert.assertEquals("Size should be correct as nodes should exist in context", 2, context.nodesPendingStartup.size());
		AutomationContext.getContext().addPendingNode(firstNode);
		AutomationContext.getContext().addPendingNode(secondNode);
		context.clearPendingNodes();
		Assert.assertEquals("Size should be the same as nodes should exist in context", 2, context.nodesPendingStartup.size());
	}

}
