#!/bin/bash
IP=$(curl http://169.254.169.254/latest/meta-data/local-ipv4)
java -DipAddress="$IP" \
  -DpropertyFileLocation="/aws.properties" \
  -cp target/automation-grid.jar org.openqa.grid.selenium.GridLauncher \
  -role hub -servlets "com.rmn.qa.servlet.AutomationTestRunServlet","com.rmn.qa.servlet.StatusServlet" \
  > /opt/SeleniumGridScaler/log/stdouterr 2>&1 &
