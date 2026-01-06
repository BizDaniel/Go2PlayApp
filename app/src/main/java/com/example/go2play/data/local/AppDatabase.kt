package com.example.go2play.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.go2play.data.local.dao.FieldDao
import com.example.go2play.data.local.entity.FieldEntity

@Database(entities = [FieldEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fieldDao(): FieldDao
}

