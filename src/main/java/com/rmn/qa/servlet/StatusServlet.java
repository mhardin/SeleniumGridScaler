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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;
import org.openqa.grid.internal.utils.configuration.GridHubConfiguration;
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;
import org.openqa.grid.web.servlet.RegistryBasedServlet;
import org.openqa.grid.web.utils.BrowserNameUtils;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;
import com.rmn.qa.AutomationConstants;
import com.rmn.qa.AutomationContext;
import com.rmn.qa.AutomationDynamicNode;
import com.rmn.qa.AutomationRequestMatcher;
import com.rmn.qa.AutomationRunRequest;

/**
 * Front end to monitor what is currently happening on the proxies. The display is defined by
 * HtmlRenderer returned by the RemoteProxy.getHtmlRenderer() method.
 */
public class StatusServlet extends RegistryBasedServlet {

    private static final long serialVersionUID = 8484071790930378855L;
    private static final Logger log = LoggerFactory.getLogger(StatusServlet.class);
    private static String coreVersion;
    private static String coreRevision;

    private AutomationRequestMatcher requestMatcher;

    /**
     * Constructs a servlet with default functionality
     */
    public StatusServlet() {
        this(null);
    }

    /**
     * Constructs a servlet with the specified registry
     * @param registry
     */
    public StatusServlet(Registry registry) {
        super(registry);
        getVersion();
        requestMatcher = new AutomationRequestMatcher();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        int refresh = -1;

        if (request.getParameter("refresh") != null) {
            try {
                refresh = Integer.parseInt(request.getParameter("refresh"));
            } catch (NumberFormatException e) {
                // ignore wrong param
            }

        }

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);

        StringBuilder builder = new StringBuilder();

        builder.append("<html>");
        builder.append("<head>");

        if (refresh != -1) {
            builder.append(String.format("<meta http-equiv='refresh' content='%d' />", refresh));
        }
        builder.append("<title>Grid overview</title>");

        builder.append("<style>");
        builder.append(".busy {");
        builder.append(" opacity : 0.4;");
        builder.append("filter: alpha(opacity=40);");
        builder.append("}");
        builder.append("</style>");
        builder.append("</head>");

        builder.append("<body>");
        builder.append("<H1>Grid Hub ");
        builder.append(coreVersion).append(coreRevision);
        builder.append("</H1>");
        int chromeThreads = requestMatcher.getNumFreeThreadsForParameters(getRegistry().getAllProxies(),new AutomationRunRequest(StatusServlet.class.getSimpleName(),null, BrowserType.CHROME));
        int firefoxThreads = requestMatcher.getNumFreeThreadsForParameters(getRegistry().getAllProxies(),new AutomationRunRequest(StatusServlet.class.getSimpleName(),null,BrowserType.FIREFOX));
        int ieThreads = requestMatcher.getNumFreeThreadsForParameters(getRegistry().getAllProxies(),new AutomationRunRequest(StatusServlet.class.getSimpleName(),null,"internetexplorer"));
        builder.append("<H2>Free Threads - Chrome: ").append(chromeThreads).append(" Firefox: ").append(firefoxThreads).append(" IE: ").append(ieThreads).append("</H2>");

        for (RemoteProxy proxy : getRegistry().getAllProxies()) {
            StringBuilder localBuilder = new StringBuilder();

            localBuilder.append("<fieldset>");
            localBuilder.append("<legend>").append(proxy.getClass().getSimpleName()).append("</legend>");
            localBuilder.append("listening on ").append(proxy.getRemoteHost());

            Object instanceId = proxy.getConfig().custom.get(AutomationConstants.INSTANCE_ID);
            if(instanceId != null) {
                AutomationDynamicNode node = AutomationContext.getContext().getNode((String)instanceId);
                if(node != null) {
                    localBuilder.append("<br>EC2 dynamic node " + node.getInstanceId());
                    if (!StringUtils.isEmpty(node.getInstanceType())) {
                        localBuilder.append("<br>Instance Type " + node.getInstanceType());
                    }
                    localBuilder.append("<br>Start time " + node.getStartDate());
                    localBuilder.append("<br>Current shutdown time ").append(node.getEndDate());
                    localBuilder.append("<br>Requested test run ").append(node.getUuid());
                }
            }
            if (((DefaultRemoteProxy) proxy).isDown()) {
                localBuilder.append("(cannot be reached at the moment)");
            }
            localBuilder.append("<br />");
            if (proxy.getTimeOut() > 0) {
                int inSec = proxy.getTimeOut() / 1000;
                localBuilder.append("test session time out after ").append(inSec).append(" sec.<br />");
            }

            localBuilder.append("Supports up to <b>").append(proxy.getMaxNumberOfConcurrentTestSessions())
                    .append("</b> concurrent tests from: <br />");

            localBuilder.append("");
            for (TestSlot slot : proxy.getTestSlots()) {
                TestSession session = slot.getSession();

                String icon = getIcon(slot.getCapabilities(),proxy);
                if (icon != null) {
                    localBuilder.append("<img ");
                    localBuilder.append("src='").append(icon).append("' ");
                } else {
                    localBuilder.append("<a href='#' ");
                }

                if (session != null) {
                    localBuilder.append(" class='busy' ");
                    localBuilder.append(" title='").append(session.get("lastCommand")).append("' ");
                } else {
                    localBuilder.append(" title='")
                            .append(slot.getCapabilities())
                            .append("type="+slot.getProtocol())
                            .append("' ");
                }

                if (icon != null) {
                    localBuilder.append("/>");
                } else {
                    localBuilder.append(">");
                    localBuilder.append(slot.getCapabilities().get(CapabilityType.BROWSER_NAME));
                    localBuilder.append("</a>");
                }

            }
            localBuilder.append("</fieldset>");
            builder.append(localBuilder);
        }

