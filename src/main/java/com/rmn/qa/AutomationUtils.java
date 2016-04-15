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
        Date targetDate = AutomationUtils.modifyDate(dateToCheck,unitsToCheckWith,unitType);
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

}
