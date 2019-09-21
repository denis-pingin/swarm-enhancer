package swarm.enhancer.utils;

import android.content.Context;
import android.widget.Toast;

public class Log {
    public static void d(Class clazz, Context context, String message, boolean showToast) {
        android.util.Log.d(clazz.getSimpleName(), message);
        if (showToast) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static void i(Class clazz, Context context, String message, boolean showToast) {
        android.util.Log.i(clazz.getSimpleName(), message);
        if (showToast) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static void i(Class clazz, Context context, String message, boolean showToast, Throwable throable) {
        android.util.Log.i(clazz.getSimpleName(), message, throable);
        if (showToast) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static void w(Class clazz, Context context, String message, boolean showToast) {
        android.util.Log.w(clazz.getSimpleName(), message);
        if (showToast) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static void w(Class clazz, Context context, String message, boolean showToast, Throwable throable) {
        android.util.Log.w(clazz.getSimpleName(), message, throable);
        if (showToast) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static void e(Class clazz, Context context, String message, boolean showToast) {
        android.util.Log.e(clazz.getSimpleName(), message);
        if (showToast) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static void e(Class clazz, Context context, String message, boolean showToast, Throwable throable) {
        android.util.Log.e(clazz.getSimpleName(), message, throable);
        if (showToast) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}
