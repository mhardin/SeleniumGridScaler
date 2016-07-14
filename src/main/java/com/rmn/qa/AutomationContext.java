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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context to store a singleton {@link com.rmn.qa.AutomationRunContext AutomationRunContext} object
 * @author mhardin
 */
public class AutomationContext {

    private static final Logger log = LoggerFactory.getLogger(AutomationContext.class);
    private static final int DEFAULT_MAX_THREAD_COUNT = 450;
    private static AutomationRunContext context = new AutomationRunContext();

    // Singleton to maintain a context object

    /**
     * Returns the singleton {@link com.rmn.qa.AutomationRunContext context} instance
     * @return
     */
    public static AutomationRunContext getContext() {
        return AutomationContext.context;
    }

    /**
     * Clears out the previous context.  Used for unit testing
     */
    public static synchronized void refreshContext() {
        AutomationContext.context = new AutomationRunContext();
    }

    static {
        String totalNodeCount = System.getProperty("totalNodeCount");
        // Default to 150 if node count was not passed in
        if(totalNodeCount == null) {
            getContext().setTotalNodeCount(AutomationContext.DEFAULT_MAX_THREAD_COUNT);
            log.warn("Defaulting node count to " + AutomationContext.DEFAULT_MAX_THREAD_COUNT);
        } else {
            getContext().setTotalNodeCount(Integer.parseInt(totalNodeCount));
        }
    }

}
