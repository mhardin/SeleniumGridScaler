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
package com.rmn.qa.aws;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.BrowserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.google.common.annotations.VisibleForTesting;
import com.rmn.qa.AutomationConstants;
import com.rmn.qa.AutomationUtils;
import com.rmn.qa.BrowserPlatformPair;
import com.rmn.qa.NodesCouldNotBeStartedException;

/**
 * @author  mhardin
 */
public class AwsVmManager implements VmManager {

    private static final Logger log = LoggerFactory.getLogger(AwsVmManager.class);
    public static final DateFormat NODE_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    public static final Platform DEFAULT_PLATFORM = Platform.UNIX;
    private AmazonEC2Client client;
    @VisibleForTesting AWSCredentials credentials;
    private Properties awsProperties;
    public static final int CHROME_THREAD_COUNT = 15;
    public static final int FIREFOX_IE_THREAD_COUNT = 24;

    private String region;


    static {
        // Read and write dates from node config in UTC format
        NODE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Creates a new AwsVMManager instance using
     * the reagion from the properties file
     */
    public AwsVmManager() {
        awsProperties = initAWSProperties();
        this.region = awsProperties.getProperty("region");
        /**
         * By default we use the credentials provided in the configuration files.
         * If there are none we fall back to IAM roles.
         */
        try {
            credentials = getCredentials();
            client = new AmazonEC2Client(credentials);
        } catch (IllegalArgumentException e) {
            log.info("Falling back to IAM roles for authorization since no credentials provided in system properties", e);
            client = new AmazonEC2Client();
        }
        AwsVmManager.setRegion(client, awsProperties, region);
    }

    public static void setRegion(AmazonEC2Client client, Properties awsProperties, String region) {
        client.setEndpoint(awsProperties.getProperty(region + "_endpoint"));
    }

    /**
     * Creates a new AwsVmManager instance.
     *
     * @param  client {@link com.amazonaws.services.ec2.AmazonEC2Client Client} to use for AWS interaction
     * @param  properties {@link java.util.Properties Properties} to use for EC2 config loading
     * @param  region      Region inside of AWS to use
     */
    public AwsVmManager(final AmazonEC2Client client, final Properties properties, final String region) {
        this.client = client;
        this.awsProperties = properties;
        this.region = region;
    }

    /**
     * Initializes the AWS properties from the default properties file. Allows the user to override the default
     * properties if desired
     *
     * @return
     */
    Properties initAWSProperties() {
        Properties properties = new Properties();
        String propertiesLocation = System.getProperty("propertyFileLocation");

        // If the user passed in an AWS config file, go ahead and use it instead of the default one
        if (propertiesLocation != null) {
            File f = new File(propertiesLocation);
            try {
                InputStream is = new FileInputStream(f);
                properties.load(is);
            } catch (IOException e) {
                throw new RuntimeException("Could not load custom aws.properties", e);
            }
        } else {
            InputStream stream = AwsVmManager.class.getClassLoader().getResourceAsStream(
                    AutomationConstants.AWS_DEFAULT_RESOURCE_NAME);
            try {
                properties.load(stream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return properties;
    }

    @VisibleForTesting
    AmazonEC2Client getClient() {
        return client;
    }

    protected Properties getAwsProperties() {
        return awsProperties;
    }

    /**
     * Retrieves AWS {@link com.amazonaws.auth.BasicAWSCredentials credentials} from the configuration file.
     *
     * @return
     */
    @VisibleForTesting
    AWSCredentials getCredentials() {
        Properties awsProperties = getAwsProperties();
        // Give the system property credentials precedence over ones found in the config file
        String accessKey = System.getProperty(AutomationConstants.AWS_ACCESS_KEY);
        if (accessKey == null) {
            accessKey = awsProperties.getProperty(AutomationConstants.AWS_ACCESS_KEY);
            if (accessKey == null) {
                throw new IllegalArgumentException(String.format(
                        "AWS Access Key must be passed in by the [%s] system property or be present in the AWS config file",
                        AutomationConstants.AWS_ACCESS_KEY));
            }
        }

        String privateKey = System.getProperty(AutomationConstants.AWS_PRIVATE_KEY);
        if (privateKey == null) {
            privateKey = awsProperties.getProperty(AutomationConstants.AWS_PRIVATE_KEY);
            if (privateKey == null) {
                throw new IllegalArgumentException(String.format(
                        "AWS Private Key must be passed in by the [%s] system property or be present in the AWS config file",
                        AutomationConstants.AWS_PRIVATE_KEY));
            }
        }

        // Token is not required, so do not throw an exception if it is not present
        String token = System.getProperty(AutomationConstants.AWS_TOKEN);
        if (token == null) {
            token = awsProperties.getProperty(AutomationConstants.AWS_TOKEN);
        }

        return new BasicSessionCredentials(accessKey, privateKey, token);
    }

    public List<Instance> launchNodes(final String amiId, final String instanceType, final int numberToStart,
            final String userData, final boolean terminateOnShutdown) throws NodesCouldNotBeStartedException {
        RunInstancesRequest runRequest = new RunInstancesRequest();
        runRequest.withImageId(amiId).withInstanceType(instanceType).withMinCount(numberToStart)
                  .withMaxCount(numberToStart).withUserData(userData);
        if (terminateOnShutdown) {
            runRequest.withInstanceInitiatedShutdownBehavior("terminate");
        }

        log.info("Setting image id: " + runRequest.getImageId());
        log.info("Setting instance type: " + runRequest.getInstanceType());

        Properties awsProperties = getAwsProperties();
        String subnetKey = awsProperties.getProperty(region + "_subnet_id");
        if (subnetKey != null) {
            log.info("Setting subnet: " + subnetKey);
            runRequest.withSubnetId(subnetKey);
        }

        String securityGroupKey = awsProperties.getProperty(region + "_security_group");
        if (securityGroupKey != null) {

            String[] splitSecurityGroupdIds = securityGroupKey.split(",");

            List securityGroupIdsAryLst = new ArrayList();
            for (int i = 0; i < splitSecurityGroupdIds.length; i++) {

                log.info("Setting security group(s): " + splitSecurityGroupdIds[i]);
                securityGroupIdsAryLst.add(splitSecurityGroupdIds[i]);
            }
            runRequest.setSecurityGroupIds(securityGroupIdsAryLst);
        }

        String keyName = awsProperties.getProperty(region + "_key_name");
        if (keyName != null) {
            log.info("Setting keyname:" + keyName);
            runRequest.withKeyName(keyName);
        }

        log.info("Sending run request to AWS...");

        RunInstancesResult runInstancesResult = getResults(runRequest, 0);
        log.info("Run request result returned.  Adding tags");

        // Tag the instances with the standard RMN AWS data
        List<Instance> instances = runInstancesResult.getReservation().getInstances();
        if (instances.size() == 0) {
            throw new NodesCouldNotBeStartedException(String.format(
                    "Error starting up nodes -- count was zero and did not match expected count of %d", numberToStart));
        }

        associateTags(new Date().toString(), instances);
        return instances;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Instance> launchNodes(final String uuid, Platform platform, final String browser, final String hubHostName,
                                      final int nodeCount, final int maxSessions) throws NodesCouldNotBeStartedException {
        // If platform is null or ANY, go ahead and default to any
        if (platform == null) {
            platform = Platform.ANY;
        }
        BrowserPlatformPair browserPlatformPair = new BrowserPlatformPair(browser, platform);
        if (!AutomationUtils.browserAndPlatformSupported(browserPlatformPair)) {
            throw new RuntimeException("Unsupported browser/platform: " + browserPlatformPair);
        }
        // After we have verified the platform is supported, go ahead and set it to the default platform
        if (platform == platform.ANY) {
            platform = DEFAULT_PLATFORM;
        }
        String userData = getUserData(uuid, hubHostName, browser, platform, maxSessions);
        Properties awsProperties = getAwsProperties();
        String amiId = awsProperties.getProperty(getAmiIdForOs(platform, browser));
        String instanceType = awsProperties.getProperty("node_instance_type_" + browser);
        return this.launchNodes(amiId, instanceType, nodeCount, userData, false);
    }

    /**
     * Attempts to run the {@link com.amazonaws.services.ec2.model.RunInstancesRequest RunInstancesRequest}, falling
     * back on alternative subnets if capacity is full in the current region.
     *
     * @param   request
     * @param   requestNumber
     *
     * @return
     *
     * @throws  NodesCouldNotBeStartedException
     */
    private RunInstancesResult getResults(final RunInstancesRequest request, int requestNumber)
        throws NodesCouldNotBeStartedException {
        RunInstancesResult runInstancesResult;
        try {
            AmazonEC2Client localClient = getClient();
            if(localClient == null){
                throw new RuntimeException("The client is not initialized");
            }
            runInstancesResult = localClient.runInstances(request);
        } catch (AmazonServiceException e) {

            // If there is insufficient capacity in this subnet / availability zone, then we want to try other
            // configured subnets
            if ("InsufficientInstanceCapacity".equals(e.getErrorCode())
                    || "VolumeTypeNotAvailableInZone".equals(e.getErrorCode())) {
                log.error(String.format("Insufficient capacity in subnet [%s]: %s", request.getSubnetId(), e));
                requestNumber = requestNumber + 1;

                Properties awsProperties = getAwsProperties();
                String fallBackSubnetId = awsProperties.getProperty(region + "_subnet_fallback_id_" + requestNumber);

                // Make sure and only try to recursively loop so as long as we have a valid fallback subnet id.  Logic
                // to also
                // prevent an accidental infinite loop
                if (fallBackSubnetId != null && requestNumber < 5) {
                    log.info("Setting fallback subnet: " + fallBackSubnetId);

                    // Modify the original request with the new subnet ID we're trying to fallback on
                    request.withSubnetId(fallBackSubnetId);
                } else {
                    throw new NodesCouldNotBeStartedException(
                        "Sufficient resources were not available in any of the availability zones");
                }

                return getResults(request, requestNumber);
            } else {

                // We got an error other than insufficient capacity, and should just throw it for the caller to handle
                throw e;
            }
        }

        return runInstancesResult;
    }

    /**
     * Assigns the tags asynchronously to AWS.
     *
     * @param  threadName
     * @param  instances
     */
    @VisibleForTesting
    void associateTags(final String threadName, final Collection<Instance> instances) {
        Thread reportThread = new AwsTagReporter(threadName, getClient(), instances, getAwsProperties());
        reportThread.start();
    }

    /**
     * Gets the instance ID based on the OS that is chosen.
     *
     * @param   platform       OS for the requested test run
     * @param   browser  Browser for the requested test run
     *
     * @return
     */
    private String getAmiIdForOs(Platform platform, final String browser) {
        String requestedProperty;
        if (AutomationUtils.isPlatformWindows(platform) || browser.equals(BrowserType.IE)) {
            // We only want to run on Windows if the caller is specifically asking for it,
            // or if they want to run in IE (only supported on Windows)
            requestedProperty = region + "_windows_node_ami";
        } else if (AutomationUtils.isPlatformUnix(platform)) {
            requestedProperty = region + "_linux_node_ami";
        } else {
            throw new RuntimeException("Unsupported OS: " + platform);
        }

        return requestedProperty;
    }

    /**
     * Terminates the specified instance.
     *
     * @param  instanceId  Id of the instance to terminate
     */
    public boolean terminateInstance(final String instanceId) {
        TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest();
        terminateRequest.withInstanceIds(instanceId);

        AmazonEC2Client localClient = getClient();
        if(localClient == null){
            throw new RuntimeException("The client is not initialized");
        }
        TerminateInstancesResult result;
        try{
            result = localClient.terminateInstances(terminateRequest);
        } catch(AmazonServiceException ase) {
            // If the node was terminated outside of this plugin, handle the error appropriately
            if (ase.getErrorCode().equals("InvalidInstanceID.NotFound")) {
                log.error("Node not found when attempting to remove: " + instanceId);
                return false;
            } else {
                throw ase;
            }
        }
        List<InstanceStateChange> stateChanges = result.getTerminatingInstances();
        boolean terminatedInstance = false;
        for (InstanceStateChange stateChange : stateChanges) {
            if (instanceId.equals(stateChange.getInstanceId())) {
                terminatedInstance = true;

                InstanceState currentState = stateChange.getCurrentState();
                if (currentState.getCode() != 32 && currentState.getCode() != 48) {
                    log.error(String.format(
                            "Machine state for id %s should be terminated (48) or shutting down (32) but was %s instead",
                            instanceId, currentState.getCode()));
                    return false;
                }
            }
        }

        if (!terminatedInstance) {
            log.error("Matching terminated instance was not found for instance " + instanceId);
            return false;
        }

        log.info(String.format("Node [%s] successfully terminated", instanceId));
        return true;
    }

    @Override
    public List<Reservation> describeInstances(final DescribeInstancesRequest describeInstancesRequest) {
        return getClient().describeInstances(describeInstancesRequest).getReservations();
    }

    /**
     * Returns a zip file containing the necessary user data for the images we're going to spin up.
     *
     * @param   uuid         UUID of the test run
     * @param   hubHostName  Resolvable host name of the hub the node will register with
     * @param   browser      Browser for the requested test run
     * @param   platform           OS for the requested test run
     * @param   maxSessions  Maximum simultaneous test sessions
     *
     * @return
     */
    @VisibleForTesting
    String getUserData(final String uuid, final String hubHostName, final String browser, final Platform platform,
            final int maxSessions) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(outputStream);
        ) {

            // Pull the node config out so we can write it to the zip file
            ZipEntry nodeConfigZipEntry = new ZipEntry("nodeConfigTemplate.json");
            zos.putNextEntry(nodeConfigZipEntry);

            String nodeConfigContents = getNodeConfig(uuid, hubHostName, browser, platform, maxSessions);
            zos.write(nodeConfigContents.getBytes());
            zos.closeEntry();

            // Now pull out the S3 config file so we can include it in the zip
            // that will be sent in as userdata
            ZipEntry s3ZipEntry = new ZipEntry(".s3cfg");
            zos.putNextEntry(s3ZipEntry);

            String s3Contents = getS3Config();
            zos.write(s3Contents.getBytes());
            zos.closeEntry();

            // Make sure and close the zip before encoding it
            zos.close();
            return new String(Base64.encodeBase64(outputStream.toByteArray()));

        } catch (IOException ex) {
            throw new RuntimeException("Error getting user data", ex);
        }
    }

    /**
     * Reads the hub.json file and returns its contents as a Base64-encoded string.
     *
     * @return
     */
    @VisibleForTesting
    String getNodeConfig(final String uuid, final String hostName, final String browser, final Platform platform,
                         final int maxSessions) {
        String resourceName;
        if (AutomationUtils.isPlatformWindows(platform)) {
            resourceName = AutomationConstants.WINDOWS_PROPERTY_NAME;
        } else if (AutomationUtils.isPlatformUnix(platform)) {
            resourceName = AutomationConstants.LINUX_PROPERTY_NAME;
        } else {
            throw new RuntimeException("Unexpected OS for prop config: " + platform);
        }

        String nodeConfig = getFileContents(resourceName);
        nodeConfig = nodeConfig.replaceAll("<MAX_SESSION>", String.valueOf(maxSessions));
        nodeConfig = nodeConfig.replaceAll("<MAX_SESSION_FIREFOX>",
                String.valueOf(AwsVmManager.FIREFOX_IE_THREAD_COUNT));
        nodeConfig = nodeConfig.replaceAll("<MAX_SESSION_IE>", String.valueOf(AwsVmManager.FIREFOX_IE_THREAD_COUNT));
        nodeConfig = nodeConfig.replaceAll("<MAX_SESSION_CHROME>", String.valueOf(AwsVmManager.CHROME_THREAD_COUNT));
        nodeConfig = nodeConfig.replaceAll("<UUID>", uuid);
        nodeConfig = nodeConfig.replaceAll("<CREATED_BROWSER>", browser);
        nodeConfig = nodeConfig.replaceAll("<CREATED_OS>", platform.toString());

        Date createdDate = Calendar.getInstance().getTime();

        // Pass in the created date so we can know when this node was spun up
        nodeConfig = nodeConfig.replaceAll("<CREATED_DATE>", AwsVmManager.NODE_DATE_FORMAT.format(createdDate));
        nodeConfig = nodeConfig.replaceFirst("<HOST_NAME>", hostName);
        return nodeConfig;
    }

    /**
     * Returns the S3 config file replaced with the appropriate AWS key/secret.
     *
     * @return
     */
    @VisibleForTesting
    String getS3Config() {
        String s3Config = getFileContents(".s3cfg");
        if (credentials != null) {
            s3Config = s3Config.replaceAll("<ACCESS_KEY>", credentials.getAWSAccessKeyId());
            s3Config = s3Config.replaceAll("<SECRET_KEY>", credentials.getAWSSecretKey());
        }
        return s3Config;
    }

    /**
     * Returns the contents of the specified resource as a string.
     *
     * @param   resourceName
     *
     * @return
     */
    private static String getFileContents(final String resourceName) {
        String fileContents = "";
        try {
            InputStream stream = AwsVmManager.class.getClassLoader().getResourceAsStream(resourceName);
            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                fileContents += strLine + "\n";
            }

            stream.close();
        } catch (IOException e) {
            throw new RuntimeException("Error loading resource: -" + resourceName, e);
        }

        return fileContents;
    }
}
