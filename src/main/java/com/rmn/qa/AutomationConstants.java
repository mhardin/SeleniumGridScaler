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

/**
 * Constants in use across various hub/node configs
 * @author mhardin
 */
public interface AutomationConstants {

    // These NODE_CONFIG_* constants represent key names in the node configs
    String CONFIG_CREATED_DATE = "createdDate";
    String CONFIG_MAX_SESSION = "maxSession";
    String CONFIG_BROWSER = "createdBrowser";
    String CONFIG_OS = "createdOs";
    // This is the value that will be in the desired capabilities that our node registers with
    // Runtime value of the hub instance id that gets passed in as a system property.  Also used in the node config
    String INSTANCE_ID = "instanceId";
    // IP address of the hub that will be passed in as a system property
    String IP_ADDRESS = "ipAddress";
    // Test run UUID
    String UUID = "uuid";
    String WINDOWS_PROPERTY_NAME="node.windows.json";
    String LINUX_PROPERTY_NAME="node.linux.json";
    String EXTRA_CAPABILITIES_PROPERTY_NAME="extraCapabilities";
    String AWS_ACCESS_KEY="awsAccessKey";
    String AWS_PRIVATE_KEY="awsSecretKey";
    String AWS_TOKEN="awsToken";
    String AWS_DEFAULT_RESOURCE_NAME= "aws.properties.default";
    String REAPER_THREAD_CONFIG = "useReaperThread";
}
