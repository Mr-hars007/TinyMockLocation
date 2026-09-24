package com.mrhars007.mocklocation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MockLocationService extends Service {
    private static final String TAG = "MockLocationService";
    private static final String CHANNEL_ID = "mock_location_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START = "com.mrhars007.mocklocation.START";
    public static final String ACTION_STOP = "com.mrhars007.mocklocation.STOP";
    public static final String ACTION_UPDATE = "com.mrhars007.mocklocation.UPDATE";

    public static final String ACTION_STATUS_BROADCAST = "com.mrhars007.mocklocation.STATUS_BROADCAST";
    public static final String EXTRA_STATUS = "extra_status";
    public static final String EXTRA_RUNNING = "extra_running";

    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LON = "lon";

    private LocationManager locationManager;
    private ScheduledExecutorService executor;
    private volatile double targetLat = 0.0;
    private volatile double targetLon = 0.0;
    private volatile boolean isMockingActive = false;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP.equals(action)) {
                stopMocking();
                stopSelf();
                return START_NOT_STICKY;
            }

            if (intent.hasExtra(EXTRA_LAT) && intent.hasExtra(EXTRA_LON)) {
                try {
                    String latStr = intent.getStringExtra(EXTRA_LAT);
                    String lonStr = intent.getStringExtra(EXTRA_LON);
                    if (latStr == null) latStr = String.valueOf(intent.getDoubleExtra(EXTRA_LAT, 0.0));
                    if (lonStr == null) lonStr = String.valueOf(intent.getDoubleExtra(EXTRA_LON, 0.0));

                    double parsedLat = Double.parseDouble(latStr);
                    double parsedLon = Double.parseDouble(lonStr);

                    if (isValidCoordinates(parsedLat, parsedLon)) {
                        targetLat = parsedLat;
                        targetLon = parsedLon;
                        Prefs.saveLocation(this, String.valueOf(targetLat), String.valueOf(targetLon));
                    } else {
                        broadcastStatus("Invalid coordinates", false);
                        return START_NOT_STICKY;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing lat/lon", e);
                    broadcastStatus("Invalid coordinates", false);
                    return START_NOT_STICKY;
                }
            }

            startMockingForeground();
        }

        return START_STICKY;
    }

    private void startMockingForeground() {
        Notification notification = buildNotification("Active: " + targetLat + ", " + targetLon);
        startForeground(NOTIFICATION_ID, notification);

        if (!isMockingActive) {
            if (setupMockProviders()) {
                isMockingActive = true;
                Prefs.setRunning(this, true);
                broadcastStatus("Running", true);
                startLocationUpdateLoop();
            }
        } else {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(NOTIFICATION_ID, notification);
            }
            broadcastStatus("Running", true);
        }
    }

    private boolean setupMockProviders() {
        if (locationManager == null) {
            broadcastStatus("Location services disabled", false);
            return false;
        }

        try {
            try {
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            } catch (Exception ignored) {}

            locationManager.addTestProvider(
                    LocationManager.GPS_PROVIDER,
                    false, false, false, false,
                    true, true, true,
                    Criteria.POWER_LOW, Criteria.ACCURACY_FINE
            );
            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

            try {
                locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
            } catch (Exception ignored) {}

            locationManager.addTestProvider(
                    LocationManager.NETWORK_PROVIDER,
                    true, false, true, false,
                    false, false, false,
                    Criteria.POWER_LOW, Criteria.ACCURACY_COARSE
            );
            locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true);

            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException registering test provider", e);
            broadcastStatus("Mock provider unavailable (Check Developer Options)", false);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Exception registering test provider", e);
            broadcastStatus("Error: " + e.getMessage(), false);
            return false;
        }
    }

    private void startLocationUpdateLoop() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }

        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!isMockingActive) return;
                try {
                    injectLocation(LocationManager.GPS_PROVIDER, targetLat, targetLon, 3.0f);
                    injectLocation(LocationManager.NETWORK_PROVIDER, targetLat, targetLon, 15.0f);
                } catch (Exception e) {
                    Log.e(TAG, "Error injecting mock location", e);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void injectLocation(String provider, double lat, double lon, float accuracy) {
        if (locationManager == null) return;
        try {
            Location mockLoc = new Location(provider);
            mockLoc.setLatitude(lat);
            mockLoc.setLongitude(lon);
            mockLoc.setAltitude(0.0);
            mockLoc.setAccuracy(accuracy);
            mockLoc.setTime(System.currentTimeMillis());
            mockLoc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            locationManager.setTestProviderLocation(provider, mockLoc);
        } catch (Exception e) {
            Log.e(TAG, "Failed setTestProviderLocation for " + provider, e);
        }
    }

    private void stopMocking() {
        isMockingActive = false;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        if (locationManager != null) {
            try {
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            } catch (Exception ignored) {}
            try {
                locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, false);
                locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
            } catch (Exception ignored) {}
        }

        Prefs.setRunning(this, false);
        broadcastStatus("Stopped", false);
        stopForeground(true);
    }

    private boolean isValidCoordinates(double lat, double lon) {
        return (lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0);
    }

    private void broadcastStatus(String status, boolean isRunning) {
        Prefs.saveStatus(this, status);
        Intent intent = new Intent(ACTION_STATUS_BROADCAST);
        intent.putExtra(EXTRA_STATUS, status);
        intent.putExtra(EXTRA_RUNNING, isRunning);
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Mock Location",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Tiny Mock Location status");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String text) {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        return builder
                .setContentTitle("Tiny Mock Location")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notif_crosshair)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        stopMocking();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
