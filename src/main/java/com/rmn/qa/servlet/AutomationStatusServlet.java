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
package com.rmn.qa.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openqa.grid.internal.Registry;
import org.openqa.grid.web.servlet.RegistryBasedServlet;
import org.openqa.selenium.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationRequestMatcher;
import com.rmn.qa.AutomationRunContext;
import com.rmn.qa.AutomationRunRequest;
import com.rmn.qa.AutomationUtils;
import com.rmn.qa.RequestMatcher;

/**
 * Legacy API to pull free threads for a given browser
 */
public class AutomationStatusServlet extends RegistryBasedServlet {

    private static final long serialVersionUID = 8484071790930378855L;
    private static final Logger log = LoggerFactory.getLogger(AutomationStatusServlet.class);
    private RequestMatcher requestMatcher;

    /**
     * Constructs a default status servlet
     */
    public AutomationStatusServlet() {
        this(null);
    }

    /**
     * Constructs a default status servlet with the specified {@link org.openqa.grid.internal.Registry register}
     * @param registry
     */
    public AutomationStatusServlet(Registry registry) {
        super(registry);
        this.requestMatcher = new AutomationRequestMatcher();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        String browserRequested = request.getParameter("browser");

        if (browserRequested == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter 'browser' must be passed in as a query string parameter");
            return;
        }
        // OS is optional
        String os = request.getParameter("os");
        Platform platformRequested = AutomationUtils.getPlatformFromObject(os);
        AutomationRunRequest runRequest = new AutomationRunRequest(AutomationStatusServlet.class.getSimpleName(),null,browserRequested,null,platformRequested);
        log.info(String.format("Legacy server request received.  Browser [%s]", browserRequested));
        AutomationRunContext context = AutomationContext.getContext();
        // If a run is already going on with this browser, return an error code
        if(context.hasRun(browserRequested)) {
            response.setStatus(400);
            return;
        }
        // Synchronize this block until we've added the run to our context for other potential threads to see
        int availableNodes = requestMatcher.getNumFreeThreadsForParameters(getRegistry().getAllProxies(),runRequest);
        response.setStatus(HttpServletResponse.SC_OK);
        // Add the browser so we know the nodes are occupied
        context.addRun(new AutomationRunRequest(browserRequested,availableNodes,browserRequested,null,platformRequested));
        try (InputStream in = new ByteArrayInputStream(String.valueOf(availableNodes).getBytes("UTF-8"))){
            ByteStreams.copy(in, response.getOutputStream());
        } finally {
            response.flushBuffer();
        }
    }

}
