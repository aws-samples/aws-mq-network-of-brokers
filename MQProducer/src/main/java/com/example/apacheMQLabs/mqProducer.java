package com.example.apacheMQLabs;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQSslConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;

public class mqProducer implements RequestHandler<Object, Object> {
	
	private String user;
	private String password;
	private String queueName;
	private String failoverURI;
	
	@Override
    public String handleRequest(Object input, Context context) {
    	//Get logger 
		LambdaLogger logger = context.getLogger();
    	
    	//Do initialization
    	doInit(logger);
    
    	//Create connection factory for Openwire
    	final ActiveMQSslConnectionFactory connFacty = createAMQConnFact(logger);
    	
    	//Create a pooled connectory factory 
    	final PooledConnectionFactory pooledConnFacty = createPooledConnectionfact(logger, connFacty); 
    	
    	//Send message
    	sendMessage(logger, pooledConnFacty);
		return "Complete! ";    	
    }
	
	private void doInit(LambdaLogger logger) {
		String rName = System.getenv("AWS_REGION");
    	Region currentRegion = Region.of(rName);
    	logger.log("Current Region: " + currentRegion.toString() + " \n");
    	
        try {    	
        	//Fetch params from SSM
        	user = getStr("MQ-Username", logger, currentRegion);
        	logger.log("Username: " + user + " \n");
            password = getStr("MQ-Password", logger, currentRegion);
            //logger.log("Password: " + password + " \n");
            String uri1 = getStr("MQ-Broker1URI", logger, currentRegion);
            String uri2 = getStr("MQ-Broker2URI", logger, currentRegion);
            //logger.log("Uris: " + uri1 + " & " + uri2 + " \n");
            queueName = getStr("MQ-QueueName", logger, currentRegion);
            logger.log("Queue: " + queueName + " \n");
            //Failover URI
            failoverURI = "failover:(" + uri1 + "," + uri2 + ")?randomize=True";
            logger.log("Failover URI: " + failoverURI + " \n");
        } catch (Exception e) {
    		logger.log(e.getMessage() + " \n");
    		System.exit(1);    	}
	}
	
	private String getStr(String key, LambdaLogger logger, Region currentRegion) {
    	//set region for SSMClient
    	SsmClient client = SsmClient.builder().region(currentRegion).build();

    	try {
    		//Get param values
    		GetParameterRequest parameterRequest = GetParameterRequest.builder().name(key).build();
    		GetParameterResponse parameterResponse = client.getParameter(parameterRequest);
    		return parameterResponse.parameter().value();
    	} catch (SsmException e) {
    		logger.log("Error while gettign parameter " + key + " with message :" + e.getMessage() + " \n");
    		System.exit(1);
    		return e.getMessage();
    	}
    }	
	
	private ActiveMQSslConnectionFactory createAMQConnFact(LambdaLogger logger){
		// Create a conn factory
		final ActiveMQSslConnectionFactory connFacty = new ActiveMQSslConnectionFactory(failoverURI);
		connFacty.setConnectResponseTimeout(10000);
        return connFacty;
	}
	
	private PooledConnectionFactory createPooledConnectionfact(LambdaLogger logger, ActiveMQSslConnectionFactory connFacty) {
		// Create a pooled conn factory
		final PooledConnectionFactory pooledConnFacty = new PooledConnectionFactory(); 
		pooledConnFacty.setMaxConnections(10);
		pooledConnFacty.setConnectionFactory(connFacty);
        return pooledConnFacty;
	}
	
	private void sendMessage(LambdaLogger logger, PooledConnectionFactory pooledConnFacty) {
		//Get time
    	SimpleDateFormat timeformatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    	Date currentTime = new Date();
    	//logger.log("Datetime: " + currentTime.toString() + " \n");
		
    	try {    	
	    	//Establish connection
	        Connection conn = pooledConnFacty.createConnection(user, password);
	        conn.setClientID("AmazonMQ Networks Brokers lab-" + System.currentTimeMillis());
	        conn.start();
	            
	        //Create broker session and send message
	        Session brokerSession = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
	        MessageProducer qProducer = brokerSession.createProducer(brokerSession.createQueue(queueName));
	        TextMessage messageToSend = brokerSession.createTextMessage(String.format("Test message at " + timeformatter.format(currentTime).toString()));
	        qProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
	        qProducer.send(messageToSend);
	        logger.log("The message sent was: " + messageToSend.getText() + " \n");

	        //close
	        qProducer.close();
	        brokerSession.close();
	        conn.close();    
	          
	    } catch (JMSException ex) {
	    	System.out.println(String.format("Error: %s", ex.getMessage()));
	        System.exit(1);
	    }		
	}

}