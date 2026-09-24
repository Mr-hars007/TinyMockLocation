package com.mrhars007.mocklocation;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 101;

    // Calm palette constants
    private static final int CLR_PRIMARY           = 0xFF5B8C85;
    private static final int CLR_TEXT              = 0xFF2D3748;
    private static final int CLR_TEXT_SEC          = 0xFF8896AB;
    private static final int CLR_FAV_NAME          = 0xFF3D5A57;
    private static final int CLR_FAV_COORD         = 0xFF8896AB;
    private static final int CLR_FAV_ROW           = 0xFFF2F5F7;
    private static final int CLR_RENAME            = 0xFF8896AB;
    private static final int CLR_DELETE            = 0xFFC9A0A0;
    private static final int CLR_RUNNING           = 0xFF5B8C85;
    private static final int CLR_STOPPED           = 0xFFB0BCC9;
    private static final int CLR_INPUT_BG          = 0xFFEFF2F7;
    private static final int CLR_HINT              = 0xFFB0BCC9;

    // High-contrast Button states
    private static final int CLR_BTN_START_ON      = 0xFF5B8C85;
    private static final int CLR_BTN_START_TXT_ON  = 0xFFFFFFFF;
    private static final int CLR_BTN_START_OFF     = 0xFFDDE5E3;
    private static final int CLR_BTN_START_TXT_OFF = 0xFF95A39F;

    private static final int CLR_BTN_STOP_ON       = 0xFFC75D5D;
    private static final int CLR_BTN_STOP_TXT_ON   = 0xFFFFFFFF;
    private static final int CLR_BTN_STOP_OFF      = 0xFFE2E7EC;
    private static final int CLR_BTN_STOP_TXT_OFF  = 0xFF9AA6B2;

    private EditText etPlusCode;
    private EditText etLatitude;
    private EditText etLongitude;
    private Button btnStart;
    private Button btnStop;
    private Button btnAddFav;
    private TextView tvFavHeader;
    private LinearLayout containerFavs;
    private Button btnBatteryOpt;
    private TextView tvStatus;
    private View statusDot;
    private TextView tvAdbReady;

    private boolean isUpdatingFromCode = false;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && MockLocationService.ACTION_STATUS_BROADCAST.equals(intent.getAction())) {
                String status = intent.getStringExtra(MockLocationService.EXTRA_STATUS);
                boolean running = intent.getBooleanExtra(MockLocationService.EXTRA_RUNNING, false);
                updateUiStatus(status, running);
            }
        }
    };

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUsbStatus(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etPlusCode    = findViewById(R.id.etPlusCode);
        etLatitude    = findViewById(R.id.etLatitude);
        etLongitude   = findViewById(R.id.etLongitude);
        btnStart      = findViewById(R.id.btnStart);
        btnStop       = findViewById(R.id.btnStop);
        btnAddFav     = findViewById(R.id.btnAddFav);
        tvFavHeader   = findViewById(R.id.tvFavHeader);
        containerFavs = findViewById(R.id.containerFavs);
        btnBatteryOpt = findViewById(R.id.btnBatteryOpt);
        tvStatus      = findViewById(R.id.tvStatus);
        statusDot     = findViewById(R.id.statusDot);
        tvAdbReady    = findViewById(R.id.tvAdbReady);

        Button btnDecode = findViewById(R.id.btnDecodePlusCode);

        // Populate from saved preferences
        String savedLat = Prefs.getLat(this);
        String savedLon = Prefs.getLon(this);
        etLatitude.setText(savedLat);
        etLongitude.setText(savedLon);
        updatePlusCodeDisplay(savedLat, savedLon);

        // Round the input fields and decode button
        roundView(etPlusCode, CLR_INPUT_BG, 10);
        roundView(etLatitude, CLR_INPUT_BG, 10);
        roundView(etLongitude, CLR_INPUT_BG, 10);
        roundView(btnDecode, CLR_PRIMARY, 10);

        btnDecode.setOnClickListener(v -> decodePlusCodeFromUi());
        btnStart.setOnClickListener(v -> startMockLocationFromUi());
        btnStop.setOnClickListener(v -> stopMockLocation());
        btnAddFav.setOnClickListener(v -> promptSaveFavorite());
        btnBatteryOpt.setOnClickListener(v -> requestBatteryOptimizationExemption());

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isUpdatingFromCode)
                    updatePlusCodeDisplay(etLatitude.getText().toString(), etLongitude.getText().toString());
            }
        };
        etLatitude.addTextChangedListener(watcher);
        etLongitude.addTextChangedListener(watcher);

        checkPermissions();
        checkBatteryOptimizationStatus();
        renderFavoritesList();
        updateUiStatus(Prefs.getStatus(this), Prefs.isRunning(this));
        updateUsbStatus(null);
        handleIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); setIntent(i); handleIntent(i); }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(statusReceiver, new IntentFilter(MockLocationService.ACTION_STATUS_BROADCAST));

        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction("android.hardware.usb.action.USB_STATE");
        usbFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        usbFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(usbReceiver, usbFilter);

        updateUiStatus(Prefs.getStatus(this), Prefs.isRunning(this));
        updateUsbStatus(null);
        checkBatteryOptimizationStatus();
        renderFavoritesList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(statusReceiver); } catch (Exception ignored) {}
        try { unregisterReceiver(usbReceiver); } catch (Exception ignored) {}
    }

    // ─── USB / ADB Status ──────────────────────────────────

    private void updateUsbStatus(Intent usbIntent) {
        if (tvAdbReady == null) return;
        boolean isAdbEnabled = false;
        try {
            isAdbEnabled = Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1;
        } catch (Exception ignored) {}

        boolean isConnected = false;
        if (usbIntent != null && "android.hardware.usb.action.USB_STATE".equals(usbIntent.getAction())) {
            isConnected = usbIntent.getBooleanExtra("connected", false);
        } else {
            Intent sticky = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (sticky != null) {
                int plugged = sticky.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                isConnected = (plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_AC);
            }
        }

        if (isConnected) {
            if (isAdbEnabled) {
                tvAdbReady.setText("ADB: Connected (Ready)");
                tvAdbReady.setTextColor(0xFF4A7C74);
            } else {
                tvAdbReady.setText("USB: Connected (ADB Disabled)");
                tvAdbReady.setTextColor(0xFF8896AB);
            }
        } else {
            tvAdbReady.setText("USB: Disconnected");
            tvAdbReady.setTextColor(0xFFB0BCC9);
        }
    }

    // ─── Intent Handling ───────────────────────────────────

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action) && intent.hasExtra(Intent.EXTRA_TEXT)) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            updateUiStatus("Parsing…", false);
            LocationExtractor.extractAsync(shared, new LocationExtractor.Callback() {
                @Override public void onExtracted(double lat, double lon, String raw) {
                    runOnUiThread(() -> {
                        String la = fmt(lat), lo = fmt(lon);
                        setCoordinateFields(la, lo);
                        Prefs.saveLocation(MainActivity.this, la, lo);
                        if (Prefs.isRunning(MainActivity.this))
                            sendServiceCommand(MockLocationService.ACTION_START, la, lo);
                        else
                            updateUiStatus("Loaded " + la + ", " + lo, false);
                    });
                }
                @Override public void onError(String msg) { runOnUiThread(() -> updateUiStatus(msg, false)); }
            });
            return;
        }

        String cmd = intent.getStringExtra("action");
        String code = intent.getStringExtra("code");
        if (code == null) code = intent.getStringExtra("pluscode");
        if (code == null) code = intent.getStringExtra("plus_code");
        String latStr = intent.getStringExtra("lat");
        String lonStr = intent.getStringExtra("lon");

        if (latStr != null && (lonStr == null || lonStr.isEmpty())) {
            double[] ex = LocationExtractor.extractDirect(latStr);
            if (ex != null) { latStr = fmt(ex[0]); lonStr = fmt(ex[1]); }
        }
        if (latStr != null && PlusCodeUtils.isPlusCode(latStr)) { code = latStr; latStr = null; lonStr = null; }

        if (code != null && PlusCodeUtils.isPlusCode(code)) {
            try {
                double rla = 0, rlo = 0;
                try { rla = Double.parseDouble(Prefs.getLat(this)); rlo = Double.parseDouble(Prefs.getLon(this)); } catch (Exception ig) {}
                double[] c = PlusCodeUtils.decode(code, rla, rlo);
                latStr = fmt(c[0]); lonStr = fmt(c[1]);
                isUpdatingFromCode = true;
                etPlusCode.setText(code); etLatitude.setText(latStr); etLongitude.setText(lonStr);
                isUpdatingFromCode = false;
            } catch (Exception e) { updateUiStatus("Invalid Plus Code", false); return; }
        } else {
            if (latStr != null) etLatitude.setText(latStr);
            if (lonStr != null) etLongitude.setText(lonStr);
            updatePlusCodeDisplay(etLatitude.getText().toString(), etLongitude.getText().toString());
        }

        if ("start".equalsIgnoreCase(cmd)) startMockLocationFromUi();
        else if ("stop".equalsIgnoreCase(cmd)) stopMockLocation();
        else if ("set".equalsIgnoreCase(cmd)) {
            String la = etLatitude.getText().toString().trim(), lo = etLongitude.getText().toString().trim();
            if (valid(la, lo)) {
                Prefs.saveLocation(this, la, lo);
                if (Prefs.isRunning(this)) sendServiceCommand(MockLocationService.ACTION_START, la, lo);
                else updateUiStatus("Location updated", false);
            }
        }
    }

    // ─── Plus Code ─────────────────────────────────────────

    private void decodePlusCodeFromUi() {
        String code = etPlusCode.getText().toString().trim();
        if (!PlusCodeUtils.isPlusCode(code)) { updateUiStatus("Invalid Plus Code", false); return; }
        try {
            double rla = 0, rlo = 0;
            try { rla = Double.parseDouble(etLatitude.getText().toString().trim()); rlo = Double.parseDouble(etLongitude.getText().toString().trim()); } catch (Exception ig) {}
            double[] c = PlusCodeUtils.decode(code, rla, rlo);
            String la = fmt(c[0]), lo = fmt(c[1]);
            setCoordinateFields(la, lo);
            Prefs.saveLocation(this, la, lo);
            if (Prefs.isRunning(this)) sendServiceCommand(MockLocationService.ACTION_START, la, lo);
            else updateUiStatus("Decoded " + la + ", " + lo, false);
        } catch (Exception e) { updateUiStatus("Decode failed", false); }
    }

    private void updatePlusCodeDisplay(String la, String lo) {
        if (valid(la, lo)) {
            try {
                isUpdatingFromCode = true;
                etPlusCode.setText(PlusCodeUtils.encode(Double.parseDouble(la), Double.parseDouble(lo)));
                isUpdatingFromCode = false;
            } catch (Exception ignored) {}
        } else {
            if (!isUpdatingFromCode) {
                isUpdatingFromCode = true;
                etPlusCode.setText("");
                isUpdatingFromCode = false;
            }
        }
    }

    private void setCoordinateFields(String la, String lo) {
        isUpdatingFromCode = true;
        etLatitude.setText(la); etLongitude.setText(lo);
        isUpdatingFromCode = false;
        updatePlusCodeDisplay(la, lo);
    }

    // ─── Favorites ─────────────────────────────────────────

    private void promptSaveFavorite() {
        String la = etLatitude.getText().toString().trim(), lo = etLongitude.getText().toString().trim();
        if (!valid(la, lo)) { updateUiStatus("Invalid coordinates", false); return; }
        if (Prefs.getFavorites(this).size() >= 5) { updateUiStatus("Max 5 favorites", Prefs.isRunning(this)); return; }

        showNameDialog("Save to Favorites", "", name -> {
            if (name.isEmpty()) name = la + ", " + lo;
            Prefs.addFavorite(this, name, la, lo);
            renderFavoritesList();
            updateUiStatus("Saved ✓", Prefs.isRunning(this));
        });
    }

    private void showNameDialog(String title, String current, OnName cb) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        wrap.setPadding(dp(20), dp(16), dp(20), dp(8));

        EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46));
        input.setLayoutParams(lp);
        input.setText(current);
        input.setSelectAllOnFocus(true);
        input.setSingleLine(true);
        input.setTextColor(CLR_TEXT);
        input.setHintTextColor(CLR_HINT);
        input.setHint("e.g. Home, Office, Gym…");
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setGravity(Gravity.CENTER_VERTICAL);
        roundView(input, CLR_INPUT_BG, 8);

        wrap.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(wrap)
                .setPositiveButton("Save", (d, w) -> cb.onName(input.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (pos != null) {
                pos.setTextColor(CLR_PRIMARY);
                pos.setTypeface(null, Typeface.BOLD);
            }
            if (neg != null) {
                neg.setTextColor(CLR_TEXT_SEC);
            }
        });

        dialog.show();
    }

    private interface OnName { void onName(String n); }

    private void renderFavoritesList() {
        containerFavs.removeAllViews();
        List<Prefs.FavItem> favs = Prefs.getFavorites(this);
        tvFavHeader.setText("Favorites" + (favs.isEmpty() ? "" : "  (" + favs.size() + "/5)"));

        if (favs.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No favorites yet — tap + Save to add one");
            empty.setTextSize(13);
            empty.setTextColor(CLR_HINT);
            empty.setPadding(0, dp(6), 0, dp(6));
            containerFavs.addView(empty);
            return;
        }

        for (int i = 0; i < favs.size(); i++) {
            containerFavs.addView(buildFavRow(i, favs.get(i)));
        }
    }

    private View buildFavRow(int idx, Prefs.FavItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setColor(CLR_FAV_ROW);
        rowBg.setCornerRadius(dp(8));
        row.setBackground(rowBg);

        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, dp(3), 0, dp(3));
        row.setLayoutParams(rp);

        // Info block (name + coords)
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        info.setPadding(dp(14), dp(10), dp(4), dp(10));

        TextView name = new TextView(this);
        name.setText(item.name);
        name.setTextSize(14);
        name.setTextColor(CLR_FAV_NAME);
        name.setTypeface(null, Typeface.BOLD);
        info.addView(name);

        TextView coords = new TextView(this);
        coords.setText(item.lat + ",  " + item.lon);
        coords.setTextSize(11);
        coords.setTextColor(CLR_FAV_COORD);
        coords.setPadding(0, dp(2), 0, 0);
        info.addView(coords);

        // Tap to select
        info.setOnClickListener(v -> {
            setCoordinateFields(item.lat, item.lon);
            Prefs.saveLocation(this, item.lat, item.lon);
            if (Prefs.isRunning(this)) sendServiceCommand(MockLocationService.ACTION_START, item.lat, item.lon);
            updateUiStatus(item.name, Prefs.isRunning(this));
        });

        // Long press to rename
        info.setOnLongClickListener(v -> {
            showNameDialog("Rename", item.name, n -> {
                if (n.isEmpty()) n = item.lat + ", " + item.lon;
                Prefs.renameFavorite(this, idx, n);
                renderFavoritesList();
            });
            return true;
        });

        row.addView(info);

        // Rename button
        TextView btnR = new TextView(this);
        btnR.setText("✎");
        btnR.setTextSize(16);
        btnR.setTextColor(CLR_RENAME);
        btnR.setGravity(Gravity.CENTER);
        btnR.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        btnR.setOnClickListener(v -> showNameDialog("Rename", item.name, n -> {
            if (n.isEmpty()) n = item.lat + ", " + item.lon;
            Prefs.renameFavorite(this, idx, n);
            renderFavoritesList();
        }));
        row.addView(btnR);

        // Delete button
        TextView btnD = new TextView(this);
        btnD.setText("✕");
        btnD.setTextSize(16);
        btnD.setTextColor(CLR_DELETE);
        btnD.setGravity(Gravity.CENTER);
        btnD.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        btnD.setPadding(0, 0, dp(4), 0);
        btnD.setOnClickListener(v -> {
            Prefs.removeFavorite(this, idx);
            renderFavoritesList();
        });
        row.addView(btnD);

        return row;
    }

    // ─── Mock Location ─────────────────────────────────────

    private void startMockLocationFromUi() {
        String la = etLatitude.getText().toString().trim(), lo = etLongitude.getText().toString().trim();
        if (!valid(la, lo)) { updateUiStatus("Invalid coordinates", false); return; }
        Prefs.saveLocation(this, la, lo);
        sendServiceCommand(MockLocationService.ACTION_START, la, lo);
    }

    private void stopMockLocation() {
        Intent i = new Intent(this, MockLocationService.class);
        i.setAction(MockLocationService.ACTION_STOP);
        startService(i);
        updateUiStatus("Stopped", false);
    }

    private void sendServiceCommand(String action, String la, String lo) {
        Intent i = new Intent(this, MockLocationService.class);
        i.setAction(action);
        i.putExtra(MockLocationService.EXTRA_LAT, la);
        i.putExtra(MockLocationService.EXTRA_LON, lo);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
        else startService(i);
    }

    // ─── Status ────────────────────────────────────────────

    private void updateUiStatus(String status, boolean running) {
        tvStatus.setText(status);
        if (running) {
            tvStatus.setTextColor(CLR_RUNNING);
            roundView(statusDot, CLR_RUNNING, 100);

            // Start button disabled
            btnStart.setEnabled(false);
            roundView(btnStart, CLR_BTN_START_OFF, 10);
            btnStart.setTextColor(CLR_BTN_START_TXT_OFF);
            btnStart.setAlpha(1.0f);

            // Stop button ENABLED (high contrast warm coral red with white text)
            btnStop.setEnabled(true);
            roundView(btnStop, CLR_BTN_STOP_ON, 10);
            btnStop.setTextColor(CLR_BTN_STOP_TXT_ON);
            btnStop.setAlpha(1.0f);
        } else {
            tvStatus.setTextColor(CLR_TEXT_SEC);
            roundView(statusDot, CLR_STOPPED, 100);

            // Start button ENABLED (calm teal with white text)
            btnStart.setEnabled(true);
            roundView(btnStart, CLR_BTN_START_ON, 10);
            btnStart.setTextColor(CLR_BTN_START_TXT_ON);
            btnStart.setAlpha(1.0f);

            // Stop button DISABLED (muted neutral gray)
            btnStop.setEnabled(false);
            roundView(btnStop, CLR_BTN_STOP_OFF, 10);
            btnStop.setTextColor(CLR_BTN_STOP_TXT_OFF);
            btnStop.setAlpha(1.0f);
        }
    }

    // ─── Permissions ───────────────────────────────────────

    private void checkPermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
    }

    private void checkBatteryOptimizationStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null)
                btnBatteryOpt.setVisibility(pm.isIgnoringBatteryOptimizations(getPackageName()) ? View.GONE : View.VISIBLE);
        } else btnBatteryOpt.setVisibility(View.GONE);
    }

    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } catch (Exception e) {
                try { startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); } catch (Exception ig) {}
            }
        }
    }

    // ─── Helpers ───────────────────────────────────────────

    private boolean valid(String la, String lo) {
        if (TextUtils.isEmpty(la) || TextUtils.isEmpty(lo)) return false;
        try { double a = Double.parseDouble(la), o = Double.parseDouble(lo); return a >= -90 && a <= 90 && o >= -180 && o <= 180; }
        catch (NumberFormatException e) { return false; }
    }

    private String fmt(double v) { return String.format(Locale.US, "%.6f", v); }

    private int dp(int d) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, d, getResources().getDisplayMetrics()); }

    private void roundView(View v, int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        v.setBackground(d);
    }
}
