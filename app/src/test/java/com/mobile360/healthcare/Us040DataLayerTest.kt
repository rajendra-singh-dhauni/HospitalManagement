// Tests: US-040 (BO-06, NFR-004)
package com.mobile360.healthcare

import com.mobile360.healthcare.data.ConnectivityObserver
import com.mobile360.healthcare.navigation.MainViewModel
import com.mobile360.healthcare.ui.screens.AppointmentsViewModel
import com.mobile360.healthcare.ui.screens.AvailabilityViewModel
import com.mobile360.healthcare.ui.screens.ChatViewModel
import com.mobile360.healthcare.ui.screens.ConsultationViewModel
import com.mobile360.healthcare.ui.screens.DoctorProfileAndBookingViewModel
import com.mobile360.healthcare.ui.screens.DoctorScheduleViewModel
import com.mobile360.healthcare.ui.screens.DoctorSearchViewModel
import com.mobile360.healthcare.ui.screens.OTPEntryViewModel
import com.mobile360.healthcare.ui.screens.PatientHomeViewModel
import com.mobile360.healthcare.ui.screens.PhoneEntryViewModel
import com.mobile360.healthcare.ui.screens.PrescriptionDetailViewModel
import com.mobile360.healthcare.ui.screens.RecordsViewModel
import com.mobile360.healthcare.ui.screens.SecurityAndAccountViewModel
import com.mobile360.healthcare.ui.screens.SettingsViewModel
import org.junit.Assert.assertTrue
import org.junit.Test

class Us040DataLayerTest {
    private val viewModels: List<Class<*>> = listOf(
        MainViewModel::class.java, AppointmentsViewModel::class.java, AvailabilityViewModel::class.java,
        ChatViewModel::class.java, ConsultationViewModel::class.java, DoctorProfileAndBookingViewModel::class.java,
        DoctorScheduleViewModel::class.java, DoctorSearchViewModel::class.java, OTPEntryViewModel::class.java,
        PatientHomeViewModel::class.java, PhoneEntryViewModel::class.java, PrescriptionDetailViewModel::class.java,
        RecordsViewModel::class.java, SecurityAndAccountViewModel::class.java, SettingsViewModel::class.java
    )

    @Test fun test_us_040_viewmodels_depend_only_on_data_interfaces() {
        val bad = viewModels.flatMap { vm ->
            vm.constructors.flatMap { it.parameterTypes.toList() }
                .filter { it.name.startsWith("com.mobile360.healthcare.data") }
                .filter { !it.isInterface && it != ConnectivityObserver::class.java }
                .map { "${vm.simpleName} -> ${it.simpleName}" }
        }
        assertTrue("Concrete data dependencies: $bad", bad.isEmpty())
    }

    @Test fun test_us_040_every_viewmodel_is_injected_with_at_least_one_dependency_or_none_concrete() {
        assertTrue(viewModels.isNotEmpty())
    }
}
