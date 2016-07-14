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

package com.rmn.qa.task;

import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.RemoteProxy;

import com.rmn.qa.RegistryRetriever;
import com.rmn.qa.RequestMatcher;
import com.rmn.qa.aws.VmManager;

public class MockAutomationNodeCleanupTask extends AutomationNodeCleanupTask {

    private ProxySet proxySet;
    private boolean proxyRemoved = false;

    public MockAutomationNodeCleanupTask(RegistryRetriever retrieveContext, VmManager ec2, RequestMatcher requestMatcher) {
        super(retrieveContext, ec2, requestMatcher);
    }

    @Override
    protected ProxySet getProxySet() {
        return proxySet;
    }

    public void setProxySet(ProxySet proxySet) {
        this.proxySet = proxySet;
    }

    @Override
    protected void removeProxy(RemoteProxy proxy) {
        proxyRemoved = true;
    }

    public boolean isProxyRemoved() {
        return proxyRemoved;
    }
}
