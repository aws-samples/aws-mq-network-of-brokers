using System;
using Amazon.Lambda.Core;
using Amazon.Lambda.Serialization;
using Apache.NMS;
using Amazon.SimpleSystemsManagement;
using Amazon.SimpleSystemsManagement.Model;
using System.Threading.Tasks;

[assembly: LambdaSerializer(typeof(Amazon.Lambda.Serialization.Json.JsonSerializer))]
namespace MQLambda
{
    public class MQClient
    {


        public  string consumerHandler (ILambdaContext context)
        {

            string region = Environment.GetEnvironmentVariable("AWS_REGION");
            string user = GetParamValueAsync("MQ-Username",region).Result;
            string password = GetParamValueAsync("MQ-Password", region).Result;
            string url1 = GetParamValueAsync("MQ-Broker1URI", region).Result;
            string url2 = GetParamValueAsync("MQ-Broker2URI", region).Result;
            string queueName = GetParamValueAsync("MQ-QueueName", region).Result;


            LambdaLogger.Log("Current Region: " + region + " \n");
            LambdaLogger.Log("DateTime : " + DateTime.Now.ToLongDateString() + " \n");
            LambdaLogger.Log("User Name : " + user + " \n");
            
            LambdaLogger.Log("\n Uris : " + url1 + "," + url2 + " \n");
         
            LambdaLogger.Log("Queue : " + queueName + " \n");
            string uristr = "failover:(" + url1 + "," + url2 + ")?randomize=true";


        Uri connecturi = new Uri(uristr); 
        IConnectionFactory factory = new Apache.NMS.ActiveMQ.ConnectionFactory(uristr);

        using (IConnection connection = factory.CreateConnection(user, password))
        {
            
            connection.Start();
            //LambdaLogger.Log("Started connection  \n");
            IDestination destination;
            using (ISession session = connection.CreateSession())
            {
                destination = session.GetQueue(queueName);
                using (IMessageConsumer consumer = session.CreateConsumer(destination))
                {
                    IMessage msg =  consumer.Receive(TimeSpan.FromSeconds(1));
                    if (msg != null)
                    {
                        ITextMessage textMessage = msg as ITextMessage;
                        string result =  "\n Message Received : " + textMessage.Text.ToString();
                        LambdaLogger.Log(result);

                        return result;
                    }
                        LambdaLogger.Log("\n No message found !");
                        return "\n No message found !";

                    }
                    
             }
        }
        
            

        }

        public string producerHandler(ILambdaContext context)
        {
            string region = Environment.GetEnvironmentVariable("AWS_REGION");
            string user = GetParamValueAsync("MQ-Username", region).Result;
            string password = GetParamValueAsync("MQ-Password", region).Result;
            string url1 = GetParamValueAsync("MQ-Broker1URI", region).Result;
            string url2 = GetParamValueAsync("MQ-Broker2URI", region).Result;
            string queueName = GetParamValueAsync("MQ-QueueName", region).Result;


            LambdaLogger.Log("queueName : " + queueName);
            LambdaLogger.Log("user : " + user);
            LambdaLogger.Log("password : " + password);
            LambdaLogger.Log("url : " + url1); ;
            LambdaLogger.Log("queueName : " + queueName);




            Uri connecturi = new Uri("activemq:" + url1);
            IConnectionFactory factory = new NMSConnectionFactory(connecturi);
            LambdaLogger.Log("Connected : " + connecturi.ToString());
            using (IConnection connection = factory.CreateConnection(user, password))
            {
                 
                connection.Start();
                 
                IDestination destination;
                using (ISession session = connection.CreateSession())
                {
                    destination = session.GetQueue(queueName);
                    using (IMessageProducer producer = session.CreateProducer(destination))
                    {
                        IMessage msg =  producer.CreateTextMessage("Test Message at " + DateTime.Now.ToString());
                        producer.Send(msg);
                        return msg.ToString();


                    }
                    
                }
            }



        }

        private static async Task<string> GetParamValueAsync(string paramName , string region )
        {
            
            try
            {

                Amazon.RegionEndpoint regionEndpoint = Amazon.RegionEndpoint.GetBySystemName(region);

                using (var client = new AmazonSimpleSystemsManagementClient(regionEndpoint))
                {
                    var request = new GetParameterRequest() { Name = paramName };
                    var response = await client.GetParameterAsync(request);
                    return response.Parameter.Value;


                }
            }
            catch (Exception ex)
            {
                LambdaLogger.Log("Error while gettign parameter " + paramName + " with message :" + ex.Message);
                return null;
            }
        }

    }
}
