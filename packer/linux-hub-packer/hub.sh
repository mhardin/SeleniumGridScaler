#!/bin/bash
set -e

yum update -y
yum-config-manager --enable "rhui-REGION-rhel-server-optional"
yum install -y java-1.8.0-openjdk-devel maven git tmux curl

git clone https://github.com/strataconsulting/SeleniumGridScaler.git /opt/SeleniumGridScaler
cd /opt/SeleniumGridScaler
chown ec2-user -R /opt/SeleniumGridScaler
su - ec2-user -c "cd /opt/SeleniumGridScaler ; mvn install"

cp /tmp/start.sh .
cp /tmp/aws.properties /
chown ec2-user -R /opt/SeleniumGridScaler /aws.properties

### https://www.certdepot.net/rhel7-rc-local-service/
echo "su - ec2-user -c 'cd /opt/SeleniumGridScaler ; ./start.sh'" >> /etc/rc.d/rc.local
chmod u+x /etc/rc.d/rc.local /opt/SeleniumGridScaler/start.sh
systemctl enable rc-local
