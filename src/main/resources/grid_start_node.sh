#!/bin/bash
# ######################################################################################
# Shell script to configure a Selenium Grid worker node and add it to the grid.
# This is executed by this command in the crontab, as seen with "cronatab -l":
#   @reboot /home/ubuntu/grid/grid_start_node.sh >> /home/ubuntu/grid/process.log 2>&1
# 
# ######################################################################################
PATH=/sbin:/usr/sbin:/bin:/usr/bin
cd /home/ubuntu/grid/

# Call into AWS and figure out what our EC2 instance ID is.
# Sometimes cron kicks off this script before the AWS/EC2 networking stack is awake.
# We need to wait, polling for when the instance ID details beome available.
echo "Grid Automation process started, looking up our EC2 Instance at `date`"
export EC2_INSTANCE_ID=
while [ -z "${EC2_INSTANCE_ID}" ] ; do
   # Wait 1 seconds to let the network settle down on boot up and let AWS get all of
   # the user-data zip file artifiacts in place.
   sleep 1
   export EC2_INSTANCE_ID="`wget -q -O - http://169.254.169.254/latest/meta-data/instance-id`"
done

echo "Retreived our EC2 Instance ID: $EC2_INSTANCE_ID at `date`"

# Pull down the user data, which will be a zip file containing necessary information
# This is placed in the user-data fild for the instance by the getUserData() and launchNodes() methods.
export NODE_TEMPLATE="/home/ubuntu/grid/nodeConfigTemplate.json"
curl http://169.254.169.254/latest/user-data -o /home/ubuntu/grid/data.zip

# Now, unzip the data downloaded from the userdata
unzip -o /home/ubuntu/grid/data.zip -d /home/ubuntu/grid/

# Replace the instance ID in the node config file
sed "s/<INSTANCE_ID>/$EC2_INSTANCE_ID/g" $NODE_TEMPLATE > /home/ubuntu/grid/nodeConfig.json

# Finally, run the java process in a window so browsers can run
xvfb-run --auto-servernum --server-args='-screen 0, 1600x1200x24' \
   java -jar /home/ubuntu/grid/selenium-server-node.jar -role node \
   -nodeConfig /home/ubuntu/grid/nodeConfig.json \
   -Dwebdriver.chrome.driver="/home/ubuntu/grid/chromedriver" \
   -log /home/ubuntu/grid/grid.log &

