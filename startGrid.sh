#!/bin/bash
AWS_ACCESS_KEY=AKIAIPZU444AQ2NOQHEQ
AWS_SECRET_KEY=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
[ -z ${AWS_ACCESS_KEY} ] && echo "AWS_ACCESS_KEY MUST BE SET!" && exit 1
[ -z ${AWS_SECRET_KEY} ] && echo "AWS_SECRET_KEY MUST BE SET!" && exit 1
echo $AWS_ACCESS_KEY
# set defaults
[ -z ${IP_ADDRESS} ] && IP_ADDRESS=172.21.134.223

java -DawsAccessKey="${AWS_ACCESS_KEY}" \
    -DawsSecretKey="${AWS_SECRET_KEY}" \
    -DipAddress="${IP_ADDRESS}" \
    -cp ./target/automation-grid.jar org.openqa.grid.selenium.GridLauncher \
    -role hub \
    -servlets "com.rmn.qa.servlet.AutomationTestRunServlet","com.rmn.qa.servlet.StatusServlet" \
    -DPOOL_MAX=1024 \
    -DtotalNodeCount=325 \
    -hubConfig hub.static.json \
    -log grid.log \
    -DpropertyFileLocation=/Users/rambighananthan/tools/GridScaleExp/aws.properties
