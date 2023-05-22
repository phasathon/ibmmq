package ibmmq;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Message {
	

	private Message() {
		throw new IllegalStateException("Utility class");
	}

	public static void generateMessage(Logger logger) throws SQLException, IOException {

		Properties prop = loadProp();

		java.sql.Connection dbConnection = connectDb(prop,logger);
		Statement statement = dbConnection.createStatement();
		ResultSet resultSet = execute(statement, prop);

		JSONArray records = makeResult(resultSet,logger);

		closeConnection(resultSet, statement, dbConnection);

		String jsonString = convertJsonToString(records);
		logger.log("record: "+jsonString);
		JmsPub.produce(jsonString, prop,logger);

	}

	private static Properties loadProp() throws IOException {
		InputStream input = new FileInputStream("./resources/application.properties");
		Properties prop = new Properties();
		prop.load(input);
		return prop;
	}

	private static ResultSet execute(Statement statement, Properties prop) throws SQLException {
		String sql = prop.getProperty("db.sql");
		return statement.executeQuery(sql);
	}

	private static java.sql.Connection connectDb(Properties prop,Logger logger) throws SQLException {
		logger.log("connect db");
		String dbUrl = prop.getProperty("db.url");
		String dbUsername = prop.getProperty("db.user");
		String dbPassword = prop.getProperty("db.password");
		return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
	}

	private static void closeConnection(ResultSet resultSet, Statement statement, java.sql.Connection dbConnection)
			throws SQLException {
		resultSet.close();
		statement.close();
		dbConnection.close();
	}

	private static String convertJsonToString(JSONArray records) {
		JSONObject json = new JSONObject();
		json.put("records", records);
		return json.toString();
	}

	private static JSONArray makeResult(ResultSet resultSet,Logger logger) throws SQLException {

		int numberJson = 0;

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		String formattedTime = LocalDateTime.now().format(formatter);

		JSONArray records = new JSONArray();
		
		int count = 0;
		
		while (resultSet.next()) {

			String toBank = resultSet.getString("TO_BANK");
			String exceedsTh = resultSet.getString("EXCEEDS_THRESHOLD");
			String toBankDesc = resultSet.getString("TO_BANK_DESC");
			numberJson++; // Incremented for each record.

			// Format the number as a string with leading zeroes if it's between 1 and 9.
			String formattedNumber = "";

			if (numberJson >= 1 && numberJson <= 9) {
				formattedNumber = "0" + numberJson;
			} else {
				formattedNumber = Integer.toString(numberJson);
			}

			boolean healthy = getHealty(toBank, exceedsTh);

			// Create a JSON object for each record
			JSONObject recordJson = new JSONObject();
			recordJson.put("id", formattedNumber);
			JSONObject fields = new JSONObject();
			fields.put("bankId", "0"+toBank);
			fields.put("bankCode", toBankDesc);
			fields.put("isHealthy", healthy);
			fields.put("eventDateTime", formattedTime + "+07:00");

			// Add the fields object to the record
			recordJson.put("fields", fields);

			// Add the record to the records array
			records.put(recordJson);
			count++;

		}
		
		logger.log("row size :"+count);
		
		return records;
	}

	private static boolean getHealty(String toBank, String exceedsTh) {
		boolean healthy = false;
		List<String> toBankList = new ArrayList<>();
		toBankList.add("02");
		toBankList.add("06");
		toBankList.add("14");
		toBankList.add("04");
		toBankList.add("11");
		toBankList.add("30");
		if ("0".equals(exceedsTh) && toBankList.contains(toBank)) {
			healthy = true;
		}

		return healthy;
	}
}
