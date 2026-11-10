package com.mobile360.healthcare

import com.mobile360.healthcare.data.AccountRepository
import com.mobile360.healthcare.testutil.FakeAuthRepository
import com.mobile360.healthcare.testutil.MainDispatcherRule
import com.mobile360.healthcare.testutil.StubAccount
import com.mobile360.healthcare.testutil.runTestUnconfined
import com.mobile360.healthcare.ui.screens.SettingsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Tests: CR-002 (dark theme toggle). */
class Us044ThemeToggleTest {
    @get:Rule
    val main = MainDispatcherRule()

    private class ThemeAccount(
        private val theme: MutableStateFlow<Boolean> = MutableStateFlow(false),
        private val fail: Boolean = false,
        base: AccountRepository = StubAccount()
    ) : AccountRepository by base {
        override val darkTheme: Flow<Boolean> = theme
        override suspend fun setDarkTheme(enabled: Boolean) {
            if (fail) throw IllegalStateException("disk full")
            theme.value = enabled
        }
    }

    @Test
    fun test_cr_002_dark_mode_is_off_on_a_fresh_install() = runTestUnconfined {
        val vm = SettingsViewModel(FakeAuthRepository(), ThemeAccount())
        assertFalse(vm.darkTheme.value)
    }

    @Test
    fun test_cr_002_toggle_turns_dark_mode_on_and_off() = runTestUnconfined {
        val vm = SettingsViewModel(FakeAuthRepository(), ThemeAccount())
        vm.setDarkTheme(true)
        assertTrue(vm.darkTheme.value)
        vm.setDarkTheme(false)
        assertFalse(vm.darkTheme.value)
    }

    @Test
    fun test_cr_002_choice_persists_for_a_new_view_model() = runTestUnconfined {
        val account = ThemeAccount()
        SettingsViewModel(FakeAuthRepository(), account).setDarkTheme(true)
        assertTrue(SettingsViewModel(FakeAuthRepository(), account).darkTheme.value)
    }

    @Test
    fun test_cr_002_failed_save_keeps_switch_off() = runTestUnconfined {
        val vm = SettingsViewModel(FakeAuthRepository(), ThemeAccount(fail = true))
        vm.setDarkTheme(true)
        assertFalse(vm.darkTheme.value)
    }
}
