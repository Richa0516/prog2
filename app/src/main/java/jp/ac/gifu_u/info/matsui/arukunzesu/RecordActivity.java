package jp.ac.gifu_u.info.matsui.arukunzesu;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import androidx.room.Room;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordActivity extends AppCompatActivity implements OnMapReadyCallback {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location previousLocation;
    private float totalDistance = 0f;
    private long startTime;

    private TextView textTime, textDistance, textSpeed, textCalories;
    private TextView textCountdown;
    private Button btnFinish;

    private GoogleMap mMap;
    private Marker currentMarker;

    private List<LatLng> pathPoints = new ArrayList<>();
    private Polyline polyline;

    private float currentSpeed = 0f;
    private float currentCalories = 0f;

    private Date startDate;
    private boolean mapReady = false;
    private boolean locationReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_record);

        startDate = new Date();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textTime = findViewById(R.id.textTime);
        textDistance = findViewById(R.id.textDistance);
        textSpeed = findViewById(R.id.textSpeed);
        textCalories = findViewById(R.id.textCalories);
        btnFinish = findViewById(R.id.btnFinish);
        textCountdown = findViewById(R.id.textCountdown);

        btnFinish.setOnClickListener(v -> {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            long elapsed = SystemClock.elapsedRealtime() - startTime;

            WalkRecord record = new WalkRecord();
            record.date = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(startDate);
            record.startTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(startDate);
            record.time = (int)(elapsed / 1000);
            record.distanceKm = totalDistance / 1000f;
            record.speed = currentSpeed;
            record.calories = currentCalories;

            JSONArray jsonArray = new JSONArray();
            for (LatLng point : pathPoints) {
                JSONArray latLng = new JSONArray();
                try {
                    latLng.put(point.latitude);
                    latLng.put(point.longitude);
                    jsonArray.put(latLng);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            record.pathJson = jsonArray.toString();

            AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                            AppDatabase.class, "walk-db")
                    .fallbackToDestructiveMigration()
                    .build();

            new Thread(() -> db.walkRecordDao().insert(record)).start();

            Intent intent = new Intent(RecordActivity.this, ResultActivity.class);
            intent.putExtra("time", elapsed);
            intent.putExtra("distance", totalDistance);
            intent.putExtra("pathJson", record.pathJson);
            startActivity(intent);
            finish();
        });

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true); // ← これが青いマーカーを表示する
        }

        mapReady = true;
        checkAndStart();
    }

    private void checkAndStart() {
        if (!mapReady) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f));
                locationReady = true;
                startCountdown();
            }
        });
    }

    private void startCountdown() {
        final int[] count = {5};
        textCountdown.setText(String.valueOf(count[0]));
        textCountdown.setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                count[0]--;
                if (count[0] > 0) {
                    textCountdown.setText(String.valueOf(count[0]));
                    new Handler().postDelayed(this, 1000);
                } else {
                    textCountdown.setVisibility(View.GONE);
                    startTime = SystemClock.elapsedRealtime();
                    startLocationUpdates();
                }
            }
        }, 1000);
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location currentLocation = locationResult.getLastLocation();
                if (previousLocation != null) {
                    float distance = previousLocation.distanceTo(currentLocation);
                    totalDistance += distance;

                    long elapsed = SystemClock.elapsedRealtime() - startTime;
                    currentSpeed = totalDistance / (elapsed / 1000f);
                    currentCalories = totalDistance * 0.05f;

                    textTime.setText("時間：" + (elapsed / 1000) + " 秒");
                    textDistance.setText(String.format("距離：%.2f km", totalDistance / 1000));
                    textSpeed.setText(String.format("速度：%.2f m/s", currentSpeed));
                    textCalories.setText(String.format("消費カロリー：%.1f kcal", currentCalories));

                    LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                    pathPoints.add(currentLatLng);

                    if (mMap != null) {
                        if (polyline != null) polyline.remove();
                        polyline = mMap.addPolyline(new PolylineOptions()
                                .addAll(pathPoints)
                                .width(20f)
                                .color(0xFF0000FF));
                    }

                    if (mMap != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f));
                    }
                }
                previousLocation = currentLocation;
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
