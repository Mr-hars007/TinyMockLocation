package com.mrhars007.mocklocation;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocationExtractor {
    private static final String TAG = "LocationExtractor";

    public interface Callback {
        void onExtracted(double lat, double lon, String rawSource);
        void onError(String message);
    }

    // Priority 1: Map parameter — exact pin location
    private static final Pattern P_DATA_3D4D = Pattern.compile("!3d(-?\\d+\\.?\\d*)!4d(-?\\d+\\.?\\d*)");

    // Priority 2: q= / query= / ll= parameter
    private static final Pattern P_QUERY = Pattern.compile("(?:[?&](?:q|query|ll))=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");

    // Priority 3: /place/lat,lon in path
    private static final Pattern P_PLACE = Pattern.compile("/place/(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");

    // Priority 4: @lat,lon — viewport center
    private static final Pattern P_AT = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");

    // Priority 5: raw lat,lon pair
    private static final Pattern P_RAW = Pattern.compile("(-?\\d+\\.\\d+)[,\\s]+(-?\\d+\\.\\d+)");

    // URL detector
    private static final Pattern P_URL = Pattern.compile("https?://\\S+");

    public static void extractAsync(final String text, final Callback callback) {
        if (TextUtils.isEmpty(text)) {
            if (callback != null) callback.onError("Empty input");
            return;
        }

        double[] coords = extractFromText(text);
        if (coords != null) {
            if (callback != null) callback.onExtracted(coords[0], coords[1], text);
            return;
        }

        Matcher urlMatcher = P_URL.matcher(text);
        if (urlMatcher.find()) {
            final String urlStr = urlMatcher.group(0);
            new Thread(() -> {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.connect();

                    String finalUrl = conn.getURL().toString();

                    StringBuilder body = new StringBuilder();
                    try {
                        InputStream is = conn.getInputStream();
                        if (is != null) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                            char[] buf = new char[4096];
                            int read;
                            int total = 0;
                            while ((read = reader.read(buf)) != -1 && total < 32768) {
                                body.append(buf, 0, read);
                                total += read;
                            }
                            reader.close();
                        }
                    } catch (Exception ignored) {}
                    conn.disconnect();

                    String combined = finalUrl + "\n" + body.toString();

                    double[] resolved = extractFromText(combined);
                    if (resolved != null) {
                        if (callback != null) callback.onExtracted(resolved[0], resolved[1], text);
                        return;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error resolving URL", e);
                }
                if (callback != null) callback.onError("Could not extract coordinates from link");
            }).start();
        } else {
            if (callback != null) callback.onError("No coordinates found");
        }
    }

    public static double[] extractDirect(String text) {
        return extractFromText(text);
    }

    private static double[] extractFromText(String text) {
        if (TextUtils.isEmpty(text)) return null;

        String trimmed = text.trim();
        if (PlusCodeUtils.isPlusCode(trimmed)) {
            try {
                return PlusCodeUtils.decode(trimmed, 0.0, 0.0);
            } catch (Exception ignored) {}
        }

        // Priority 1: !3d<lat>!4d<lon> — EXACT pin location
        Matcher m1 = P_DATA_3D4D.matcher(text);
        if (m1.find()) {
            double lat = Double.parseDouble(m1.group(1));
            double lon = Double.parseDouble(m1.group(2));
            if (isValid(lat, lon)) return new double[]{lat, lon};
        }

        // Priority 2: q=lat,lon
        Matcher m2 = P_QUERY.matcher(text);
        if (m2.find()) {
            double lat = Double.parseDouble(m2.group(1));
            double lon = Double.parseDouble(m2.group(2));
            if (isValid(lat, lon)) return new double[]{lat, lon};
        }

        // Priority 3: /place/lat,lon
        Matcher m3 = P_PLACE.matcher(text);
        if (m3.find()) {
            double lat = Double.parseDouble(m3.group(1));
            double lon = Double.parseDouble(m3.group(2));
            if (isValid(lat, lon)) return new double[]{lat, lon};
        }

        // Priority 4: @lat,lon — viewport center
        Matcher m4 = P_AT.matcher(text);
        if (m4.find()) {
            double lat = Double.parseDouble(m4.group(1));
            double lon = Double.parseDouble(m4.group(2));
            if (isValid(lat, lon)) return new double[]{lat, lon};
        }

        // Priority 5: raw lat,lon
        Matcher m5 = P_RAW.matcher(text);
        if (m5.find()) {
            double lat = Double.parseDouble(m5.group(1));
            double lon = Double.parseDouble(m5.group(2));
            if (isValid(lat, lon)) return new double[]{lat, lon};
        }

        return null;
    }

    private static boolean isValid(double lat, double lon) {
        return (lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0);
    }
}
