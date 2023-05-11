package ibmmq;

public class Main {

	public static void main(String[] args) {
		try {
			Message.generateMessage();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
