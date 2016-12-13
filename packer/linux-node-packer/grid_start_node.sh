#!/bin/sh

PATH=/sbin:/usr/sbin:/bin:/usr/bin

install -d -oseluser /home/seluser/grid
cd /home/seluser/grid/
export EC2_INSTANCE_ID="`wget -q -O - http://169.254.169.254/latest/meta-data/instance-id || die \"wget instance-id has failed: $?\"`"
# Pull down the user data, which will be a zip file containing necessary information
export NODE_TEMPLATE="/home/seluser/grid/nodeConfigTemplate.json"
curl http://169.254.169.254/latest/user-data -o /home/seluser/grid/data.zip

# Now, unzip the data downloaded from the userdata
unzip -o /home/seluser/grid/data.zip -d /home/seluser/grid/
# Replace the instance ID in the node config file
sed "s/<INSTANCE_ID>/$EC2_INSTANCE_ID/g" $NODE_TEMPLATE > /home/seluser/grid/nodeConfig.json
# Finally, run the java process in a window so browsers can run
xvfb-run --auto-servernum --server-args='-screen 0, 1600x1200x24' java -jar /opt/secawselenium/selenium-server-standalone.jar -role node -nodeConfig /home/seluser/grid/nodeConfig.json -log /home/seluser/grid/grid.log &

