package com.mobile360.healthcare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccountEntity::class, DoctorEntity::class, SlotEntity::class, AppointmentEntity::class,
        PrescriptionEntity::class, MedicationEntity::class, TimelineEntity::class, NoteEntity::class,
        MessageEntity::class, AvailabilityEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): CareDao
}
