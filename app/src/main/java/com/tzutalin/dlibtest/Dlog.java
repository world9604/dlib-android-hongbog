package com.tzutalin.dlibtest;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Created by taein on 2018-07-06.
 */
public class Dlog {

    private static boolean DEBUG = true;
    private static String LOG_TAG = "taein";

    public static final void e(String msg){
        if (DEBUG) Log.e(LOG_TAG, buildLogMsg(msg));
    }

    public static final void w(String msg){
        if (DEBUG) Log.w(LOG_TAG, buildLogMsg(msg));
    }

    public static final void d(String msg){
        if (DEBUG) Log.d(LOG_TAG, buildLogMsg(msg));
    }

    public static final void i(String msg){
        if (DEBUG) Log.i(LOG_TAG, buildLogMsg(msg));
    }

    public static final void v(String msg){
        if (DEBUG) Log.v(LOG_TAG, buildLogMsg(msg));
    }

    private static String buildLogMsg(String message) {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[4];

        StringBuilder sb = new StringBuilder();
        sb.append("[")
                .append(ste.getClassName())
                .append("::")
                .append(ste.getMethodName())
                .append("]  ")
                .append(message);

        return sb.toString();
    }

    /**
     * get Debug Mode
     * @param context
     * @return
     */
    public static void isDebuggable(Context context) {
        boolean debuggable = false;

        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo appinfo = pm.getApplicationInfo(context.getPackageName(), 0);
            debuggable = (0 != (appinfo.flags & ApplicationInfo.FLAG_DEBUGGABLE));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        DEBUG = debuggable;
    }

}
