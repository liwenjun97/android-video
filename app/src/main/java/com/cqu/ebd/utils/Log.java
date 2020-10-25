//package com.cqu.ebd.utils;
//
//import static java.util.logging.Level.INFO;
//
//public class Log {
//    private static final String EMPTY = "";
//    private static  String fileName = "/sdcard/logsdk.txt";
//    private static  String filePath = "/sdcard";
//    private static int logLevel = LogWrite.LEVEL_DEBUG;
//
//    public static void setFileName(String path,String name){
//        filePath = path;
//        fileName = name;
//    }
//    public static void setFileName(String name){
//        fileName = name;
//        int index = fileName.lastIndexOf("/");
//        filePath = fileName.substring(0,index);
//    }
//    public static void setLogLevel(int level){
//        switch (level){
//            case 1:
//                logLevel = LogWrite.LEVEL_DEBUG;break;
//            case 2:
//                logLevel = LogWrite.LEVEL_INFO;break;
//            case 3:
//                logLevel = LogWrite.LEVEL_WARNING;break;
//            case 4:
//                logLevel = LogWrite.LEVEL_ERROR;break;
//            default:
//                logLevel = LogWrite.LEVEL_VERBOSE;break;
//        }
//    }
//    public static String getFilePath(){
//        return filePath;
//    }
//    public static int getLogLevel(){
//        return logLevel;
//    }
//
//    //插入日志
//    public static void d(String logTag, String logStr) {
//        writeLogToFile(logTag, logStr, LogWrite.LEVEL_DEBUG);
//        android.util.Log.d(logTag,logStr);
//    }
//    public static void d(String logTag, String logStr, Throwable e) {
//        String exceptionstr = logStr + '\n' + android.util.Log.getStackTraceString(e);
//        writeLogToFile(logTag, exceptionstr, LogWrite.LEVEL_DEBUG);
//        android.util.Log.d(logTag, logStr, e);
//    }
//    public static void i(String logTag, String logStr) {
//        writeLogToFile(logTag, logStr, Logwrite.LEVEL_INFO);
//        android.util.Log.i(logTag, logStr);
//    }
//    public static void i(String logTag, String logStr, Throwable e) {
//        String exceptionstr = logStr + '\n' + android.util.Log.getStackTraceString(e);
//        writelogToFile(logTag, exceptionstr, LogWrite.LEVEL_INFO);
//        android.util.Log.i(logTag, logStr, e);
//    }
//
//    public static void w(String logTag, String logStr) {
//        writeLogToFile(logTag, logStr, LogWrite.LEVEL_WARNING);
//
//        android.util.Log.w(logTag, logStr);
//    }
//
//    public static void w(String logTag, String logStr, Throwable e) {
//        String exceptionstr = logStr + '\n' + android.util.Log.getStackTraceString(e);
//        writeLogToFile(logTag, exceptionstr, Logwrite.LEVEL_WARNING);
//        android.util.Log.w(logTag, logStr, e);
//    }
//
//    public static void e(String logTag, String logStr) {
//        writeLogToFile(logTag, logStr, Logwrite.LEVEL_ERROR);
//        android.util.Log.e(logTag, logStr);
//    }
//
//    public static void e(String logTag, String logStr, Throwable e) {
//        String exceptionstr = logStr + '\n' + android.util.Log.getStackTraceString(e);
//        writeLogToFile(logTag,  exceptionstr, LogWrite.LEVEL_ERROR);
//        android.util.Log.e(logTag, logStr, e);
//    }
//}
