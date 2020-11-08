package com.cqu.ebd.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

public final class LogWrite {
    public static final int LEVEL_VERBOSE = 0;
    public static final int LEVEL_DEBUG = 1;
    public static final int LEVEL_INFO = 2;
    public static final int LEVEL_WARNING = 3;
    public static final int LEVEL_ERROR = 4;

    public static final int WRITE_LOG_SUCCESS = 0;
    public static final int WRITE_LOG_FAIL = 1;
    public static final int NONE_INSTANCE_FOUND_FAIL = 3;
    public static final int WRITE_LOG_LEVEL_ERROR = -1;

    public static final int CLEAR_LOG_SUCCESS = 0;
    public static final int CLEAR_OLD_FAIL = 1;
    public static final int CLEAR_LOG_FAIL = 2;

    private static final char[] LOG_LEVEL = {'V', 'D', 'I', 'W', 'E'};
    //    private static final int LOG_TAG_MAX_LENGTH = 32;
//    private static final int LOG_CONTENT_MAX_LENGTH = 1024;
    private static final String FORMAT_UTF8 = "UTF-8";
    private static final String BLANK_SPACE = " ";
    private static final String FILE_TYPE = ".txt";
    private static final String H_LINE = "_";
    private static final String SLANT = "/";
    private static final String EMPTY = "";
    private static ReentrantLock locker = new ReentrantLock();
    private String logPath;
    private int logLevel;
    private File file;
    private BufferedWriter bufferedWriter = null;
    private OutputStreamWriter outputStreamWriter = null;
    private FileOutputStream fileOutputStream = null;

    private final long defaultFileSize = 524288L;  //512*1024
    private final int defaultFileNum = 2;
    private final long maxFileSize = 5242880L;      //5*1024*1024
    private final int maxFileNum = 10;
    private int fileNum = 2;
    private long fileSize = 524288L;       //512*1024

    private static HashMap<String, LogWrite> hashMap = new HashMap<String, LogWrite>();

    public String toString() {
        return "Log{logPath='" + this.logPath + '\'' + ", logLevel="
                + this.logLevel + ", file=" + this.file + ", bw=" + this.bufferedWriter
                + ", defaultFileSize=" + defaultFileSize + ", defaultFileNum=" + defaultFileNum
                + ", maxFileSize=" + maxFileSize + ", maxFileNum=" + maxFileNum
                + ", fileNum=" + this.fileNum + ", fileSize=" + this.fileSize
                + '}';
    }

    private LogWrite(String logPath) {
        this.logPath = logPath;
    }

    public int getMinLevel() {
        return (this.logLevel == LEVEL_VERBOSE) ? LEVEL_DEBUG : this.logLevel;
    }

    public void setMinLevel(int level) {
        if ((level < LEVEL_VERBOSE) || (level > LEVEL_ERROR)) {
            return;
        }
        this.logLevel = level;
    }

    public void setFileNum(int fileNum) {
        if (fileNum > maxFileNum) {
            this.fileNum = maxFileNum;
        } else if (fileNum < defaultFileNum) {
            this.fileNum = defaultFileNum;
        } else
            this.fileNum = fileNum;
    }

    public int getFileNum() {
        return this.fileNum;
    }

