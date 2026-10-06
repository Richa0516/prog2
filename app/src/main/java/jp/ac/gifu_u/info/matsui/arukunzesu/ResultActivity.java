package jp.ac.gifu_u.info.matsui.arukunzesu;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ResultActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private List<LatLng> pathPoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        long time = getIntent().getLongExtra("time", 0);
        float distance = getIntent().getFloatExtra("distance", 0f);
        String pathJson = getIntent().getStringExtra("pathJson");

        // JSON 文字列 → List<LatLng>
        if (pathJson != null) {
            try {
                JSONArray array = new JSONArray(pathJson);
                for (int i = 0; i < array.length(); i++) {
                    JSONArray latlng = array.getJSONArray(i);
                    double lat = latlng.getDouble(0);
                    double lng = latlng.getDouble(1);
                    pathPoints.add(new LatLng(lat, lng));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        float speed = distance / (time / 1000f);
        float calories = distance * 0.05f;

        ((TextView) findViewById(R.id.resultTime)).setText("時間：" + (time / 1000) + " 秒");
        ((TextView) findViewById(R.id.resultDistance)).setText(String.format("距離：%.2f km", distance / 1000));
        ((TextView) findViewById(R.id.resultSpeed)).setText(String.format("速度：%.2f m/s", speed));
        ((TextView) findViewById(R.id.resultCalories)).setText(String.format("消費カロリー：%.1f kcal", calories));

        findViewById(R.id.btnReturnHome).setOnClickListener(v -> {
            Intent intent = new Intent(ResultActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.resultMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;

        if (!pathPoints.isEmpty()) {
            mMap.addPolyline(new PolylineOptions()
                    .addAll(pathPoints)
                    .width(10f)
                    .color(0xFF0000FF));

            // 中央にズーム
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pathPoints.get(0), 17f));
        }
    }
}
