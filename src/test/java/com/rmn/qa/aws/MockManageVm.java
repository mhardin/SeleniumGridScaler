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

import java.util.Properties;

import org.openqa.selenium.Platform;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;

public class MockManageVm extends AwsVmManager {

    private String userData;
    private AWSCredentials awsCredentials;

    public MockManageVm() {
        super();
    }

    public MockManageVm(AmazonEC2Client client, Properties properties, String region) {
        super(client,properties,region);
    }

    public MockManageVm(AmazonEC2Client client, Properties properties, String region, BasicAWSCredentials credentials) {
        this(client, properties, region);
        this.credentials = credentials;
    }

    @Override
    String getUserData(String uuid, String hubHostName, String browser, Platform platform, int maxSessions) {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public void setCredentials(AWSCredentials basicAWSCredentials) {
        this.credentials = basicAWSCredentials;
    }

    public void setAwsCredentials(AWSCredentials awsCredentials) {
        this.awsCredentials = awsCredentials;
    }
}
