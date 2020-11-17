package com.cqu.ebd.utils;

import java.io.File;

public class Log {

    private static final String EMPTY = "";
    private static String fileName = "/sdcard/VideoBle/log.txt";
    private static String filePath = "/sdcard";
    private static int logLevel = LogWrite.LEVEL_DEBUG;

    public static void setFileName(String path, String name) {
        filePath = path;
        fileName = path + File.separator + name;
    }
    public static void setFileName(String name) {
        fileName = name;
        int index = fileName.lastIndexOf("/");
        filePath = fileName.substring(0, index);
    }
    public static void setLogLevel(int level) {
        switch (level) {
            case 1:
                logLevel = LogWrite.LEVEL_DEBUG;break;
            case 2:
                logLevel = LogWrite.LEVEL_INFO;break;
            case 3:
                logLevel = LogWrite.LEVEL_WARNING;break;
            case 4:
                logLevel = LogWrite.LEVEL_ERROR;break;
            case 5:
                logLevel = LogWrite.LEVEL_ERROR;break;
            default:
                logLevel = LogWrite.LEVEL_VERBOSE;break;
        }
    }
    public static String getFilePath() {
        return filePath;
    }

    public static int getLogLevel() {
        return logLevel;
    }

    /** 插入日志 */
    public static void d(String logTag, String logStr) {
        writeLogToFile(logTag, logStr, LogWrite.LEVEL_DEBUG);
        android.util.Log.d(logTag, logStr);
    }
    public static void d(String logTag, String logStr, Throwable e) {
        String exceptionStr = logStr + '\n' + android.util.Log.getStackTraceString(e);
        writeLogToFile(logTag, exceptionStr, LogWrite.LEVEL_DEBUG);
        android.util.Log.d(logTag, logStr, e);
    }

    public static void i(String logTag, String logStr) {
        writeLogToFile(logTag, logStr, LogWrite.LEVEL_INFO);
        android.util.Log.i(logTag, logStr);
    }
    public static void i(String logTag, String logStr, Throwable e) {
        String exceptionStr = logStr + '\n' + android.util.Log.getStackTraceString(e);
        writeLogToFile(logTag, exceptionStr, LogWrite.LEVEL_INFO);
        android.util.Log.i(logTag, logStr, e);
    }

    public static void w(String logTag, String logStr) {
        writeLogToFile(logTag, logStr, LogWrite.LEVEL_WARNING);
        android.util.Log.w(logTag, logStr);
    }
    public static void w(String logTag, String logStr, Throwable e) {
        String exceptionStr = logStr + '\n' + android.util.Log.getStackTraceString(e);
        writeLogToFile(logTag, exceptionStr, LogWrite.LEVEL_WARNING);
        android.util.Log.w(logTag, logStr, e);
    }

    public static void e(String logTag, String logStr) {
        writeLogToFile(logTag, logStr, LogWrite.LEVEL_ERROR);
        android.util.Log.e(logTag, logStr);
    }
    public static void e(String logTag, String logStr, Throwable e) {
        String exceptionStr = logStr + '\n' + android.util.Log.getStackTraceString(e);
        writeLogToFile(logTag, exceptionStr, LogWrite.LEVEL_ERROR);
        android.util.Log.e(logTag, logStr, e);
    }
    public static void e(String logTag, Throwable e) {
        e(logTag, "", e);
    }
    public static void v(String logTag, String logStr) {
        android.util.Log.v(logTag, logStr);
    }

    private static void writeLogToFile(String logTag, String logStr, int infoLevel) {
        writeLogToFile(fileName, logLevel, logTag, logStr, infoLevel);
    }

    private synchronized static void writeLogToFile(String fileName, int fileOpenLevel,
            String logTag, String logStr, int infoLevel) {

        if(!EMPTY.equals(fileName)) {
            LogWrite loginstance = LogWrite.open(fileName, fileOpenLevel);
            if(loginstance!=null) {
                try {
                    if(loginstance.getFileNum()!=3) {
                        loginstance.setFileNum(3);
                    }
                    if(loginstance.getFileSize()!=2) {
                        loginstance.setFileSize(2);
                    }
                    switch(infoLevel) {
                    case LogWrite.LEVEL_DEBUG:
                        loginstance.debug(logTag, logStr);
                        break;
                    case LogWrite.LEVEL_INFO:
                        loginstance.info(logTag, logStr);
                        break;
                    case LogWrite.LEVEL_ERROR:
                        loginstance.error(logTag, logStr);
                        break;
                    case LogWrite.LEVEL_WARNING:
                        loginstance.warning(logTag, logStr);
                        break;
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    loginstance.close();
                }
            }
        }
    }

}
