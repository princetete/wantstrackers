package com.wantstracker.app.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun LockScreen(activity: FragmentActivity, onUnlock: () -> Unit) {
    fun prompt() {
        val executor = ContextCompat.getMainExecutor(activity)
        val bp = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onUnlock()
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Wants Tracker")
            .setAllowedAuthenticators(LockAuth.AUTHENTICATORS)
            .build()
        bp.authenticate(info)
    }

    LaunchedEffect(Unit) {
        // If the phone no longer has any screen lock, don't lock the user out of their own data.
        val can = BiometricManager.from(activity).canAuthenticate(LockAuth.AUTHENTICATORS)
        if (can == BiometricManager.BIOMETRIC_SUCCESS) prompt() else onUnlock()
    }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔒", fontSize = 56.sp)
        Text("Wants Tracker", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Button(onClick = { prompt() }) { Text("Unlock") }
    }
}
