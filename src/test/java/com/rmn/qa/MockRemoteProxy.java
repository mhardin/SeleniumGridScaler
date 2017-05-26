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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;
import org.openqa.grid.internal.utils.CapabilityMatcher;
import org.openqa.grid.internal.utils.HtmlRenderer;
import org.openqa.grid.internal.utils.configuration.GridNodeConfiguration;
import org.openqa.selenium.remote.internal.HttpClientFactory;

import com.google.gson.JsonObject;

/**
 * Created by mhardin on 2/6/14.
 */
public class MockRemoteProxy implements RemoteProxy {

    private List<TestSlot> testSlots = new ArrayList<TestSlot>();;
    private GridNodeConfiguration config = new GridNodeConfiguration();
    private CapabilityMatcher matcher;
    private int maxSessions;

    @Override
    public List<TestSlot> getTestSlots() {
        return testSlots;
    }

    public void setTestSlots(List<TestSlot> testSlots) {
        this.testSlots = testSlots;
    }

    public void setMultipleTestSlots(TestSlot testSlot, int count) {
        for(int i=0;i<count;i++) {
            testSlots.add(testSlot);
        }
    }

    @Override
    public Registry getRegistry() {
        return null;
    }

    @Override
    public CapabilityMatcher getCapabilityHelper() {
        return matcher;
    }

    public void setCapabilityMatcher(CapabilityMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public void setupTimeoutListener() {

    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public void teardown() {

    }

    @Override
    public GridNodeConfiguration getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config.custom = config;
    }

    @Override
    public RegistrationRequest getOriginalRegistrationRequest() {
        return null;
    }

    @Override
    public int getMaxNumberOfConcurrentTestSessions() {
        return maxSessions;
    }

    public void setMaxNumberOfConcurrentTestSessions(int sessions) {
        this.maxSessions = sessions;
    }

    @Override
    public URL getRemoteHost() {
        return null;
    }

    @Override
    public TestSession getNewSession(Map<String, Object> requestedCapability) {
        return null;
    }

    @Override
    public int getTotalUsed() {
        return 0;
    }

    @Override
    public HtmlRenderer getHtmlRender() {
        return null;
    }

    @Override
    public int getTimeOut() {
        return 0;
    }

    @Override
    public HttpClientFactory getHttpClientFactory() {
        return null;
    }

    @Override
    public JsonObject getStatus() throws GridException {
        return null;
    }

    @Override
    public boolean hasCapability(Map<String, Object> requestedCapability) {
        return false;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    @Override
    public float getResourceUsageInPercent() {
        return 0;
    }

    @Override
    public int compareTo(RemoteProxy remoteProxy) {
        return 0;
    }

    @Override
    public long getLastSessionStart() {
        return 0;
    }

}