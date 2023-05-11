package ibmmq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final String LOG_FILE_NAME = "service.log";
    private static final String LOG_DIRECTORY = "./logs/";

    private File logFile;

    public Logger() {
        // create the logs directory if it does not exist
        File logDir = new File(LOG_DIRECTORY);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        // create or get the log file
        this.logFile = new File(LOG_DIRECTORY + LOG_FILE_NAME);
        if (this.logFile.exists()) {
            backupLogFile();
        }
    }

    private void backupLogFile() {
        // get the current date and time
        LocalDateTime now = LocalDateTime.now().minusDays(1);
        String formattedDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // rename the existing log file by appending the current date and time to its name
        String backupFileName = LOG_DIRECTORY + LOG_FILE_NAME + "." + formattedDate;
        File backupFile = new File(backupFileName);
        this.logFile.renameTo(backupFile);

        // create a new log file
        try {
            this.logFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(String message) {
        try (FileWriter writer = new FileWriter(this.logFile, true)) {
            // write the new log message to the new log file
            LocalDateTime now = LocalDateTime.now();
            String formattedDate = now.format(DateTimeFormatter.ISO_DATE_TIME);
            writer.write(formattedDate + " ->> " + message + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

