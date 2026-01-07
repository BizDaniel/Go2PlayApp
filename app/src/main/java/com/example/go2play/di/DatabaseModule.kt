package com.example.go2play.di

import android.content.Context
import androidx.room.Room
import com.example.go2play.data.local.AppDatabase
import com.example.go2play.data.local.dao.EventDao
import com.example.go2play.data.local.dao.FieldDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "go2play_database"
        ).build()
    }

    @Provides
    fun provideFieldDao(database: AppDatabase): FieldDao {
        return database.fieldDao()
    }

    @Provides
    fun provideEventDao(database: AppDatabase): EventDao {
        return database.eventDao()
    }
}