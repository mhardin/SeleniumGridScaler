# Linux SeleniumGridScaler(HUB) AMI

# Note

Please update aws.properties file according to your AWS account
and then build an AMI

## Technical Information

OS - rhel7

## Files and/or Scripts

* capacity.sh - sample script to request nodes to spin-up
* hub.sh - script to install and configure the AMI
* linux-hub.json - packer json file
* start.sh - script that starts the SeleniumGridScaler

## How To Build an AMI

set up the following variables and run the build 
(ami_id in this example - us-west-2 rhel7 as of 8/20/16 ami-775e4f16)

```
packer build \
  -var 'ami_id=ami-775e4f16' \
  -var 'aws_region=us-west-2' \
  linux-hub.json
```

also you can specifiy vpc_id and subnet_id

```
packer build \
  -var 'ami_id=ami-775e4f16' \
  -var 'vpc_id=vpc-XXXXXXXX' \
  -var 'subnet_id=subnet-XXXXXXXX' \
  -var 'aws_region=us-west-2' \
  linux-hub.json
```



