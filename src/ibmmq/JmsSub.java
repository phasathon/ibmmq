package ibmmq;

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

import com.ibm.mq.jms.MQDestination;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.*;

/**
 * A minimal and simple application for Point-to-point messaging.
 * <p>
 * Application makes use of fixed literals, any customisations will require
 * re-compilation of this source file. Application assumes that the named queue
 * is empty prior to a run.
 * <p>
 * Notes:
 * <p>
 * API type: JMS API (v2.0, simplified domain)
 * <p>
 * Messaging domain: Point-to-point
 * <p>
 * Provider type: IBM MQ
 * <p>
 * Connection mode: Client connection
 * <p>
 * JNDI in use: No
 */
public class JmsSub {

    // System exit status value (assume unset value to be 1)
    private static int status = 1;

    // Create variables for the connection to MQ
    private static final String HOST = "localhost"; // Host name or IP address
    private static final int PORT = 1414; // Listener port for your queue manager
    private static final String CHANNEL = "DEV.APP.SVRCONN"; // Channel name
    private static final String QMGR = "QM1"; // Queue manager name
    private static final String APP_USER = "admin"; // User name that application uses to connect to MQ
    private static final String APP_PASSWORD = "passw0rd"; // Password that the application uses to connect to MQ
    private static final String TOPIC_NAME = "DEV.APP.TOPIC"; // Topic that the application uses to pub and sub messages to and from

    /**
     * Main method
     *
     * @param args
     */
    public static void main(String[] args) {

        // Variables
        JMSContext context = null;
        Destination destination = null;
        JMSConsumer subscriber = null;

        System.setProperty("javax.net.ssl.trustStore", "D:\\bay_ibm_mq\\bay_ibm_mq\\certs\\clientkey.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");
        System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings", "false");

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
            cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, false);
            //cf.setStringProperty(WMQConstants.USERID, APP_USER);
            //cf.setStringProperty(WMQConstants.PASSWORD, APP_PASSWORD);
            //cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "*TLS12ORHIGHER");
            cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384");
            //cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "SSL_ECDHE_RSA_WITH_AES_256_CBC_SHA384");

            // Create JMS objects
            context = cf.createContext();
            destination = context.createTopic("topic://" + TOPIC_NAME);

            long uniqueNumber = System.currentTimeMillis() % 1000;
            TextMessage message = context.createTextMessage("Your lucky number today is " + uniqueNumber);

            setTargetClient(destination);

            subscriber = context.createConsumer(destination);

            System.out.println("Subscriber created");

            while (true) {
                try {
                    Message receivedMessage = subscriber.receive();
                    getAndDisplayMessageBody(receivedMessage);
                } catch (JMSRuntimeException jmsex) {

                    jmsex.printStackTrace();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }

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

    private static void getAndDisplayMessageBody(Message receivedMessage) {
        if (receivedMessage instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) receivedMessage;
            try {
                System.out.println("Received message: " + textMessage.getText());
            } catch (JMSException jmsex) {
                recordFailure(jmsex);
            }
        } else if (receivedMessage instanceof Message) {
            System.out.println("Message received was not of type TextMessage.\n");
        } else {
            System.out.println("Received object not of JMS Message type!\n");
        }
    }
}
