package com.example.go2play.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.go2play.data.local.dao.EventDao
import com.example.go2play.data.local.dao.FieldDao
import com.example.go2play.data.local.entity.EventEntity
import com.example.go2play.data.local.entity.FieldEntity

@Database(
    entities = [
        FieldEntity::class,
        EventEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fieldDao(): FieldDao
    abstract fun eventDao(): EventDao
}

