package jp.ac.gifu_u.info.matsui.arukunzesu;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {WalkRecord.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract WalkRecordDao walkRecordDao();
}
