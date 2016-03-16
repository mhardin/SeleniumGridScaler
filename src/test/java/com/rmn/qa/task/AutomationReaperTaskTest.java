package com.rmn.qa.task;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationDynamicNode;
import com.rmn.qa.AutomationUtils;
import com.rmn.qa.MockVmManager;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class AutomationReaperTaskTest {

    @Test
    public void testShutdown() {
        MockVmManager ec2 = new MockVmManager();
        Reservation reservation = new Reservation();
        Instance instance = new Instance();
        String instanceId = "foo";
        instance.setInstanceId(instanceId);
        instance.setLaunchTime(AutomationUtils.modifyDate(new Date(),-5,Calendar.HOUR));
        reservation.setInstances(Arrays.asList(instance));
        ec2.setReservations(Arrays.asList(reservation));
        AutomationReaperTask task = new AutomationReaperTask(null,ec2);
        task.run();
        Assert.assertTrue("Node should be terminated as it was empty", ec2.isTerminated());
    }

    @Test
    // Tests that a node that is not old enough is not terminated
    public void testNoShutdownTooRecent() {
        MockVmManager ec2 = new MockVmManager();
        Reservation reservation = new Reservation();
        Instance instance = new Instance();
        String instanceId = "foo";
        instance.setInstanceId(instanceId);
        instance.setLaunchTime(AutomationUtils.modifyDate(new Date(),-15,Calendar.MINUTE));
        reservation.setInstances(Arrays.asList(instance));
        ec2.setReservations(Arrays.asList(reservation));
        AutomationReaperTask task = new AutomationReaperTask(null,ec2);
        task.run();
        Assert.assertFalse("Node should NOT be terminated as it was not old", ec2.isTerminated());
    }

    @Test
    // Tests that a node that is being tracked internally is not shut down
    public void testNoShutdownNodeTracked() {
        MockVmManager ec2 = new MockVmManager();
        Reservation reservation = new Reservation();
        Instance instance = new Instance();
        String instanceId = "foo";
        AutomationContext.getContext().addNode(new AutomationDynamicNode("faky",instanceId,null,null,new Date(),1));
        instance.setInstanceId(instanceId);
        instance.setLaunchTime(AutomationUtils.modifyDate(new Date(),-5,Calendar.HOUR));
        reservation.setInstances(Arrays.asList(instance));
        ec2.setReservations(Arrays.asList(reservation));
        AutomationReaperTask task = new AutomationReaperTask(null,ec2);
        task.run();
        Assert.assertFalse("Node should NOT be terminated as it was tracked internally", ec2.isTerminated());
    }

    @Test
    // Tests that the hardcoded name of the task is correct
    public void testTaskName() {
        AutomationReaperTask task = new AutomationReaperTask(null,null);
        Assert.assertEquals("Name should be the same",AutomationReaperTask.NAME, task.getDescription()  );
    }

    @Test
    public void testNodeMatch(){
        Reservation reservation = new Reservation();
        Instance instance = new Instance();
        String instanceId = "foo";
        instance.setInstanceId(instanceId);
        instance.setLaunchTime(AutomationUtils.modifyDate(new Date(),-5,Calendar.HOUR));
        AutomationContext.getContext().addNode(new AutomationDynamicNode("faky",instanceId,null,null,new Date(),1));
        List<Tag> tags = new ArrayList<>();
        Tag nodeTag = new Tag("LaunchSource","SeleniumGridScalerPlugi_" + instanceId);
        tags.add(nodeTag);
        instance.setTags(tags);
        reservation.setInstances(Arrays.asList(instance));
        String expectedTag = "Tags: [{Key: LaunchSource,Value: SeleniumGridScalerPlugi_foo}]";
        Assert.assertTrue("The node tag should match!",reservation.toString().contains(expectedTag));
    }

    @Test
    public void testNodeDoNotMatch(){
        Reservation reservation = new Reservation();
        Instance instance = new Instance();
        String instanceId = "foo";
        instance.setInstanceId(instanceId);
        instance.setLaunchTime(AutomationUtils.modifyDate(new Date(),-5,Calendar.HOUR));
        AutomationContext.getContext().addNode(new AutomationDynamicNode("faky",instanceId,null,null,new Date(),1));
        List<Tag> tags = new ArrayList<>();
        Tag nodeTag = new Tag("LaunchSource","SeleniumGridScalerPlugi_" + instanceId);
        tags.add(nodeTag);
        instance.setTags(tags);
        reservation.setInstances(Arrays.asList(instance));
        String expectedTag = "Tags: [{Key: LaunchSource,Value: SeleniumGridScalerPlugi_nofoo}]";
        reservation.setInstances(Arrays.asList(instance));
        Assert.assertFalse("The node tag should not match!",reservation.toString().contains(expectedTag));
    }
}
