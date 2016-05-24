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

import junit.framework.Assert;
import org.junit.Test;
import org.easymock.EasyMock;
import java.util.Calendar;
import java.util.Date;

public class AutomationUtilsTest {

    @Test
    public void testDifferentCasedEquals() {
        String case1 = "CASE";
        String case2 = case1.toLowerCase();
        Assert.assertTrue("Match should not be case sensitive", AutomationUtils.lowerCaseMatch(case1, case2));
    }

    @Test
    public void testDifferentCasedReversedEquals() {
        String case1 = "CASE";
        String case2 = case1.toLowerCase();
        Assert.assertTrue("Match should not be case sensitive", AutomationUtils.lowerCaseMatch(case2, case1));
    }

    @Test
    public void testWhiteSpaceSensitiveEquals() {
        String case1 = "    foo fighters    ";
        String case2 = "foofighters";
        Assert.assertTrue("Whitespace should be ignored", AutomationUtils.lowerCaseMatch(case1, case2));
    }

    @Test
    public void testDateIsAfter() {
        Date baseDate = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(baseDate);
        c.add(Calendar.SECOND,-10);
        Date afterDate = c.getTime();
        Assert.assertTrue("Date should be considered after", AutomationUtils.isCurrentTimeAfterDate(afterDate,9,Calendar.SECOND));
    }

    @Test
    public void testReadMetaURIResponseWhenNoResponse() throws IOException {
        BufferedReader reader = EasyMock.createMock(BufferedReader.class);
        EasyMock.expect(reader.readLine()).andReturn("NoResponse");
        EasyMock.replay(reader);
        Assert.assertEquals("NoResponse",AutomationUtils.readMetaURIResponse(reader));
    }

    @Test
    public void testReadMetaURIResponse() throws IOException {
        BufferedReader reader = EasyMock.createMock(BufferedReader.class);
        EasyMock.expect(reader.readLine()).andReturn("fake123");
        EasyMock.replay(reader);
        Assert.assertEquals("fake123",AutomationUtils.readMetaURIResponse(reader));
    }

}
