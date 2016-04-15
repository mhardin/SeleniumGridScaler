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

import com.rmn.qa.RegistryRetriever;

/**
 * Created by mhardin on 4/24/14.
 */
public class MockAutomationOrphanedNodeRegistryTask extends AutomationOrphanedNodeRegistryTask {

    private ProxySet proxySet;

    public MockAutomationOrphanedNodeRegistryTask(RegistryRetriever retrieveContext) {
        super(retrieveContext);
    }

    public void setProxySet(ProxySet proxySet) {
        this.proxySet = proxySet;
    }

    @Override
    public ProxySet getProxySet() {
        return proxySet;
    }
}
