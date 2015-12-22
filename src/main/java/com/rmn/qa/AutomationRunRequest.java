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

import com.google.common.annotations.VisibleForTesting;

/**
 * Represents a run request which will typically be sent in by a test run requesting resources.  Used
 * to encapsulate various run parameters
 * @author mhardin
 */
public final class AutomationRunRequest {

    private final String uuid;
    private final Integer threadCount;
    private final String browser;
    private final String browserVersion;
    private final String os;
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
     * @param os OS for the requesting test run
     */
    public AutomationRunRequest(String runUuid, Integer threadCount, String browser, String browserVersion, String os) {
        this(runUuid, threadCount, browser, browserVersion, os, new Date());
    }

    /**
     * Constructs a run request object
     * @param runUuid UUID to represent the requesting test run
     * @param threadCount Number of threads for the requesting test run
     * @param browser Browser for the requesting test run
     * @param browserVersion Browser version for the requesting test run
     * @param os OS for the requesting test run
     * @param createdDate Date that the test run request was received
     */
    @VisibleForTesting
    public AutomationRunRequest(String runUuid, Integer threadCount, String browser,String browserVersion, String os, Date createdDate) {
        this.uuid = runUuid;
        this.threadCount = threadCount;
        this.browser = browser;
        this.browserVersion = browserVersion;
        this.os = os;
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
        String capabilityOs = (String)capabilities.get(CapabilityType.PLATFORM);
        return new AutomationRunRequest(null,null,capabilityBrowser,capabilityBrowserVersion,capabilityOs);
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
     * Returns the OS (e.g. 'linux')
     * @return
     */
    public String getOs() { return os; }
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
        if(!StringUtils.isEmpty(os)) {
            builder.append(" - OS: ").append(os);
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
        Object platform = capabilities.get(CapabilityType.PLATFORM);
        String capabilityOs;
        if (platform instanceof Platform) {
            capabilityOs = ((Platform) platform).getPartOfOsName()[0];
        } else {
            capabilityOs = (String)platform;
        }
        if(!AutomationUtils.lowerCaseMatch(browser, capabilityBrowser)) {
            return false;
        }
        if(browserVersion != null && !AutomationUtils.lowerCaseMatch(browserVersion,capabilityBrowserVersion))  {
            return false;
        }
        if(os != null && !AutomationUtils.lowerCaseMatch(os, capabilityOs)) {
            // If either OS has 'ANY' for the platform, that means it should be a match regardless and we don't have to count this as a non-match
            if(!AutomationUtils.lowerCaseMatch(os,"any") && !AutomationUtils.lowerCaseMatch(capabilityOs,"any")) {
                return false;
            }
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
        if(os != null && !AutomationUtils.lowerCaseMatch(os, otherRequest.getOs())) {
            // If either OS has 'ANY' for the platform, that means it should be a match regardless and we don't have to count this as a non-match
            if(!AutomationUtils.lowerCaseMatch(os,"any") && !AutomationUtils.lowerCaseMatch(otherRequest.getOs(),"any")) {
                return false;
            }
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
        if (os != null ? !os.equals(that.os) : that.os != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = browser.hashCode();
        result = 31 * result + (browserVersion != null ? browserVersion.hashCode() : 0);
        result = 31 * result + (os != null ? os.hashCode() : 0);
        return result;
    }
}