        int numUnprocessedRequests = getRegistry().getNewSessionRequestCount();

        if (numUnprocessedRequests > 0) {
            builder.append(String.format("%d requests waiting for a slot to be free.", numUnprocessedRequests));
        }

        builder.append("<ul>");
        for (DesiredCapabilities req : getRegistry().getDesiredCapabilities()) {
            builder.append("<li>").append(req.asMap()).append("</li>");
        }
        builder.append("</ul>");

        if (request.getParameter("config") != null) {
            builder.append(getConfigInfo(request.getParameter("configDebug") != null));
        } else {
            builder.append("<a href='?config=true&configDebug=true'>view config</a>");
        }

        builder.append("</body>");
        builder.append("</html>");

        try (InputStream in = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));) {
            ByteStreams.copy(in, response.getOutputStream());
        } finally {
            response.flushBuffer();
        }
    }

    /**
     * retracing how the hub config was built to help debugging.
     *
     * @return
     */
    private String getConfigInfo(boolean verbose) {
        StringBuilder builder = new StringBuilder();

        GridHubConfiguration config = getRegistry().getConfiguration();
        builder.append("<b>Config for the hub :</b><br/>");
        builder.append(prettyHtmlPrint(config));

        if (verbose) {

            GridHubConfiguration tmp = GridHubConfiguration.loadFromJSON("defaults/DefaultHub.json");

            builder.append("<b>Config details :</b><br/>");
            /*builder.append("<b>hub launched with :</b>");
            for (int i = 0; i < config.getArgs().length; i++) {
                builder.append(config.getArgs()[i]).append(" ");
            } */

            builder.append("<br/><b>the final configuration comes from :</b><br/>");
            builder.append("<b>the default :</b><br/>");
            builder.append(prettyHtmlPrint(tmp));
            /*
            builder.append("<b>updated with grid1 config :</b>");
            if (config.getGrid1Yml() != null) {
                builder.append(config.getGrid1Yml()).append("<br/>");
                tmp.loadFromGridYml(config.getGrid1Yml());
                builder.append(prettyHtmlPrint(tmp));
            } else {
                builder
                        .append("No grid1 file specified. To specify one, use -grid1Yml XXX.yml where XXX.yml is a grid1 config file</br>");
            } */

            builder.append("<br/><b>updated with hub config : </b>");
            if (config.hubConfig != null) {
                builder.append(config.hubConfig).append("<br/>");
                tmp=GridHubConfiguration.loadFromJSON(config.hubConfig);
                builder.append(prettyHtmlPrint(tmp));
            } else {
                builder
                        .append("No hub config file specified. To specify one, use -hubConfig XXX.json where XXX.json is a hub config file</br>");
            }

            /*builder.append("<br/><b>updated with params :</b></br>");
            tmp.loadFromCommandLine(config.getArgs());
            builder.append(prettyHtmlPrint(tmp));*/
        }
        return builder.toString();
    }

    private String key(String key) {
        // return "<abbr title='" + GridDocHelper.getHubParam(key) + "'>" + key + " : </abbr>";
        // no help for Selenium V3
        return "<abbr title='" + "No help specified for " + key + "'>" + key + " : </abbr>";
    }

    private String prettyHtmlPrint(GridHubConfiguration config) {
        StringBuilder b = new StringBuilder();

        b.append(key("host")).append(config.host).append("</br>");
        b.append(key("port")).append(config.port).append("</br>");
        b.append(key("cleanUpCycle")).append(config.cleanUpCycle).append("</br>");
        b.append(key("timeout")).append(config.timeout).append("</br>");
        b.append(key("browserTimeout")).append(config.browserTimeout).append("</br>");


        b.append(key("newSessionWaitTimeout")).append(config.newSessionWaitTimeout)
                .append("</br>");
        //no grid1mapping
        //b.append(key("grid1Mapping")).append(config.getGrid1Mapping()).append("</br>");
        b.append(key("throwOnCapabilityNotPresent")).append(config.throwOnCapabilityNotPresent)
                .append("</br>");

        b.append(key("capabilityMatcher"))
                .append(
                        config.capabilityMatcher == null ? "null" : config.capabilityMatcher
                                .getClass().getCanonicalName()).append("</br>");
        b.append(key("prioritizer"))
                .append(
                        config.prioritizer == null ? "null" : config.prioritizer.getClass()
                                .getCanonicalName())
                .append("</br>");
        b.append(key("servlets"));
        for (String s : config.servlets) {
            b.append(s.getClass().getCanonicalName()).append(",");
        }
        b.append("</br></br>");
        b.append("<u>all customs :</u></br></br>");
        List<String> keys = new ArrayList<String>();
        keys.addAll(config.custom.keySet());
        Collections.sort(keys);
        for (String s : keys) {
            b.append(key(s.replaceFirst("-", ""))).append(config.custom.get(s)).append("</br>");
        }
        b.append("</br>");
        return b.toString();
    }

    private void getVersion() {
        final Properties p = new Properties();

        InputStream stream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("VERSION.txt");
        if (stream == null) {
            log.error("Couldn't determine version number");
            return;
        }
        try {
            p.load(stream);
        } catch (IOException e) {
            log.error("Cannot load version from VERSION.txt" + e.getMessage());
        }
        coreVersion = p.getProperty("selenium.core.version");
        coreRevision = p.getProperty("selenium.core.revision");
        if (coreVersion == null) {
            log.error("Cannot load selenium.core.version from VERSION.txt");
        }
    }

    private String getIcon(Map<String, Object> capabilities,RemoteProxy proxy) {
        return BrowserNameUtils.getConsoleIconPath(new DesiredCapabilities(capabilities),
                proxy.getRegistry());
    }

}
