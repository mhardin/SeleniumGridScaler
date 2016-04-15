# SeleniumGridScaler [![Build Status](https://travis-ci.org/RetailMeNot/SeleniumGridScaler.svg?branch=master)](https://travis-ci.org/RetailMeNot/SeleniumGridScaler)

***
#### Overview

A Selenium Grid plugin for integrating your project with AWS (Amazon Web Services) to allow for resources (EC2 'nodes') to dynamically spin up/down (autoscale) depending on current test load.  The jar should be pulled down and installed to serve as a hub for the desired Selenium Grid setup.

#### Details
New nodes will be started as needed depending on the browser requested:

* Chrome -  New c3.large instances will be started capable of running 6 threads (tests) per instance.  If your test run requests 30 threads, this will result in 5 nodes being started up, where as 31 are requested, this will result in 6 nodes started up.  Note that these can also run firefox tests as well
* Firefox - New t2.micro instances will be started capable of running 1 thread (test) per instance.  Selenium window focus issues were the reason behind running 1 thread per virtual machine.  These instances can run chrome tests as well.
* Internet Explorer - IE is currently not supported.  There are plans to add IE support in an upcoming release.

AWS bills by rounding up to the next hour, so if you leave a machine on for 5 minutes, you get billed for the full hour (60 minutes).  Based on this logic, SeleniumGridScaler's automatic termination logic will terminate nodes at the end of the current billing cycle, taking into account current test load.  So if there is enough test load, say 12 tests running and only 12 threads are online (2 chrome instances each capable of running 6 for a total of 12), all of the nodes will stay on.  If the current time crosses into the next billing cycle (e.g. they're on for 1 hour and 5 minutes), SeleniumGridScaler will not attempt to terminate them until the end of that next billing cycle (will attempt to terminate at 1 hour and 55 minutes instead of prematurely terminating paid for resources).

## Requirements
A Java 8 or later runtime is required in order to run this application

## Installation/Startup
Download the automation-grid jar file. To start up the hub process, start the jar with the java jar command like so.  Note, you must register the Scaler servlet (AutomationTestRunServlet) with GridLauncher.

    java -DawsAccessKey=foo -DawsSecretKey=bar -DipAddress="10.10.28.205" -cp automation-grid.jar org.openqa.grid.selenium.GridLauncher -role hub -servlets "com.rmn.qa.servlet.AutomationTestRunServlet","com.rmn.qa.servlet.StatusServlet" -hubConfig hub.json


The -hubConfig configuration key and value are optional to specify Selenium Hub configuration information.  Please see the 'Selenium Configuration' section below for more information.  After starting the hub process, you should be able to run your tests against the hub in the **Running Your Tests** section below

## Installation/Startup w/Docker
Clone source code repository, and build target jars with:

    mvn install

After building target jars, build docker image:

    docker build -t selenium-grid-ec2

