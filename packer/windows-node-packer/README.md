# Winows SeleniumGridScaler(Node) AMI

for auto-login feature this AMI has admin's password inside.

login: administrator
permanent password: selenium123#

if you want to change this password - please look through all the scripts, as it occurs in several places

## Files and/or Scripts

* grid.cmd - shell script for autorun
  * to workaround https://github.com/mitchellh/packer/issues/2714 grid.cmd file first being uploaded to c:\ and then using inline script copies to startup destination

* grid.ps1 - script to install and run the selenium
* windows2012.json - packer json file
* winrm.ps1 - user_data powershell script
  * configures winrm
  * reconfigures ec2-services
  * changes administrator's password
  * installs chocolatey 
  * installs all the necessary software
  * configures autologon

## How To Build an AMI

set up the following variables and run the build 
(ami_id in this example - us-west-2 windows2012r2 as of 8/20/16 ami-1712d877)

```
packer build \
  -var 'ami_id=ami-1712d877' \
  -var 'aws_region=us-west-2' \
  windows2012.json
```

also you can specifiy vpc_id and subnet_id

```
packer build \
  -var 'ami_id=ami-1712d877' \
  -var 'vpc_id=vpc-XXXXXXXX' \
  -var 'subnet_id=subnet-XXXXXXXX' \
  -var 'aws_region=us-west-2' \
  windows2012.json
```

