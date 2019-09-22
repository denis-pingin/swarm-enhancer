package swarm.enhancer.utils;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import swarm.enhancer.foursquare.FileTokenStore;

public class Log {
    private static final String ACTIVITY_LOG_FILENAME = "activity.log";
    private static final int ACTIVITY_LOG_LIMIT = 200;

    private static List<String> activityLog;

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
        logActivity(context, message);
    }

    public static void w(Class clazz, Context context, String message, boolean showToast) {
        android.util.Log.w(clazz.getSimpleName(), message);
        if (showToast) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
        logActivity(context, message);
    }

    public static void e(Class clazz, Context context, String message, boolean showToast) {
        android.util.Log.e(clazz.getSimpleName(), message);
        if (showToast) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
        logActivity(context, message);
    }

    public static void e(Class clazz, Context context, String message, boolean showToast, Throwable throwable) {
        android.util.Log.e(clazz.getSimpleName(), message, throwable);
        if (showToast) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
        logActivity(context, message);
    }

    public static String getActivityLog(Context context) {
        if (activityLog == null) {
            activityLog = readActivityLogFromFile(context);
        }

        return activityLog.stream()
                .map(value -> "- " + value)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private static void logActivity(Context context, String text) {
        if (activityLog == null) {
            activityLog = readActivityLogFromFile(context);
        }

        try {
            // Build logged value
            String logValue = DateFormat.format("yyyy-MM-dd HH:mm:ss", new Date()).toString() + " - " + text;

            addActivityToLog(logValue);
            writeActivityLogToFile(context, logValue);
            triggerActivityUpdatedEvent(context);
        } catch (Exception e) {
            android.util.Log.d(FileTokenStore.class.getSimpleName(), e.getMessage(), e);
        }
    }

    private static void addActivityToLog(String text) {
        activityLog.add(0, text);
        while (activityLog.size() > ACTIVITY_LOG_LIMIT) {
            activityLog.remove(activityLog.size() - 1);
        }
    }

    private static List<String> readActivityLogFromFile(Context context) {
        try {
            FileInputStream fileInputStream = context.openFileInput(ACTIVITY_LOG_FILENAME);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            List<String> activityLog = bufferedReader.lines()
                    .limit(ACTIVITY_LOG_LIMIT)
                    .collect(Collectors.toList());

            Collections.reverse(activityLog);

            return activityLog;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static void writeActivityLogToFile(Context context, String value) throws IOException {
        FileOutputStream fileOutputStream = context.openFileOutput(ACTIVITY_LOG_FILENAME, Context.MODE_APPEND);

        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

        bufferedWriter.write(value);
        bufferedWriter.write(System.lineSeparator());
        bufferedWriter.flush();

        fileOutputStream.close();
    }

    private static void triggerActivityUpdatedEvent(Context context) {
        Intent intent = new Intent("activity-updated");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