After the docker image has been built, you can start a new docker container with it:

    docker run -d -p 4444:4444 -e AWS_ACCESS_KEY={REPLACE_WITH_YOUR_KEY} -e AWS_SECRET_KEY={REPLACE_WITH_SECRET_KEY selenium-grid-ec2

## Startup Configuration
 Certain configuration values can be passed in to change the default behavior of SeleniumGridScaler

 System properties

* awsAccessKey (most likely required) - This should be the Access Key ID for your AWS account. If not set permissions are derived from IAM role.
* awsSecretKey (most likely required) - This should be the Secret Key for your AWS account. If not set permissions are derived from IAM role.
* ipAddress (required) - Resolvable host name or ip address of the hub for started nodes to connect to.  Note 'localhost' or '127.0.0.1' will not work as this will not be resolvable from the context of another machine
* totalNodeCount - Maximum number of nodes that can connect to your hub.  Defaults to 150 if not specified
* extraCapabilities - CSV list of capabilities you want to be considered if specified client side (e.g. adding 'target' to this list will require any capabilities coming in with the 'target' key present to match the value in the node config)
* useReaperThread - Set this to 'false' if you do not want the reaper thread running.  The reaper thread will automatically terminate instances that are not registered with the hub to prevent unused instances from accumulating in AWS over time.  This can be useful to disable if you're trying to debug a node that the plugin started and killed the java process on it.

More information can be found [here](http://docs.aws.amazon.com/AWSSecurityCredentials/1.0/AboutAWSCredentials.html) on AWS Access

## Running Your Tests
In your test run, you must send a GET HTTP request to the servlet, with the required query string parameters, to ensure the requested resources are available.

* browser - This is the browser you want to run in.  Currently supported values, are chrome, firefox, and internetexplorer
* threadCount - Number of threads you want to run for your tests
* uuid - Unique test run identifier

If you wanted to do a test run with 10 threads in chrome with a test run UUID of 'testRun1', the HTTP request would look something like this

    http://hubIpAddress:4444/grid/admin/AutomationTestRunServlet?uuid=testRun1&threadCount=10&browser=chrome

Possible response codes:

* Returns a 201 if the request can be fulfilled but AMIs must be started
* Returns a 202 if the request can be fulfilled with current capacity
* Returns a 400 if the required parameters are not passed in.
* Returns a 409 if the server is at full node capacity
* Returns a 500 for an unexpected error condition (see log for details)

After a successful response code (201 or 202), you should be able to run your test run against the hub end point like so.
Note that the test run UUID used in the API call above must be passed in via a desired capability or you may run into unintended side effects



        URL url = null;
        try {
            url = new URL("http://servername:4444/wd/hub");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        capabilities.setBrowserName("chrome");
        // The test run UUID associated with the test needs to be passed in
        // so SeleniumGridScaler can associate in progress tests with test runs
        capabilities.setCapability("uuid","testRun1");
        WebDriver driver = new RemoteWebDriver(url,capabilities);

## Selenium Configuration
Note that on top of the SeleniumGridScaler configuration, you can use all existing configurations for Selenium.  Available selenium configurations values can be found [here](https://code.google.com/p/selenium/source/browse/java/server/src/org/openqa/grid/common/defaults/GridParameters.properties) and an example of passing them in can be seen in the **Running Your Tests** section above

## Test Access
If you're trying to test against a website thats not publicly available on the internet, such as a private testing environment behind a firewall, you will need to setup up some sort of solution to allow your nodes have to access your test environment(s).  Two ways of granting access have been included below.

* **Whitelist IP Addresses** One way to accomplish access for your dynamically started nodes to have access to your test environments is by white listing IP addresses in AWS.  This [guide](https://github.com/RetailMeNot/SeleniumGridScaler/blob/master/HowToSetupAVPC.md) is a great way to show how to setup two separate subnets, and route all traffic through a single NAT machine so that you only have to whitelist the IP address of the NAT machine, and not for every individual node.  Essentially, you'll create a VPC with 2 subnets, one private and one public, each occupying half the range of the VPC (or whatever you prefer).  The NAT will live in the public subnet, with the nodes living in the private subnet.  You will then configure a security group to route all external traffic (outside the VPC range) in the private subnet through the NAT and associate this security group with your nodes when you start them up.  You will also want to create an internet gateway and associate it with your VPC so your machines can have access to the internet.

* **Setup a Tunnel** Another way to configure access for your nodes to hit your test environments would be to setup a network tunnel of some sort.  You would then need to modify the AMI to run this tunnel on startup or have it running 24/7.  You will probably need to work with your IT department to accomplish this.

## Logging
Logging will be saved to a log file found in in <working_directory>/log/automation.log.  If you would like to change the location, you can override the directory and log file name with the 'logLocation' system property (e.g. -DlogLocation=/log/myLogName.log) The current logging levels is set to INFO or higher.

## Installation to Develop Locally

The SeleniumGridScaler is a Maven project, which means when you have downloaded it and opened it in your favorite
Maven-enabled IDE, you should be able to run "mvn install", and a SNAPSHOT jar file will be deployed to your
local Maven repo.

Once the jar has been deployed, you should be able to include a small snippet in your project pom file to point to your installed SNAPSHOT to develop and test any changes you may want to make:

    <dependency>
        <groupId>com.retailmenot</groupId>
        <artifactId>selenium-grid-scaler</artifactId>
        <version>1.1-SNAPSHOT</version>
    </dependency>

If you're currently using Maven, and your repos, paths, and IDE are all set up correctly, you should be able
to address the classes in this project immediately (you may need to update your Maven dependencies).

If you would prefer to simply build a jar file and include it into your classpath, run "mvn package", and
the jar file should appear in the target folder under the main folder and then you can add it to your sources for your project.

## AWS Customization
If you would like to specify custom attributes for the instances that will be started in AWS, you can do so by creating a .properties file
and passing it in via a system property 'propertyFileLocation' for the file location (e.g. -DpropertyFileLocation=/myPath/customAws.properties).  Please see aws.properties.sample for an idea of how the format should look.
Possible values to override/implement:

* region - Represents the region you are running in, such as 'east' or 'west'.
* <region>_endpoint - Endpoint address for the region's API
* <region>\_linux\_node_ami - AMI ID for linux instances to be started
* <region>\_windows\_node_ami - AMI ID for windows instances to be started
* <region>\_security\_group (optional) - Security group ID for the instance to use
* <region>\_subnet_id (optional) - VPC ID for the instance to use
* <region>\_subnet\_fallback\_id\_1 (optional) - Set of fallback subnet IDs to use if the availability zone in the above subnet is full.  Naming should have a 1, 2, 3, etc suffix for the naming convention.  See aws.properties.sample for an example
* <region>\_key_name (optional) - Key name to associate with the instance
Please see AWS documentation for more information on how to use/setup these things
* tags (optional) - If you want to add any [tags](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/Using_Tags.html) to your instances as they're started, prefix your comma separated key/value pairs with 'tag' (e.g. 'tagDepartment=Department,QA' would add a tag with a key of 'Department' and a value of 'QA')


## Contributing

All pull requests are greatly appreciated! This project is intended for end users of Selenium Grid, but we've only implemented functionality we've needed to handle our current needs. If you need new features, open an issue on github, or better yet, contribute your own features!

The SeleniumGridScaler is a Maven/Java project, using JUnit for unit tests, Cobertura for coverage checks, and Selenium for web browser interaction.

## Contact
For any questions, concerns, or problems, please feel free to contact the author Matthew Hardin at mhardin.github@gmail.com

## License

This project has been released under the GPL license. Please see the license.txt file for more details.
