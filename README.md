# AmazonMQNoB

## Getting started

+ Use a CloudFormation template to deploy a network of brokers in two AWS regions. 
+ Each region has a 3-node network of brokers across two subnets – an internal subnet where the consumer/producer applications are located and an external facing subnet that interfaces with the brokers in the other region.
+ Configure networking and broker configurations. We use VPC peering for IP reachability between the brokers in the two regions.
+ Deploy a producer lambda function in one region to send a message to a consumer lambda function in the other region.
+ Deploy a network of brokers in two regions.
+ Setup AWS Systems Manager parameter store to save passwords, broker URIs and usernames. The consumer and producer lambdas fetch these from the Systems manager parameter store, rather than storing them in other configuration stores.
## Setup Steps 
+  Step 1: Complete these prerequisites: 
    + In a workstation with AWS CLI v2, create an IAM role to be used by lambda.  
    +  Download the two policy json files from scripts folder and execute the below command using AWS CLI.

    rolename="LambdaRoleForMQ"
    
    aws iam create-role --role-name $rolename --assume-role-policy-document file://Policy.json
    
    aws iam attach-role-policy --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole --role-name $rolename
    
    aws iam attach-role-policy --policy-arn arn:aws:iam::aws:policy/AWSLambda_FullAccess --role-name $rolename

    aws iam create-policy --policy-name my-policy --policy-document file://perm.json

    aws iam attach-role-policy --policy-arn arn:aws:iam::033466939092:policy/my-policy --role-name $rolename

+ Step 2: Create the Network of Brokers, in the first region of your choice.
  + Log in to the AWS console.  
  + Select the CloudFormation Service and choose Create stack.
  + In the Prerequisite - Prepare template section, select the Template is ready.
  + On the Create stack page choose Upload the template file AmazonMQNobCF.json in the Specify template section and click on Choose file button. Click Next.
  + Provide a name for the stack.

  + Configure the following parameters for the network configuration section: 
    +  VPCCIDR – provide the CIDR for the VPC in this region. 
    + AZ1 and AZ2 & Subnet1CIDR and Subnet2CIDR – For each subnet, choose the AZs.

  + Configure the following parameters in Amazon MQ configuration section: 
    + AmazonMqUsername – the username for AmazonMQ broker. 
    + AmazonMqPassword – Password for AmazonMQ broker.    
  **Note:* Please ensure these two parameter values are kept identical in both the regions.*

  + Configure the following parameter in the Lambda configuration section:

    + LambdaCodeS3Bucket- The S3bucket where Lambda jar or zip file is uploaded. The Lambda source and binaries are available . 
    + LambdaCodeS3Key – The name including the prefix of the lambda function to deploy in this region. In the first region, this is mqProducer.jar. 
    + LambdaCodeRuntime – There are two lambda functions provided – one for Java11 and another for .Net. For the first region, choose the Java11 runtime. 
    + LambdaRoleName –the name of the Lambda execution role to be created.

  + Click Next  
  + Select the checkbox for “I acknowledge that AWS CloudFormation might create IAM resources with custom names”. 
  + Click Create stack. 
  + Verify that the brokers are created by logging in to the AWS AmazonMQ console. There should be three brokers in this region - Broker1, Broker2 and Broker3 

+ Step 3: Create the Network of Brokers in the second region 
  + Follow the steps in Step2 to create the network of brokers in the second region. The following values need to be carefully provided to ensure correct interoperation of resources across the two regions: 
    +  Network configuration section:
      +  VPCCIDR – Ensure that the VPC CIDR in region2 is different than the one in the region1. 
    + Amazon MQ configuration section: 
      + AmazonMqUsername & AmazonMqPassword – Ensure that the username and password are same as in region1. 
    + Lambda configuration section: 
       + LambdaCodeS3Bucket –a bucket in this region. 
       + LambdaCodeS3Key value –choose the second lambda function – mqConsumer.zip. This lambda function is written in the .Net. 
       + LambdaCodeRuntime - choose the dotnetcore3.1 runtime.

+ Step 4: VPC Peering and updating route tables. 
  + Follow the steps here to complete VPC peering between the two VPCs. 
  + After peering connection is established, update the route tables in both the VPC to use the peering connection for traffic bound for the destination VPC. 
  + Follow these steps to enable DNS resolution for the peering connection. 
  + Route tables after updating should have entries as below:

+ Step 5: Configure the Network of Brokers The steps involved in configuring brokers include creating network connectors.

+ In AmazonMQ console in region1, click on Broker3, and under the Connections section, note the endpoint for the openwire protocol. + In region2 on broker3, set up network of brokers using the networkConnector configuration element. 
  +  Select broker3 and click edit.

  +  In Configuration section, click Edit, which opens a new browser window.

  + Edit the configuration revision for each broker and add a new NetworkConnector within the NetworkConnectors section. Replace the uri as below with the URI for the broker3 in region1, noted in step above.

        <networkConnector name="broker3inRegion2_to_ broker3inRegion1" duplex="true" networkTTL="5" userName="MQUserName" uri="static:(ssl://b-123ab4c5-6d7e-8f9g-ab85-fc222b8ac102-1.mq.ap-south-1.amazonaws.com:61617)" />
  +  To apply your changes, edit your broker and set it to use the latest revision. Reboot the broker.

+ Step 6: Send a test message On a workstation with AWS CLI v2, send a test message using mqProducer lambda function in region1.

  + Configure AWS CLI v2 with AWS default region as region1. 
  + Execute the producer lambda:

    aws lambda invoke --function-name mqProducer out --log-type Tail --query 'LogResult' --output text | base64 -d

  • Successful execution of lambda returns output confirming a test message was sent. This confirms that the lambda has sent the message to the broker in region1.
 
+ Step 7: Receive the test message 
  + Change the AWS default region in CLI to region2. +
  + Execute the consumer lambda:

    aws lambda invoke --function-name mqConsumer out --log-type Tail --query 'LogResult' --output text | base64 -d

  + Successful execution of the lambda returns output confirming the message was recieved. This confirms that the lambda has received the message from the broker in region2.
  + This receipt of the message confirms that the message has traversed the network of brokers from region1 to region2.

## Clean up

+ Clean up resources created by the CloudFormation template by clicking on Delete stack on the CloudFormation console, in both the regions. 
+  In S3 buckets in the two regions, delete the mqProducer.jar and mqConsumer.zip files. image