    public void setFileSize(long fileSize) {
        if (fileSize > maxFileSize) {
            this.fileSize = maxFileSize;
        } else if (fileSize < defaultFileSize) {
            this.fileSize = defaultFileSize;
        } else
            this.fileSize = fileSize;
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public static LogWrite open(String fileName, int level) {

        if ((checkFilename(fileName)) || (checkLevelLegal(level))) {
            return null;
        }
        if (!fileName.endsWith(FILE_TYPE)) {
            fileName = fileName + FILE_TYPE;
        }
        // android.util.Log.d("Log", "add FILE_TYPE : " + fileName);
        LogWrite log = (LogWrite) hashMap.get(fileName);
        if (log == null) {
            // android.util.Log.d("Log", "log is null : " + fileName);
            locker.lock();
            try {
                log = new LogWrite(fileName);
                log.create(fileName);
                log.logLevel = level;
                hashMap.put(fileName, log);
            } catch (IOException e) {
                return null;
            } finally {
                locker.unlock();
            }

        }
        return log;
    }

    public synchronized void close() {
        try {
            if (this.bufferedWriter != null) {
                this.bufferedWriter.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (this.outputStreamWriter != null) {
                this.outputStreamWriter.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (this.fileOutputStream != null) {
                this.fileOutputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        hashMap.remove(this.logPath);
    }

    public synchronized int clear() {
        LogWrite log = (LogWrite) hashMap.get(this.logPath);

        if (log == null) {
            return NONE_INSTANCE_FOUND_FAIL;
        }

        File tFile = null;
        String fileN = getFileName();
        for (int i = 1; i < this.fileNum; ++i) {
            tFile = new File(fileN + H_LINE + i + FILE_TYPE);
            if (!tFile.exists())
                break;
            tFile.delete();
        }

        try {
            if (null != this.bufferedWriter) {
                this.bufferedWriter.close();
            }
            if (this.outputStreamWriter != null) {
                this.outputStreamWriter.close();
            }
            if (this.fileOutputStream != null) {
                this.fileOutputStream.close();
            }
            this.bufferedWriter = createBufferedWriter(this.file, false);
        } catch (IOException e) {
            return CLEAR_LOG_FAIL;
        }

        return CLEAR_LOG_SUCCESS;
    }

    public int debug(String tag, String content) throws NullPointerException {
        return write(LEVEL_DEBUG, tag, content);
    }

    public int info(String tag, String content) throws NullPointerException {
        return write(LEVEL_INFO, tag, content);
    }

    public int warning(String tag, String content) throws NullPointerException {
        return write(LEVEL_WARNING, tag, content);
    }

    public int error(String tag, String content) throws NullPointerException {
        return write(LEVEL_ERROR, tag, content);
    }

    private static boolean checkLevelLegal(int level) {
        return (level != LEVEL_DEBUG) && (level != LEVEL_INFO) && (level != LEVEL_ERROR) && (level != LEVEL_VERBOSE)
                && (level != LEVEL_WARNING);
    }

    private static boolean checkFilename(String fileName) {
        if (fileName == null) {
            return true;
        }
        if (EMPTY.equals(fileName.trim())) {
            return true;
        }

        return fileName.endsWith(SLANT);
    }

    private void create(String fileName) throws IOException {
        File pFile = new File(fileName);
        if ((!pFile.isFile()) && (!pFile.isDirectory())) {
            File parentFile = pFile.getParentFile();
            if (null == parentFile) {
                throw new IOException();
            }
            parentFile.mkdirs();
        }
        this.file = pFile;
        this.logPath = fileName;
        this.bufferedWriter = createBufferedWriter(pFile, true);
    }

    private BufferedWriter createBufferedWriter(File pFile, boolean isAppend)
            throws IOException {
        fileOutputStream = new FileOutputStream(pFile, isAppend);
        outputStreamWriter = new OutputStreamWriter(fileOutputStream, FORMAT_UTF8);
        return new BufferedWriter(outputStreamWriter);
    }

    private synchronized int write(int lev, String tag, String content) {
        int flag = WRITE_LOG_SUCCESS;

        LogWrite log = (LogWrite) hashMap.get(this.logPath);
        if (log == null) {
            throw new NullPointerException("This Log Object is closed!");
        }

        if (lev < this.logLevel) {
            return WRITE_LOG_LEVEL_ERROR;
        }
        try {
//            checkStrParam(tag, LOG_TAG_MAX_LENGTH);
//            checkStrParam(content, LOG_CONTENT_MAX_LENGTH);
            content = getLogContent(lev, tag, content, "yyyy-MM-dd HH:mm:ss.SSS");
            if (writeFile(content)) {
                return WRITE_LOG_FAIL;
            }
        } catch (IOException e) {
            flag = WRITE_LOG_FAIL;
        } catch (RuntimeException e) {
            flag = WRITE_LOG_FAIL;
        }
        return flag;
    }

    private boolean writeFile(String content) throws IOException {
        if (this.file.length() + content.length() < this.fileSize) {
            if (writeToFile(content)) {
                return true;
            }
        } else {
            deleteOldFile();
            if (writeToFile(content)) {
                return true;
            }
        }
        return false;
    }

    private boolean writeToFile(String content) throws IOException {
        if (null == this.bufferedWriter) {
            return true;
        }

        this.bufferedWriter.write(content);
        this.bufferedWriter.flush();

        return false;
    }

    private String getLogContent(int lev, String tag, String content,
                                 String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date()));
        sb.append(BLANK_SPACE);
        sb.append(LOG_LEVEL[lev]);
        sb.append(BLANK_SPACE).append(String.format(Locale.getDefault(), "%20s", tag)).append(BLANK_SPACE).append(content).append("\n");

        return sb.toString();
    }

    private String getFileName() {
        int index = this.logPath.lastIndexOf(FILE_TYPE);
        if (index == -1) {
            return this.logPath;
        }
        return this.logPath.substring(0, index);
    }

    private void deleteOldFile() throws IOException {
        if (null != this.bufferedWriter) {
            this.bufferedWriter.close();
        }

        String fileN = getFileName();
        File tFile = null;
        for (int i = this.fileNum - 1; i > 0; --i) {
            tFile = new File(fileN + H_LINE + i + FILE_TYPE);
            if (!tFile.exists()) {
                continue;
            }

            if (!tFile.exists()) {
                continue;
            }
            if (i != this.fileNum - 1) {
                tFile.renameTo(new File(fileN + H_LINE + (i + 1) + FILE_TYPE));
            } else {
                tFile.delete();
            }
        }

        this.file.renameTo(new File(fileN + H_LINE + 1 + FILE_TYPE));
        this.file = new File(this.logPath);
        this.fileOutputStream = new FileOutputStream(this.file, true);
        this.outputStreamWriter = new OutputStreamWriter(this.fileOutputStream, FORMAT_UTF8);
        this.bufferedWriter = new BufferedWriter(this.outputStreamWriter);
    }

    public int verbose(String tag, String content) throws NullPointerException {
        return write(LEVEL_VERBOSE, tag, content);
    }
}