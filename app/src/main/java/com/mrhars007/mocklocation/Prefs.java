package com.mrhars007.mocklocation;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class Prefs {
    private static final String PREF_NAME = "tiny_mock_prefs";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_STATUS = "status";

    private static final String KEY_FAV_COUNT = "fav_count";
    private static final String KEY_FAV_PREFIX_LAT = "fav_lat_";
    private static final String KEY_FAV_PREFIX_LON = "fav_lon_";
    private static final String KEY_FAV_PREFIX_NAME = "fav_name_";

    public static class FavItem {
        public String name;
        public String lat;
        public String lon;

        public FavItem(String name, String lat, String lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveLocation(Context context, String lat, String lon) {
        getPrefs(context).edit()
                .putString(KEY_LAT, lat)
                .putString(KEY_LON, lon)
                .apply();
    }

    public static String getLat(Context context) {
        return getPrefs(context).getString(KEY_LAT, "0.000000");
    }

    public static String getLon(Context context) {
        return getPrefs(context).getString(KEY_LON, "0.000000");
    }

    public static void setRunning(Context context, boolean running) {
        getPrefs(context).edit().putBoolean(KEY_RUNNING, running).apply();
    }

    public static boolean isRunning(Context context) {
        return getPrefs(context).getBoolean(KEY_RUNNING, false);
    }

    public static void saveStatus(Context context, String status) {
        getPrefs(context).edit().putString(KEY_STATUS, status).apply();
    }

    public static String getStatus(Context context) {
        return getPrefs(context).getString(KEY_STATUS, "Stopped");
    }

    // Favorites Management (Max 5)
    public static List<FavItem> getFavorites(Context context) {
        List<FavItem> list = new ArrayList<>();
        SharedPreferences sp = getPrefs(context);
        int count = sp.getInt(KEY_FAV_COUNT, 0);
        for (int i = 0; i < count; i++) {
            String lat = sp.getString(KEY_FAV_PREFIX_LAT + i, "");
            String lon = sp.getString(KEY_FAV_PREFIX_LON + i, "");
            String name = sp.getString(KEY_FAV_PREFIX_NAME + i, "Fav " + (i + 1));
            if (!lat.isEmpty() && !lon.isEmpty()) {
                list.add(new FavItem(name, lat, lon));
            }
        }
        return list;
    }

    public static boolean addFavorite(Context context, String name, String lat, String lon) {
        List<FavItem> current = getFavorites(context);
        if (current.size() >= 5) {
            return false;
        }
        current.add(new FavItem(name, lat, lon));
        saveFavoritesList(context, current);
        return true;
    }

    public static void removeFavorite(Context context, int index) {
        List<FavItem> current = getFavorites(context);
        if (index >= 0 && index < current.size()) {
            current.remove(index);
            saveFavoritesList(context, current);
        }
    }

    public static void renameFavorite(Context context, int index, String newName) {
        List<FavItem> current = getFavorites(context);
        if (index >= 0 && index < current.size()) {
            current.get(index).name = newName;
            saveFavoritesList(context, current);
        }
    }

    private static void saveFavoritesList(Context context, List<FavItem> list) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putInt(KEY_FAV_COUNT, list.size());
        for (int i = 0; i < list.size(); i++) {
            FavItem item = list.get(i);
            editor.putString(KEY_FAV_PREFIX_LAT + i, item.lat);
            editor.putString(KEY_FAV_PREFIX_LON + i, item.lon);
            editor.putString(KEY_FAV_PREFIX_NAME + i, item.name);
        }
        editor.apply();
    }
}
