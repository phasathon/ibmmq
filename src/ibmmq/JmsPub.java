package ibmmq;

import java.util.Properties;

import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;

import com.ibm.mq.jms.MQDestination;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsConstants;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

public class JmsPub {

	private JmsPub() {
		throw new IllegalStateException("Utility class");
	}

	private static int status = 1;

	public static void produce(String msg, Properties prop,Logger logger) {

		// Variables
		JMSContext context = null;
		Destination destination = null;
		JMSProducer publisher = null;

		if("true".equals(prop.getProperty("selfTrustStore.enable"))) {
			System.setProperty("javax.net.ssl.trustStore", prop.getProperty("selfTrustStore.path"));
			System.setProperty("javax.net.ssl.trustStorePassword", "password");
		}

		System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings", "false");

		try {

			JmsConnectionFactory cf = setContext(prop);

			// Create JMS objects
			context = cf.createContext();
			destination = context.createTopic("topic://" + prop.getProperty("mq.topic"));
			
			logger.log("produce message");
			TextMessage message = context.createTextMessage(msg);
			message.setIntProperty(JmsConstants.JMS_IBM_RETAIN, JmsConstants.RETAIN_PUBLICATION);

			setTargetClient(destination);

			publisher = context.createProducer();
			publisher.send(destination, message);
			System.out.println("Publish message:\n" + message);

			context.close();

			recordSuccess();
			
			logger.log("success");
		} catch (JMSException jmsex) {
			logger.log("exception: "+jmsex.getMessage());
			recordFailure(jmsex);
		}

		System.exit(status);

	} // end main()

	private static JmsConnectionFactory setContext(Properties prop) throws JMSException {

//		String host = prop.getProperty("mq.host");
		String connectionNameList = prop.getProperty("mq.hostNameList");
		int port = Integer.parseInt(prop.getProperty("mq.port"));
		String channel = prop.getProperty("mq.channel");
		String qmgr = prop.getProperty("mq.qmgr");
		String user = prop.getProperty("mq.username");
		String password = prop.getProperty("mq.password");

		// Create a connection factory
		JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
		JmsConnectionFactory cf = ff.createConnectionFactory();

		// Set the properties
//		cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, host);
		cf.setStringProperty(WMQConstants.WMQ_CONNECTION_NAME_LIST, connectionNameList);
		cf.setIntProperty(WMQConstants.WMQ_PORT, port);
		cf.setStringProperty(WMQConstants.WMQ_CHANNEL, channel);
		cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
		cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, qmgr);
		cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "JmsPub (JMS)");
		cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
		cf.setStringProperty(WMQConstants.USERID, user);
		cf.setStringProperty(WMQConstants.PASSWORD, password);
		cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384");
		return cf;
	}

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
