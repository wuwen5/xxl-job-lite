package com.xxl.job.core.log;

import com.xxl.job.core.biz.model.LogResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * store trigger log in each log-file
 * @author xuxueli 2016-3-12 19:25:12
 */
@UtilityClass
@Slf4j
public class XxlJobFileAppender {

    /**
     * log base path
     *
     * strut like:
     * 	---/
     * 	---/gluesource/
     * 	---/gluesource/10_1514171108000.js
     * 	---/gluesource/10_1514171108000.js
     * 	---/2017-12-25/
     * 	---/2017-12-25/639.log
     * 	---/2017-12-25/821.log
     *
     */
    private static String logBasePath = "/data/applogs/xxl-job/jobhandler";

    private static String glueSrcPath = logBasePath.concat("/gluesource");

    public static void initLogPath(String logPath) {
        // init
        if (logPath != null && !logPath.trim().isEmpty()) {
            logBasePath = logPath;
        }
        // mk base dir
        File logPathDir = new File(logBasePath);
        if (!logPathDir.exists() && !logPathDir.mkdirs()) {
            log.warn("Failed to create log base directory: {}", logBasePath);
        }
        logBasePath = logPathDir.getPath();

        // mk glue dir
        File glueBaseDir = new File(logPathDir, "gluesource");
        if (!glueBaseDir.exists() && !glueBaseDir.mkdirs()) {
            log.warn("Failed to create glue source directory: {}", glueBaseDir);
        }
        glueSrcPath = glueBaseDir.getPath();
    }

    public static String getLogPath() {
        return logBasePath;
    }

    public static String getGlueSrcPath() {
        return glueSrcPath;
    }

    /**
     * log filename, like "logPath/yyyy-MM-dd/9999.log"
     *
     * @param triggerDate
     * @param logId
     * @return
     */
    public static String makeLogFileName(Date triggerDate, long logId) {

        // filePath/yyyy-MM-dd
        // avoid concurrent problem, can not be static
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        File logFilePath = new File(getLogPath(), sdf.format(triggerDate));
        if (!logFilePath.exists() && !logFilePath.mkdir()) {
            log.warn("Failed to create log file directory: {}", logFilePath);
        }

        // filePath/yyyy-MM-dd/9999.log
        return logFilePath
                .getPath()
                .concat(File.separator)
                .concat(String.valueOf(logId))
                .concat(".log");
    }

    /**
     * append log
     *
     * @param logFileName
     * @param appendLog
     */
    public static void appendLog(String logFileName, String appendLog) {

        // log file
        if (logFileName == null || logFileName.trim().isEmpty()) {
            return;
        }
        File logFile = new File(logFileName);

        if (!logFile.exists()) {
            try {
                if (!logFile.createNewFile()) {
                    log.warn("Failed to create log file: {}", logFile);
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return;
            }
        }

        // log
        if (appendLog == null) {
            appendLog = "";
        }
        appendLog += "\r\n";

        // append file content
        try (FileOutputStream fos = new FileOutputStream(logFile, true)) {
            fos.write(appendLog.getBytes(StandardCharsets.UTF_8));
            fos.flush();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * support read log-file
     *
     * @param logFileName
     * @return log content
     */
    public static LogResult readLog(String logFileName, int fromLineNum) {

        // valid log file
        if (logFileName == null || logFileName.trim().isEmpty()) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not found", true);
        }
        File logFile = new File(logFileName);

        if (!logFile.exists()) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not exists", true);
        }

        // read file
        StringBuilder logContentBuffer = new StringBuilder();
        int toLineNum = 0;
        try (LineNumberReader reader = new LineNumberReader(
                new InputStreamReader(Files.newInputStream(logFile.toPath()), StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                // [from, to], start as 1
                toLineNum = reader.getLineNumber();
                if (toLineNum >= fromLineNum) {
                    logContentBuffer.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        // result
        return new LogResult(fromLineNum, toLineNum, logContentBuffer.toString(), false);
    }

    /**
     * read log data
     * @param logFile
     * @return log line content
     */
    public static String readLines(File logFile) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(logFile.toPath()), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
