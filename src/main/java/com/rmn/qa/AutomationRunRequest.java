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

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.CapabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.rmn.qa.task.AbstractAutomationCleanupTask;

/**
 * Represents a run request which will typically be sent in by a test run requesting resources.  Used
 * to encapsulate various run parameters
 * @author mhardin
 */
public final class AutomationRunRequest {

    private static final Logger log = LoggerFactory.getLogger(AbstractAutomationCleanupTask.class);

    private final String uuid;
    private final Integer threadCount;
    private final String browser;
    private final String browserVersion;
    private final Platform platform;
    private final Date createdDate;

    // Require callers to have required variables through constructor below
    private AutomationRunRequest() {
        this(null);
    }

    /**
     * Constructs a run request instance for the specified browser
     * @param browser Browser for the requesting test run
     */
    public AutomationRunRequest(String browser) {
        this(null,null,browser);
    }

    /**
     * Constructs a run request object
     * @param uuid UUID to represent the requesting test run
     * @param threadCount Number of threads for the requesting test run
     * @param browser Browser for the requesting test run
     */
    public AutomationRunRequest(String uuid, Integer threadCount,String browser) {
        this(uuid,threadCount,browser,null,null);
    }

    /**
     * Constructs a run request object
     * @param runUuid UUID to represent the requesting test run
     * @param threadCount Number of threads for the requesting test run
     * @param browser Browser for the requesting test run
     * @param browserVersion Browser version for the requesting test run
     * @param platform Platform for the requesting test run
     */
    public AutomationRunRequest(String runUuid, Integer threadCount, String browser, String browserVersion, Platform platform) {
        this(runUuid, threadCount, browser, browserVersion, platform, new Date());
    }

    /**
     * Constructs a run request object
     * @param runUuid UUID to represent the requesting test run
     * @param threadCount Number of threads for the requesting test run
     * @param browser Browser for the requesting test run
     * @param browserVersion Browser version for the requesting test run
     * @param platform Platform for the requesting test run
     * @param createdDate Date that the test run request was received
     */
    @VisibleForTesting
    public AutomationRunRequest(String runUuid, Integer threadCount, String browser,String browserVersion, Platform platform, Date createdDate) {
        this.uuid = runUuid;
        this.threadCount = threadCount;
        this.browser = browser;
        this.browserVersion = browserVersion;
        this.platform = platform;
        this.createdDate = createdDate;
    }

    /**
     * Generates a AutomationRunRequest object from the capabilities passed in
     * @param capabilities
     * @return
     */
    public static AutomationRunRequest requestFromCapabilities(Map<String,Object> capabilities) {
        String capabilityBrowser = (String)capabilities.get(CapabilityType.BROWSER_NAME);
        String capabilityBrowserVersion = null;
        if(capabilities.containsKey(CapabilityType.VERSION)) {
            capabilityBrowserVersion = (String)capabilities.get(CapabilityType.VERSION);
        }
        Object platform = capabilities.get(CapabilityType.PLATFORM);
        Platform capabilityPlatform = AutomationUtils.getPlatformFromObject(platform);
        return new AutomationRunRequest(null,null,capabilityBrowser,capabilityBrowserVersion,capabilityPlatform);
    }

    /**
     * Returns the UUID for this run request
     * @return
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Returns the thread count requested by this run
     * @return
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * Returns the browser (e.g. 'chrome', 'firefox', etc) for this run request
     * @return
     */
    public String getBrowser() {
        return browser;
    }

    /**
     * Returns the version of the browser (e.g. '27' for Firefox)
     * @return
     */
    public String getBrowserVersion() { return browserVersion; }

    /**
     * Returns the Platform (e.g. 'Platform.WINDOWS')
     * @return
     */
    public Platform getPlatform() { return platform; }
    /**
     * Returns the created date for this run request
     * @return
     */
    public Date getCreatedDate() {
        return createdDate;
    }

    /**
     * Returns true if this run request is less than 2 minutes old, false otherwise
     * @return
     */
    public boolean isNewRun() {
        Calendar c = Calendar.getInstance();
        c.setTime(createdDate);
        c.add(Calendar.MINUTE,2);
        return new Date().before(c.getTime());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Run Request");
        if(!StringUtils.isEmpty(uuid)) {
            builder.append(" - UUID: ").append(uuid);
        }
        if(threadCount != null) {
            builder.append(" - Thread count: ").append(threadCount);
        }
        if(!StringUtils.isEmpty(browser)) {
            builder.append(" - Browser: ").append(browser);
        }
        if(platform != null) {
            builder.append(" - Platform: ").append(platform);
        }
        return builder.toString();
    }

    /**
     * Returns true if this run request matches the capabilities passed in.  Includes browser, browser version, and OS
     * @param capabilities
     * @return
     */
    public boolean matchesCapabilities(Map<String,Object> capabilities) {
        String capabilityBrowser = (String)capabilities.get(CapabilityType.BROWSER_NAME);
        String capabilityBrowserVersion = (String)capabilities.get(CapabilityType.VERSION);
        Object capabilityPlatformObject = capabilities.get(CapabilityType.PLATFORM);
        Platform capabilityPlatform = AutomationUtils.getPlatformFromObject(capabilityPlatformObject);
        if(!AutomationUtils.lowerCaseMatch(browser, capabilityBrowser)) {
            return false;
        }
        if(browserVersion != null && !AutomationUtils.lowerCaseMatch(browserVersion,capabilityBrowserVersion))  {
            return false;
        }
        if(platform != null && !AutomationUtils.firstPlatformCanBeFulfilledBySecondPlatform(platform, capabilityPlatform)) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if this run request matches the run request passed in.  Includes browser, browser version, and OS
     * @param otherRequest
     * @return
     */
    public boolean matchesCapabilities(AutomationRunRequest otherRequest) {
        if(!AutomationUtils.lowerCaseMatch(browser, otherRequest.getBrowser())) {
            return false;
        }
        if(browserVersion != null && browserVersion != otherRequest.getBrowserVersion()) {
            return false;
        }
        if(platform != null && !AutomationUtils.firstPlatformCanBeFulfilledBySecondPlatform(platform, otherRequest.getPlatform())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AutomationRunRequest that = (AutomationRunRequest) o;

        if (!browser.equals(that.browser)) return false;
        if (browserVersion != null ? !browserVersion.equals(that.browserVersion) : that.browserVersion != null)
            return false;
        if (platform != null ? !platform.equals(that.platform) : that.platform != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = browser.hashCode();
        result = 31 * result + (browserVersion != null ? browserVersion.hashCode() : 0);
        result = 31 * result + (platform != null ? platform.hashCode() : 0);
        return result;
    }
}
