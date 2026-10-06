package jp.ac.gifu_u.info.matsui.arukunzesu;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class WalkRecord {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String date;        // 日付（例: 2025/06/11）
    public String startTime;   // 開始時刻（例: 14:23）
    public int time;           // 時間（秒単位）
    public float distanceKm;   // 距離（km）
    public float speed;        // 平均速度（m/s）
    public float calories;     // 消費カロリー（kcal）

    @NonNull
    public String pathJson;    // 軌跡データをJSON形式で保存
}
