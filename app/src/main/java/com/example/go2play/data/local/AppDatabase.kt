package com.example.go2play.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.go2play.data.local.dao.EventDao
import com.example.go2play.data.local.dao.FieldDao
import com.example.go2play.data.local.dao.UserProfileDao
import com.example.go2play.data.local.entity.EventEntity
import com.example.go2play.data.local.entity.FieldEntity
import com.example.go2play.data.local.entity.UserProfileEntity

@Database(
    entities = [
        FieldEntity::class,
        EventEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun fieldDao(): FieldDao
    abstract fun eventDao(): EventDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        const val DATABASE_NAME = "go2play_database"
    }
}