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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rmn.qa.RegistryRetriever;

/**
 * Base task class that has exception/running handling for other tasks to extend with their
 * own implementations
 * @author mhardin
 */
public abstract class AbstractAutomationCleanupTask extends Thread {

    private static final Logger log = LoggerFactory.getLogger(AbstractAutomationCleanupTask.class);

    protected RegistryRetriever registryRetriever;
    private Throwable t;

    public AbstractAutomationCleanupTask(RegistryRetriever registryRetriever) {
        this.registryRetriever = registryRetriever;
    }

    @Override
    public void run() {
        try{
            this.doWork();
        }catch(Throwable t) {
            this.t = t;
            log.error(String.format("Error executing cleanup thread [%s]: %s", getDescription(), t), t);
        }
    }

    /**
     * Performs the work of the task
     */
    public abstract void doWork();

    /**
     * Returns the description of the task
     * @return
     */
    public abstract String getDescription();

    /**
     * Returns the exception of this task if any was encountered
     * @return
     */
    public Throwable getThrowable() {
        return t;
    }

}
