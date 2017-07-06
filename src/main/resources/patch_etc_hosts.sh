#!/bin/bash
# ###########################################################################
# A shell script to patch the /etc/hosts file for new grid node hosts.
# Stock hosts files on EC2 usually only contain the loopback addresses and
# are missing the actual host name and the DHCP address binding.  This can
# delay certain network operations.
# This script patches the /etc/hosts file to contain the hostname on both the
# loopback and DHCP based VPC IP address.
# ###########################################################################
PATH=/sbin:/usr/sbin:/bin:/usr/bin

# Make certain we can find our host's EC2 instance ID.  This ensures that the
# network stack is online and that it can get out to the rest of the netowrk.
export EC2_INSTANCE_ID=
while [ -z "${EC2_INSTANCE_ID}" ] ; do
   # Wait 1 seconds to let the network settle down on boot up and let AWS get all of
   # the user-data zip file artifiacts in place.
   sleep 1
   export EC2_INSTANCE_ID="`wget -q -O - http://169.254.169.254/latest/meta-data/instance-id`"
done

# Now we are certain the network stack is online, what is the host's IP?
LOCALIP=`ifconfig | grep "inet addr" | grep -v "127.0.0" | cut -d':' -f2 | cut -d' ' -f1`
HOSTNAME=`hostname`

# Count the number of instacnces of the local ip in /etc/hosts. If this comes
# back as zero then we know the local IP address needs to be added to the
# /etc/hosts file.
HASLOCALIP=`grep -c $LOCALIP /etc/hosts`

# Count the number of instances of the host name in /etc/hosts.  If this comes
# back as zero then we know the localhost line needs to be patched to include
# the local host name. 
HASHOSTNAME=`grep -c $HOSTNAME /etc/hosts`

# Add the local hostname to /etc/hosts.
if [ "0" -eq "$HASHOSTNAME" ] ; then
   sudo sed -i -e "s/localhost/localhost $HOSTNAME/" /etc/hosts
fi

# Add the local IP to /etc/hosts.
if [ "0" -eq "$HASLOCALIP" ] ; then
   sudo bash -c "echo '# hostname added to support SeleniumGridScaler. ' >> /etc/hosts"
   sudo bash -c "echo \"$LOCALIP $HOSTNAME\" >> /etc/hosts"
fi

# Add an /etc/hosts entry for the Hub IP address.
HASHUBNODEADDR=`grep -c hubHost /home/ubuntu/grid/nodeConfig.json`
while [ "0" -eq "$HASHUBNODEADDR" ] ; do
   # Wait for the node setup script to pull down the hub's IP address.
   sleep 1
   export HASHUBNODEADDR=`grep -c hubHost /home/ubuntu/grid/nodeConfig.json`
done

# Lookup the IP address of the hubNode in the nodeConfig.json file.
HUBNODEADDR=`grep hubHost /home/ubuntu/grid/nodeConfig.json | cut -d'"' -f4`

# Only add the entry if it is actually necessary to do so.
ETCHOSTSHUBCOUNT=`grep -c $HUBNODEADDR /etc/hosts`
if [ "0" -eq "$ETCHOSTSHUBCOUNT" ] ; then
   sudo bash -c "echo $HUBNODEADDR hubnode gridhead >> /etc/hosts"
fi
