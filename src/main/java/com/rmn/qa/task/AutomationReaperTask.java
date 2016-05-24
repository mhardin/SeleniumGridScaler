package com.rmn.qa.task;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.google.common.annotations.VisibleForTesting;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationUtils;
import com.rmn.qa.RegistryRetriever;
import com.rmn.qa.aws.VmManager;

public class AutomationReaperTask extends AbstractAutomationCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(AutomationReaperTask.class);
    @VisibleForTesting static final String NAME = "VM Reaper Task";

    private VmManager ec2;

    /**
     * Constructs a registry task with the specified context retrieval mechanism
     * @param registryRetriever Represents the retrieval mechanism you wish to use
     */
    public AutomationReaperTask(RegistryRetriever registryRetriever,VmManager ec2) {
        super(registryRetriever);
        this.ec2 = ec2;
    }
    @Override
    public void doWork() {
        log.info("Running " + AutomationReaperTask.NAME);
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        Filter filter = new Filter("tag:LaunchSource");
        filter.withValues("SeleniumGridScalerPlugin_"+ AutomationUtils.getHubInstanceId());
        describeInstancesRequest.withFilters(filter);
        List<Reservation> reservations = ec2.describeInstances(describeInstancesRequest);
        for(Reservation reservation : reservations) {
            for(Instance instance : reservation.getInstances()) {
                // Look for orphaned nodes
                Date threshold = AutomationUtils.modifyDate(new Date(),-90, Calendar.MINUTE);
                String instanceId = instance.getInstanceId();
                // If we found a node old enough AND we're not internally tracking it, this means this is an orphaned node and we should terminate it
                if(threshold.after(instance.getLaunchTime()) && !AutomationContext.getContext().nodeExists(instanceId) && instance.getState().getCode() != 48) { // 48 == terminated
                    log.info("Terminating orphaned node: " + instanceId);
                    ec2.terminateInstance(instanceId);
                }
            }
        }
    }

    @Override
    public String getDescription() {
        return AutomationReaperTask.NAME;
    }
}
