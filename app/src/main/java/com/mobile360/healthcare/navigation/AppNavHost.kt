package com.mobile360.healthcare.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mobile360.healthcare.data.model.Role
import com.mobile360.healthcare.ui.components.AppTab
import com.mobile360.healthcare.ui.components.CareBottomBar
import com.mobile360.healthcare.ui.screens.AppointmentsScreen
import com.mobile360.healthcare.ui.screens.AvailabilityScreen
import com.mobile360.healthcare.ui.screens.ChatScreen
import com.mobile360.healthcare.ui.screens.ConsultationScreen
import com.mobile360.healthcare.ui.screens.DoctorProfileAndBookingScreen
import com.mobile360.healthcare.ui.screens.DoctorScheduleScreen
import com.mobile360.healthcare.ui.screens.DoctorSearchScreen
import com.mobile360.healthcare.ui.screens.OTPEntryScreen
import com.mobile360.healthcare.ui.screens.PatientHomeScreen
import com.mobile360.healthcare.ui.screens.PhoneEntryScreen
import com.mobile360.healthcare.ui.screens.PrescriptionDetailScreen
import com.mobile360.healthcare.ui.screens.RecordsScreen
import com.mobile360.healthcare.ui.screens.SecurityAndAccountScreen
import com.mobile360.healthcare.ui.screens.SettingsScreen
import com.mobile360.healthcare.ui.screens.WelcomeScreen

private fun String?.base(): String? = this?.substringBefore('?')?.substringBefore('/')

private fun tabFor(route: String?): AppTab? = when (route.base()) {
    Routes.PATIENT_HOME, Routes.DOCTOR_SCHEDULE, Routes.DOCTOR_SEARCH -> AppTab.HOME
    Routes.APPOINTMENTS -> AppTab.APPOINTMENTS
    Routes.RECORDS, Routes.PRESCRIPTION_DETAIL -> AppTab.RECORDS
    Routes.CHAT -> AppTab.MESSAGES
    Routes.SETTINGS, Routes.SECURITY_AND_ACCOUNT -> AppTab.PROFILE
    else -> null
}

private fun NavHostController.openTab(tab: AppTab, role: Role?) {
    val target = when (tab) {
        AppTab.HOME -> Routes.homeFor(role)
        AppTab.APPOINTMENTS -> Routes.APPOINTMENTS
        AppTab.RECORDS -> Routes.RECORDS
        AppTab.MESSAGES -> Routes.CHAT
        AppTab.PROFILE -> Routes.SETTINGS
    }
    navigate(target) {
        popUpTo(Routes.homeFor(role)) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNavHost(startDestination: String, role: Role?) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val tab = tabFor(entry?.destination?.route)

    // The bottom bar is drawn here, once, for the signed-in tab screens; screens must not draw their own.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (role != null && tab != null) {
                CareBottomBar(selected = tab, onSelect = { nav.openTab(it, role) })
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = startDestination, modifier = Modifier.padding(padding)) {
            composable(Routes.WELCOME) {
                WelcomeScreen(onOpenPhoneEntry = { nav.navigate(Routes.PHONE_ENTRY) })
            }
            composable(Routes.PHONE_ENTRY) {
                PhoneEntryScreen(
                    onOpenOTPEntry = { nav.navigate(Routes.O_T_P_ENTRY) },
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.O_T_P_ENTRY) {
                OTPEntryScreen(
                    onOpenPatientHome = {
                        nav.navigate(Routes.PATIENT_HOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
                    },
                    onOpenDoctorSchedule = {
                        nav.navigate(Routes.DOCTOR_SCHEDULE) { popUpTo(Routes.WELCOME) { inclusive = true } }
                    },
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.PATIENT_HOME) {
                PatientHomeScreen(
                    onOpenAppointments = { nav.navigate(Routes.APPOINTMENTS) },
                    onOpenDoctorSearch = { nav.navigate(Routes.DOCTOR_SEARCH) },
                    onOpenRecords = { nav.navigate(Routes.RECORDS) },
                    onOpenChat = { nav.navigate(Routes.chat()) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.DOCTOR_SCHEDULE) {
                DoctorScheduleScreen(
                    onOpenConsultation = { id -> nav.navigate(Routes.consultation(id)) },
                    onOpenAvailability = { nav.navigate(Routes.AVAILABILITY) }
                )
            }
            composable(Routes.DOCTOR_SEARCH) {
                DoctorSearchScreen(
                    onOpenDoctorProfileAndBooking = { doctorId -> nav.navigate(Routes.doctorProfileAndBooking(doctorId)) },
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                route = Routes.DOCTOR_PROFILE_AND_BOOKING_PATTERN,
                arguments = listOf(
                    navArgument(Routes.ARG_DOCTOR_ID) { type = NavType.StringType },
                    navArgument(Routes.ARG_APPOINTMENT_ID) { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) {
                DoctorProfileAndBookingScreen(
                    onOpenAppointments = {
                        nav.navigate(Routes.APPOINTMENTS) {
                            popUpTo(Routes.DOCTOR_PROFILE_AND_BOOKING_PATTERN) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.APPOINTMENTS) {
                AppointmentsScreen(
                    onOpenDoctorProfileAndBooking = { doctorId, appointmentId ->
                        nav.navigate(Routes.doctorProfileAndBooking(doctorId, appointmentId))
                    },
                    onOpenChat = { appointmentId -> nav.navigate(Routes.chat(appointmentId)) },
                    onOpenRecords = { nav.navigate(Routes.RECORDS) }
                )
            }
            composable(
                route = Routes.CHAT_PATTERN,
                arguments = listOf(
                    navArgument(Routes.ARG_APPOINTMENT_ID) { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) {
                ChatScreen(onOpenSettings = { nav.navigate(Routes.SETTINGS) })
            }
            composable(Routes.RECORDS) {
                RecordsScreen(
                    onOpenPrescriptionDetail = { id -> nav.navigate(Routes.prescriptionDetail(id)) },
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                route = Routes.PRESCRIPTION_DETAIL_PATTERN,
                arguments = listOf(navArgument(Routes.ARG_PRESCRIPTION_ID) { type = NavType.StringType })
            ) {
                PrescriptionDetailScreen(onBack = { nav.popBackStack() })
            }
            composable(
                route = Routes.CONSULTATION_PATTERN,
                arguments = listOf(navArgument(Routes.ARG_APPOINTMENT_ID) { type = NavType.StringType })
            ) {
                ConsultationScreen(
                    onOpenChat = { appointmentId -> nav.navigate(Routes.chat(appointmentId)) },
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.AVAILABILITY) {
                AvailabilityScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenSecurityAndAccount = { nav.navigate(Routes.SECURITY_AND_ACCOUNT) },
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.SECURITY_AND_ACCOUNT) {
                SecurityAndAccountScreen(
                    onOpenWelcome = {
                        nav.navigate(Routes.WELCOME) { popUpTo(nav.graph.id) { inclusive = true } }
                    },
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
