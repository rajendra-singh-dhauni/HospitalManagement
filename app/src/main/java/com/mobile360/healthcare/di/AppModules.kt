package com.mobile360.healthcare.di

import android.content.Context
import androidx.room.Room
import com.mobile360.healthcare.data.AccountRepository
import com.mobile360.healthcare.data.AccountRepositoryImpl
import com.mobile360.healthcare.data.AppointmentRepository
import com.mobile360.healthcare.data.AppointmentRepositoryImpl
import com.mobile360.healthcare.data.AuthRepository
import com.mobile360.healthcare.data.AuthRepositoryImpl
import com.mobile360.healthcare.data.AvailabilityRepository
import com.mobile360.healthcare.data.AvailabilityRepositoryImpl
import com.mobile360.healthcare.data.ChatRepository
import com.mobile360.healthcare.data.ChatRepositoryImpl
import com.mobile360.healthcare.data.DoctorRepository
import com.mobile360.healthcare.data.DoctorRepositoryImpl
import com.mobile360.healthcare.data.RecordsRepository
import com.mobile360.healthcare.data.RecordsRepositoryImpl
import com.mobile360.healthcare.data.local.AppDatabase
import com.mobile360.healthcare.data.local.CareDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "careconnect.db").fallbackToDestructiveMigration().build()

    @Provides
    fun dao(db: AppDatabase): CareDao = db.dao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun auth(impl: AuthRepositoryImpl): AuthRepository
    @Binds abstract fun account(impl: AccountRepositoryImpl): AccountRepository
    @Binds abstract fun doctors(impl: DoctorRepositoryImpl): DoctorRepository
    @Binds abstract fun appointments(impl: AppointmentRepositoryImpl): AppointmentRepository
    @Binds abstract fun records(impl: RecordsRepositoryImpl): RecordsRepository
    @Binds abstract fun chat(impl: ChatRepositoryImpl): ChatRepository
    @Binds abstract fun availability(impl: AvailabilityRepositoryImpl): AvailabilityRepository
}
