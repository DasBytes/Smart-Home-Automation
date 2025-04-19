package com.example.iot_home_automation;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String FAN_PATH = "devices/fan";
    private static final String LIGHT_PATH = "devices/light";
    public static final String OPEN_WEATHER_API_KEY = "f52a48ea3dd80f07bcd97b88d1a74aee";

    private Switch fanSwitch, lightSwitch;
    private TextView fanStatus, lightStatus, locationStatus, temperatureStatus, wifiStatus;

    private DatabaseReference dbRef;
    private FusedLocationProviderClient fusedLocationClient;
    private ExecutorService networkExecutor;
    private ConnectivityManager connectivityManager;
    private BroadcastReceiver connectivityReceiver;
    private boolean isInitialDataLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupFirebase();
        setupServices();
        setupWifiMonitoring();
        setupSwitchListeners();
        checkLocationPermission();
    }

    private void initializeViews() {
        fanSwitch = findViewById(R.id.fanSwitch);
        lightSwitch = findViewById(R.id.lightSwitch);
        fanStatus = findViewById(R.id.fanStatus);
        lightStatus = findViewById(R.id.lightStatus);
        locationStatus = findViewById(R.id.locationStatus);
        temperatureStatus = findViewById(R.id.temperatureStatus);
        wifiStatus = findViewById(R.id.wifiStatus);
    }

    private void setupFirebase() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            database.setPersistenceEnabled(true);
            dbRef = database.getReference();

            setupDeviceListener(FAN_PATH, fanSwitch, fanStatus);
            setupDeviceListener(LIGHT_PATH, lightSwitch, lightStatus);
        } catch (Exception e) {
            showToast("Firebase initialization failed.");
            Log.e(TAG, "Firebase initialization error", e);
        }
    }

    private void setupDeviceListener(String path, Switch deviceSwitch, TextView statusView) {
        dbRef.child(path).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                try {
                    Boolean state = snapshot.getValue(Boolean.class);
                    if (state != null) {
                        if (!isInitialDataLoaded || deviceSwitch.isChecked() != state) {
                            deviceSwitch.setChecked(state);
                            updateDeviceStatus(statusView, state);
                        }
                        isInitialDataLoaded = true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing data for " + path, e);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showToast("Database error.");
                Log.e(TAG, "Database error: " + error.getMessage(), error.toException());
            }
        });
    }

    private void setupServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        networkExecutor = Executors.newSingleThreadExecutor();
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private void setupWifiMonitoring() {
        connectivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateNetworkStatus();
            }
        };
        registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        updateNetworkStatus();
    }

    private void updateNetworkStatus() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

        wifiStatus.setText(isConnected ? R.string.wifi_connected : R.string.wifi_disconnected);
        wifiStatus.setTextColor(ContextCompat.getColor(this,
                isConnected ? R.color.green : R.color.red));
    }

    private void setupSwitchListeners() {
        fanSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isInitialDataLoaded) return;
            updateDeviceStatus(fanStatus, isChecked);
            updateDeviceState(FAN_PATH, isChecked);
        });

        lightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isInitialDataLoaded) return;
            updateDeviceStatus(lightStatus, isChecked);
            updateDeviceState(LIGHT_PATH, isChecked);
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void getLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            fetchWeatherData(location.getLatitude(), location.getLongitude());
                        } else {
                            locationStatus.setText(R.string.location_unavailable);
                            temperatureStatus.setText(R.string.na);
                        }
                    })
                    .addOnFailureListener(e -> {
                        locationStatus.setText(R.string.location_error);
                        temperatureStatus.setText(R.string.na);
                        Log.e(TAG, "Location error", e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Location error", e);
            locationStatus.setText(R.string.location_error);
            temperatureStatus.setText(R.string.na);
        }
    }

    private void fetchWeatherData(double latitude, double longitude) {
        networkExecutor.execute(() -> {
            try {
                String url = String.format(Locale.US,
                        "https://api.openweathermap.org/data/2.5/weather?lat=%.4f&lon=%.4f&units=metric&appid=%s",
                        latitude, longitude, OPEN_WEATHER_API_KEY);

                Log.d(TAG, "Weather API URL: " + url);

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();

                    Log.d(TAG, "API Response: " + result.toString()); // ✅ log the JSON response

                    JSONObject jsonObject = new JSONObject(result.toString());
                    JSONObject main = jsonObject.getJSONObject("main");
                    double temperature = main.getDouble("temp");
                    String cityName = jsonObject.getString("name");

                    runOnUiThread(() -> {
                        temperatureStatus.setText(getString(R.string.temperature_format, temperature));
                        locationStatus.setText(getString(R.string.location_format, cityName));
                    });
                } else {
                    BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResult = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResult.append(line);
                    }
                    errorReader.close();

                    Log.e(TAG, "Weather API Error Response: " + errorResult.toString());

                    runOnUiThread(() -> {
                        temperatureStatus.setText(R.string.na);
                        locationStatus.setText(R.string.location_error);
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Weather API Exception", e);
                runOnUiThread(() -> {
                    temperatureStatus.setText(R.string.na);
                    locationStatus.setText(R.string.location_error);
                });
            }
        });
    }


    private void updateDeviceState(String path, boolean state) {
        dbRef.child(path).setValue(state)
                .addOnSuccessListener(aVoid -> Log.d(TAG, path + " state updated"))
                .addOnFailureListener(e -> {
                    showToast("Device update failed.");
                    Log.e(TAG, "Error updating " + path, e);
                });
    }

    private void updateDeviceStatus(TextView statusView, boolean isOn) {
        statusView.setText(isOn ? R.string.on : R.string.off);
        statusView.setTextColor(ContextCompat.getColor(this,
                isOn ? R.color.green : R.color.red));

        if (statusView == fanStatus) {
            fanSwitch.getThumbDrawable().setColorFilter(
                    isOn ? Color.GREEN : Color.GRAY, android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (statusView == lightStatus) {
            lightSwitch.getThumbDrawable().setColorFilter(
                    isOn ? Color.GREEN : Color.GRAY, android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }
}



