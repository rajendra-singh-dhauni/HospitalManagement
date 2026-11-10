package com.mobile360.healthcare

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile360.healthcare.navigation.AppNavHost
import com.mobile360.healthcare.navigation.AppState
import com.mobile360.healthcare.navigation.MainViewModel
import com.mobile360.healthcare.navigation.Routes
import com.mobile360.healthcare.ui.theme.CareConnectTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CareConnectTheme { AppRoot() }
        }
    }
}

@Composable
fun AppRoot(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            when (val s = state) {
                AppState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> {
                    val start = remember { Routes.startFor((s as? AppState.SignedIn)?.role) }
                    AppNavHost(startDestination = start, role = (s as? AppState.SignedIn)?.role)
                }
            }
        }
    }
}
