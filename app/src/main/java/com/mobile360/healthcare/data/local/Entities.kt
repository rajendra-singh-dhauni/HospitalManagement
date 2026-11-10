package com.mobile360.healthcare.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val phone: String,
    val name: String,
    val role: String,
    val age: Int,
    val sex: String,
    val imageRes: Int,
    val doctorId: String?
)

@Entity(tableName = "doctors")
data class DoctorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val specialty: String,
    val title: String,
    val experienceYears: Int,
    val rating: Double,
    val reviewCount: Int,
    val languages: String,
    val headline: String,
    val clinic: String,
    val address: String,
    val phone: String,
    val imageRes: Int,
    val active: Boolean,
    val fee: Int,
    val bio: String
)

@Entity(tableName = "slots", indices = [Index("doctorId")])
data class SlotEntity(
    @PrimaryKey val id: String,
    val doctorId: String,
    val startMillis: Long,
    val booked: Boolean
)

@Entity(tableName = "appointments", indices = [Index("patientId"), Index("doctorId")])
data class AppointmentEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val doctorId: String,
    val slotId: String?,
    val startMillis: Long,
    val status: String,
    val reason: String,
    val room: String = "",
    val instructions: String = "",
    val prepNote: String = "",
    val fileNo: String = "",
    val vitals: String = "",
    val referral: String = "",
    val mode: String = "In-Person",
    val cancelNote: String = ""
)

@Entity(tableName = "prescriptions", indices = [Index("patientId")])
data class PrescriptionEntity(
    @PrimaryKey val id: String,
    val rxNumber: String,
    val patientId: String,
    val doctorId: String,
    val appointmentId: String?,
    val issuedMillis: Long,
    val status: String,
    val advice: String,
    val version: Int,
    val updatedMillis: Long
)

@Entity(tableName = "medications", indices = [Index("prescriptionId")])
data class MedicationEntity(
    @PrimaryKey val id: String,
    val prescriptionId: String,
    val name: String,
    val category: String,
    val dosage: String,
    val frequency: String,
    val supplyDays: Int,
    val refillsLeft: Int,
    val note: String
)

@Entity(tableName = "timeline", indices = [Index("patientId")])
data class TimelineEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val type: String,
    val title: String,
    val detail: String,
    val extra: String,
    val dateMillis: Long,
    val refId: String?
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val appointmentId: String,
    val text: String,
    val updatedMillis: Long
)

@Entity(tableName = "messages", indices = [Index("appointmentId")])
data class MessageEntity(
    @PrimaryKey val id: String,
    val appointmentId: String,
    val senderId: String,
    val text: String,
    val timeMillis: Long,
    val status: String
)

@Entity(tableName = "availability")
data class AvailabilityEntity(
    @PrimaryKey val id: String,
    val doctorId: String,
    val dayOfWeek: Int,
    val enabled: Boolean,
    val startTime: String,
    val endTime: String
)

data class AppointmentView(
    val id: String,
    val patientId: String,
    val doctorId: String,
    val slotId: String?,
    val startMillis: Long,
    val status: String,
    val reason: String,
    val room: String,
    val instructions: String,
    val prepNote: String,
    val fileNo: String,
    val vitals: String,
    val referral: String,
    val mode: String,
    val cancelNote: String,
    val patientName: String,
    val patientAge: Int,
    val patientSex: String,
    val patientImage: Int,
    val doctorName: String,
    val doctorSpecialty: String,
    val doctorTitle: String,
    val doctorImage: Int,
    val doctorClinic: String,
    val doctorAddress: String,
    val doctorPhone: String
)

data class PrescriptionView(
    val id: String,
    val rxNumber: String,
    val patientId: String,
    val doctorId: String,
    val appointmentId: String?,
    val issuedMillis: Long,
    val status: String,
    val advice: String,
    val version: Int,
    val updatedMillis: Long,
    val patientName: String,
    val doctorName: String,
    val doctorSpecialty: String,
    val doctorImage: Int,
    val clinic: String,
    val address: String
)

data class ThreadView(
    val appointmentId: String,
    val doctorId: String,
    val doctorName: String,
    val doctorSpecialty: String,
    val doctorImage: Int,
    val doctorPhone: String,
    val patientId: String,
    val patientName: String,
    val patientImage: Int,
    val lastMessage: String?,
    val lastTime: Long?,
    val startMillis: Long
)

data class DoctorListItem(
    @Embedded val doctor: DoctorEntity,
    val nextSlotMillis: Long?
)
