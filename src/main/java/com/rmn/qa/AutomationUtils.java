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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;

import com.rmn.qa.aws.AwsTagReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.BrowserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util methods
 * @author mhardin
 */
public final class AutomationUtils {

    /**
     * Modifies the specified date
     * @param dateToModify Date to modify
     * @param unitsToModify Number of units to modify (e.g. 6 for 6 seconds)
     * @param unitType Measurement type (e.g. Calendar.SECONDS)
     * @return Modified date
     */
    private static final Logger log = LoggerFactory.getLogger(AwsTagReporter.class);
    private static int AWS_METADATA_TIMEOUT = 3 * 1000;
    private static String AWS_INSTANCE_METADATA_URI = "http://169.254.169.254/latest/meta-data";

    public static Date modifyDate(Date dateToModify,int unitsToModify,int unitType) {
        Calendar c = Calendar.getInstance();
        c.setTime(dateToModify);
        // Add 60 seconds so we're as close to the hour as we can be instead of adding 55 again
        c.add(unitType,unitsToModify);
        return c.getTime();
    }

    /**
     * Returns true if the current time is after the specified date, false otherwise
     * @param dateToCheck Date to check against the current time
     * @param unitsToCheckWith Number of units to add/subtract from dateToCheck
     * @param unitType Unit type (e.g. Calendar.MINUTES)
     * @return
     */
    public static boolean isCurrentTimeAfterDate(Date dateToCheck,int unitsToCheckWith,int unitType) {
        Date targetDate = AutomationUtils.modifyDate(dateToCheck, unitsToCheckWith, unitType);
        return new Date().after(targetDate);
    }

    /**
     * Returns true if the strings are lower case equal
     * @param string1 First string to compare
     * @param string2 Second string to compare
     * @return
     */
    public static boolean lowerCaseMatch(String string1, String string2) {
        string2 = string2.toLowerCase().replace(" ","");
        return string2.equals(string1.toLowerCase().replace(" ", ""));
    }

    /**
     * Returns AWS instanceId of the hub, returns "NoResponse" if times out
     * @return
     */
    public static String getHubInstanceId()
    {
        String hubInstanceId="NoResponse";

        try {
            URL url = new URL(AWS_INSTANCE_METADATA_URI + "/instance-id");
            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(AWS_METADATA_TIMEOUT);
            urlConnection.setReadTimeout(AWS_METADATA_TIMEOUT);
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
            hubInstanceId=readMetaURIResponse(reader);

        } catch (Exception e) {
            log.warn("Cannot create unique tag name using the Hub's instanceId, if you use multiple Hub,this may result in grid nodes being terminated by other hub(s)");
            log.info("Exception while retrieving the instanceId of the hub via metadata uri:" + e);

        }

        return hubInstanceId;
    }

    /**
     * Returns the value in the BufferReader, "NoResponse" if response is empty
     * @param reader Buffer reader of AWS META DATA URI
     * @return
     */
    public static String readMetaURIResponse (BufferedReader reader)
    {
        String line = "NoResponse";
        try {

            while ((line = reader.readLine()) != null) {

                return line;
            }

        } catch (Exception e) {

            log.info("Exception while reading the response of AWS META DATA URI: " + e);

        }
        return line;
    }

     /* Returns the Selenium platform object from an unknown object, which could be a string or an actual object.
     * If the platform can not be determined, null will be returned
     * @param platformObj
     * @return
     */
    public static Platform getPlatformFromObject(Object platformObj) {
        if (platformObj == null) {
            return null;
        }
        Platform parsedPlatform = null;
        if (platformObj instanceof Platform) {
            parsedPlatform = ((Platform) platformObj);
        } else if (platformObj instanceof String) {
            String platformString = (String) platformObj;
            if (StringUtils.isEmpty(platformString)) {
                return null;
            }
            try {
                parsedPlatform = Platform.fromString(platformString);
            } catch (WebDriverException e) {
                log.error("Error parsing out platform for string: " + platformObj, e);
            }
        } else {
            // Do nothing and return null for unexpected type
            log.warn("Platform was not an expected type: " + platformObj);
        }
        return parsedPlatform;
    }

    /**
     * Returns true if the requested browser and platform can be used within AMIs, and false otherwise
     * @param browserPair
     * @return
     */
    public static boolean browserAndPlatformSupported(BrowserPlatformPair browserPair) {
        // If no platform is defined or the user specifies linux, perform the browser check.
        if(AutomationUtils.isPlatformUnix(browserPair.getPlatform())){
            return     lowerCaseMatch(BrowserType.CHROME,browserPair.getBrowser())
                    || lowerCaseMatch(BrowserType.FIREFOX,browserPair.getBrowser());
        } else if (browserPair.getPlatform() == Platform.ANY || AutomationUtils.isPlatformWindows(browserPair.getPlatform())) {
            return lowerCaseMatch(BrowserType.CHROME, browserPair.getBrowser())
                    || lowerCaseMatch(BrowserType.FIREFOX, browserPair.getBrowser())
                    || lowerCaseMatch("internetexplorer", browserPair.getBrowser());
        } else {
            return false;
        }
    }

    /**
     * Returns true if the platform is from the Windows family
     * @param platform
     * @return
     */
    public static boolean isPlatformWindows(Platform platform) {
        return getUnderlyingFamily(platform) == Platform.WINDOWS;
    }

    /**
     * Returns true if the platform is from the Unix family
     * @param platform
     * @return
     */
    public static boolean isPlatformUnix(Platform platform) {
        return getUnderlyingFamily(platform) == Platform.UNIX;
    }

    /**
     * Returns true if the platforms match.  If either platform is Platform.ANY, this will return true.
     * @param platformOne
     * @param platformTwo
     * @return
     */
    // Even though we're handling null in here, this shouldn't really happen unless someone has a mis-configured test request or
    // manually connects a node to this hub with a mis-configured JSON file.  Unfortunately, there is not a lot we can do to prevent
    // this besides handling it here
    public static boolean firstPlatformCanBeFulfilledBySecondPlatform(Platform platformOne, Platform platformTwo) {
        // If either platform is ANY, this means this request should match
        if (platformOne == Platform.ANY || platformTwo == Platform.ANY) {
            return true;
        } else if (platformOne != null && platformTwo == null) {
            // If the first platform is requesting a specific platform, and the 2nd platform is null,
            // return false for a match as we can't determine if they do in fact match
            return false;
        } else if (platformOne == null) {
            return true;
        } else {
            // At the end of the day, we only support basic platforms (Windows or Linux), so group
            // each platform into the family and compare
            Platform platformOneFamily = getUnderlyingFamily(platformOne);
            Platform platformTwoFamily = getUnderlyingFamily(platformTwo);
            return platformOneFamily == platformTwoFamily;
        }
    }

    /**
     * Returns the underlying 'family' of the specified platform
     * @param platform
     * @return
     */
    public static Platform getUnderlyingFamily(Platform platform) {
        if (platform == null) {
            return null;
        }
        if (platform == Platform.UNIX || platform == Platform.WINDOWS || platform == Platform.MAC || platform == Platform.ANY) {
            return platform;
        } else {
            return getUnderlyingFamily(platform.family());
        }
    }
}
