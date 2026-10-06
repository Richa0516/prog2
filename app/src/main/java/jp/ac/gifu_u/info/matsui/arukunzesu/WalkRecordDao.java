package jp.ac.gifu_u.info.matsui.arukunzesu;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WalkRecordDao {

    // 記録を1件追加
    @Insert
    void insert(WalkRecord record);

    // すべての記録を取得（新しい順）
    @Query("SELECT * FROM WalkRecord ORDER BY id DESC")
    List<WalkRecord> getAll();
}
