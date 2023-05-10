package ibmmq;

/*
 /*
 * (c) Copyright IBM Corporation 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//package com.ibm.mq.samples.jms;


import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;

import com.ibm.mq.jms.MQDestination;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

/**
 * A minimal and simple application for Point-to-point messaging.
 * <p>
 * Application makes use of fixed literals, any customisations will require
 * re-compilation of this source file. Application assumes that the named queue
 * is empty prior to a run.
 * Notes:
 * API type: JMS API (v2.0, simplified domain)
  * Messaging domain: Point-to-point
  * Provider type: IBM MQ
  * Connection mode: Client connection
 
 * JNDI in use: No
 */
public class JmsPub {

    // System exit status value (assume unset value to be 1)
    private static int status = 1;

    // Create variables for the connection to MQ
    private static final String HOST = "uat.emq.krungsri.net"; // Host name or IP address
    private static final int PORT = 1419; // Listener port for your queue manager
    private static final String CHANNEL = "S1.SSL.SVRCONN"; // Channel name
    private static final String QMGR = "EMQ01"; // Queue manager name
    private static final String APP_USER = "apps1"; // User name that application uses to connect to MQ
    private static final String APP_PASSWORD = "SUdec#22"; // Password that the application uses to connect to MQ
    // private static final String QUEUE_NAME = "DEV.QUEUE.1"; // Queue that the application uses to put and get messages to and from
    private static final String TOPIC_NAME = "BANKSTATUS"; // Topic that the application uses to pub and sub messages to and from

    /**
     * Main method
     *
     * @param args
     */
    public static void main(String[] args) {

        // Variables
        JMSContext context = null;
        Destination destination = null;
        JMSProducer publisher = null;

        //System.setProperty("javax.net.ssl.trustStore", "E:\\MyKhanaphoz\\JAVA Project\\my_truststore.jks");
        //System.setProperty("javax.net.ssl.trustStorePassword", "SecureTrustStore123");
        System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings","false");
        try {
            // Create a connection factory
            JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
            JmsConnectionFactory cf = ff.createConnectionFactory();

            // Set the properties
            cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, HOST);
            cf.setIntProperty(WMQConstants.WMQ_PORT, PORT);
            cf.setStringProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
            cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
            cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QMGR);
            cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "JmsPub (JMS)");
            cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
            cf.setStringProperty(WMQConstants.USERID, APP_USER);
            cf.setStringProperty(WMQConstants.PASSWORD, APP_PASSWORD);
            cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384");
            //cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "*TLS12");

            // Create JMS objects
            context = cf.createContext();
            destination = context.createTopic("topic://" + TOPIC_NAME);

            long uniqueNumber = System.currentTimeMillis() % 1000;
            TextMessage message = context.createTextMessage("Your lucky number today is " + uniqueNumber);

            setTargetClient(destination);

            publisher = context.createProducer();
            publisher.send(destination, message);
            System.out.println("Publish message:\n" + message);

            context.close();

            recordSuccess();
        } catch (JMSException jmsex) {
            recordFailure(jmsex);
        }

        System.exit(status);

    } // end main()

    /**
     * Record this run as successful.
     */
    private static void recordSuccess() {
        System.out.println("SUCCESS");
        status = 0;
        return;
    }

    /**
     * Record this run as failure.
     *
     * @param ex
     */
    private static void recordFailure(Exception ex) {
        if (ex != null) {
            if (ex instanceof JMSException) {
                processJMSException((JMSException) ex);
            } else {
                System.out.println(ex);
            }
        }
        System.out.println("FAILURE");
        status = -1;
        return;
    }

    /**
     * Process a JMSException and any associated inner exceptions.
     *
     * @param jmsex
     */
    private static void processJMSException(JMSException jmsex) {
        System.out.println(jmsex);
        Throwable innerException = jmsex.getLinkedException();
        if (innerException != null) {
            System.out.println("Inner exception(s):");
        }
        while (innerException != null) {
            System.out.println(innerException);
            innerException = innerException.getCause();
        }
        return;
    }

    private static void setTargetClient(Destination destination) {
        try {
            MQDestination mqDestination = (MQDestination) destination;
            mqDestination.setTargetClient(WMQConstants.WMQ_CLIENT_NONJMS_MQ);
        } catch (JMSException jmsex) {
            System.out.println("Unable to set target destination to non JMS");
        }
    }
}
