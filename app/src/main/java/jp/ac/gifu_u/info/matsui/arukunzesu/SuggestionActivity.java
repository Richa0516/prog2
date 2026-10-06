package jp.ac.gifu_u.info.matsui.arukunzesu;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class SuggestionActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private final int LOCATION_PERMISSION_REQUEST = 1001;
    private final String DIRECTIONS_API_KEY = BuildConfig.DIRECTIONS_API_KEY;

    private final LatLng gifuGate = new LatLng(35.462199, 136.735916);
    private final LatLng marusa21 = new LatLng(35.448997, 136.743398);
    private final LatLng kuronoPark = new LatLng(35.459893, 136.723375);

    private Map<String, LatLng> favoriteDestinations = new HashMap<>();
    private Map<String, Polyline> routeMap = new HashMap<>();
    private LatLng currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestion);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.suggestionMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        mMap.setMyLocationEnabled(true);

        mMap.setOnMapLongClickListener(latLng -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("お気に入りの名前を入力");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("追加", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    favoriteDestinations.put(name, latLng);
                    mMap.addMarker(new MarkerOptions().position(latLng).title(name));
                    saveFavorites(favoriteDestinations);
                    if (currentLocation != null) {
                        fetchDirectionsAndDrawRoute(currentLocation, latLng, name, Color.MAGENTA);
                    }
                }
            });

            builder.setNegativeButton("キャンセル", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        mMap.setOnMarkerClickListener(marker -> {
            String name = marker.getTitle();
            if (favoriteDestinations.containsKey(name)) {
                new AlertDialog.Builder(this)
                        .setTitle("お気に入りの削除")
                        .setMessage("「" + name + "」をお気に入りから削除しますか？")
                        .setPositiveButton("削除", (dialog, which) -> {
                            favoriteDestinations.remove(name);
                            marker.remove();
                            saveFavorites(favoriteDestinations);
                            if (routeMap.containsKey(name)) {
                                routeMap.get(name).remove();
                                routeMap.remove(name);
                            }
                        })
                        .setNegativeButton("キャンセル", null)
                        .show();
                return true;
            }
            return false;
        });

        favoriteDestinations = loadFavorites();

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 14f));

                fetchDirectionsAndDrawRoute(currentLocation, gifuGate, "岐阜大学正門", Color.RED);
                fetchDirectionsAndDrawRoute(currentLocation, marusa21, "マーサ21", Color.BLUE);
                fetchDirectionsAndDrawRoute(currentLocation, kuronoPark, "黒野城跡公園", Color.GREEN);

                for (Map.Entry<String, LatLng> entry : favoriteDestinations.entrySet()) {
                    fetchDirectionsAndDrawRoute(currentLocation, entry.getValue(), entry.getKey(), Color.MAGENTA);
                }
            }
        });
    }

    private void fetchDirectionsAndDrawRoute(LatLng origin, LatLng destination, String title, int color) {
        if (DIRECTIONS_API_KEY.isEmpty()) {
            return;
        }
        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&mode=walking&key=" + DIRECTIONS_API_KEY;

        new Thread(() -> {
            try {
                URL directionUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) directionUrl.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject json = new JSONObject(response.toString());
                JSONArray routes = json.getJSONArray("routes");

                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String encodedPoints = overviewPolyline.getString("points");

                    List<LatLng> points = decodePolyline(encodedPoints);

                    runOnUiThread(() -> {
                        Polyline polyline = mMap.addPolyline(new PolylineOptions()
                                .addAll(points)
                                .width(10f)
                                .color(color));
                        mMap.addMarker(new MarkerOptions().position(destination).title(title));
                        if (!routeMap.containsKey(title)) {
                            routeMap.put(title, polyline);
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("Arukundesu", "Route request failed; check configuration and connectivity.");
            }
        }).start();
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lng += dlng;

            LatLng p = new LatLng((lat / 1E5), (lng / 1E5));
            poly.add(p);
        }
        return poly;
    }

    private void saveFavorites(Map<String, LatLng> map) {
        JSONArray array = new JSONArray();
        for (Map.Entry<String, LatLng> entry : map.entrySet()) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", entry.getKey());
                obj.put("lat", entry.getValue().latitude);
                obj.put("lng", entry.getValue().longitude);
                array.put(obj);
            } catch (JSONException e) {
                android.util.Log.e("Arukundesu", "Route request failed; check configuration and connectivity.");
            }
        }
        getSharedPreferences("prefs", MODE_PRIVATE).edit()
                .putString("favorites", array.toString())
                .apply();
    }

    private Map<String, LatLng> loadFavorites() {
        Map<String, LatLng> map = new HashMap<>();
        String json = getSharedPreferences("prefs", MODE_PRIVATE).getString("favorites", "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = obj.getString("name");
                double lat = obj.getDouble("lat");
                double lng = obj.getDouble("lng");
                map.put(name, new LatLng(lat, lng));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            recreate();
        }
    }
}
