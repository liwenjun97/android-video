package com.wzh.yuvwater.utils;

import android.content.Context;
import android.os.StatFs;
import android.text.TextUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class Utils  extends FileUtil{

    public static final String VIDEO_FILE_START_WITH = "part";
    private static String TAG = "MonitorUtils";
    private static final int MIN_SPACE = 500;//最小剩余可用空间500M

    /**
     * 同一天中， 多次重启打开应用，生成分段，最好的情况是每天只有一个分段
     */
    public static String getVideoFilePath() {
        String ymd = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
        String rootFolder = "/sdcard/VideoBle/Video" + File.separator + ymd;
        FileUtil.mkdirs(rootFolder);
        File folder = new File(rootFolder);
        String pattern = "yyyy-MM-dd HH:mm:ss:SSS";//日期格式
        SimpleDateFormat mFormat = new SimpleDateFormat(pattern, Locale.CHINA);
        String date = mFormat.format(new Date());
        Logger1.i(TAG, "date: ",date);

        String newVideoPath = null;
        if (folder.exists()) {
            newVideoPath = rootFolder + "/" + date + ".mp4";
        }
        if (TextUtils.isEmpty(newVideoPath)) {
            newVideoPath = rootFolder + "/" + date + ".mp4";
        }
        return newVideoPath;
    }
    public static String read() {
        return "bb";
    }

        public static int getScreenW(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenH(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static void close(Closeable is) {
        if (is != null)
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
}
