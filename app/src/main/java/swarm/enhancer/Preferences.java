package swarm.enhancer;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
    private static final String PREFERENCES_NAME = "swarm-enhancer-preferences";
    private static final float DEFAULT_UPDATE_INTERVAL = 4;
    private static final float DEFAULT_LOCAL_RADIUS = 5;

    private static final float MIN_UPDATE_INTERVAL = 0.01f;
    private static final float MAX_UPDATE_INTERVAL = 48f;

    private static final float MIN_LOCAL_RADIUS = 0.1f;
    private static final float MAX_LOCAL_RADIUS = 100f;

    public static boolean areUpdatesPaused(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(context.getString(R.string.updates_paused_key), false);
    }

    public static void setUpdatesPaused(Context context, boolean updatesPaused) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(context.getString(R.string.updates_paused_key), updatesPaused);
        editor.apply();
    }

    public static float getUpdateInterval(Context context) {
        float updateInterval = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).getFloat(context.getString(R.string.update_interval_key), DEFAULT_UPDATE_INTERVAL);
        return limitFloat(updateInterval, MIN_UPDATE_INTERVAL, MAX_UPDATE_INTERVAL);
    }

    public static float setUpdateInterval(Context context, float updateInterval) {
        updateInterval = limitFloat(updateInterval, MIN_UPDATE_INTERVAL, MAX_UPDATE_INTERVAL);

        SharedPreferences.Editor editor = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        editor.putFloat(context.getString(R.string.update_interval_key), updateInterval);
        editor.apply();

        return updateInterval;
    }

    public static float getLocalRadius(Context context) {
        float localRadius = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).getFloat(context.getString(R.string.local_radius_key), DEFAULT_LOCAL_RADIUS);
        return limitFloat(localRadius, MIN_LOCAL_RADIUS, MAX_LOCAL_RADIUS);
    }

    public static float setLocalRadius(Context context, float localRadius) {
        localRadius = limitFloat(localRadius, MIN_LOCAL_RADIUS, MAX_LOCAL_RADIUS);

        SharedPreferences.Editor editor = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        editor.putFloat(context.getString(R.string.local_radius_key), localRadius);
        editor.apply();

        return localRadius;
    }

    private static float limitFloat(float updateInterval, float minValue, float maxValue) {
        if (updateInterval < minValue) {
            updateInterval = minValue;
        }
        if (updateInterval > maxValue) {
            updateInterval = maxValue;
        }
        return updateInterval;
    }
}
