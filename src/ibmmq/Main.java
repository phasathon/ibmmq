package ibmmq;


public class Main {

	public static void main(String[] args) {
		Logger logger = new Logger();
		logger.log("start");
		try {
			Message.generateMessage(logger);
		} catch (Exception e) {
			logger.log("exception: "+e.getMessage());
			e.printStackTrace();
		}
	}

}
