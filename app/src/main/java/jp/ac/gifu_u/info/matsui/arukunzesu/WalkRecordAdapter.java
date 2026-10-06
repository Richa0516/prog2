package jp.ac.gifu_u.info.matsui.arukunzesu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WalkRecordAdapter extends RecyclerView.Adapter<WalkRecordAdapter.ViewHolder> {

    private List<WalkRecord> recordList;

    public WalkRecordAdapter(List<WalkRecord> records) {
        this.recordList = records;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateText, distanceText, timeText, speedText, caloriesText;

        public ViewHolder(View view) {
            super(view);
            dateText = view.findViewById(R.id.textDate);
            distanceText = view.findViewById(R.id.textDistance);
            timeText = view.findViewById(R.id.textTime);
            speedText = view.findViewById(R.id.textSpeed);
            caloriesText = view.findViewById(R.id.textCalories);
        }
    }

    @NonNull
    @Override
    public WalkRecordAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WalkRecordAdapter.ViewHolder holder, int position) {
        WalkRecord record = recordList.get(position);
        holder.dateText.setText("日付: " + record.date + " " + record.startTime);
        holder.distanceText.setText(String.format("距離: %.2f km", record.distanceKm));
        holder.timeText.setText("時間: " + record.time + "秒");
        holder.speedText.setText(String.format("速度: %.2f m/s", record.speed));
        holder.caloriesText.setText(String.format("カロリー: %.1f kcal", record.calories));

        holder.itemView.setOnClickListener(v -> {
            android.content.Context context = v.getContext();
            android.content.Intent intent = new android.content.Intent(context, ResultActivity.class);
            intent.putExtra("time", record.time);
            intent.putExtra("distance", (float) (record.distanceKm * 1000));  // mに変換
            intent.putExtra("pathJson", record.pathJson);
            context.startActivity(intent);
        });;
    }

    @Override
    public int getItemCount() {
        return recordList.size();
    }
}
