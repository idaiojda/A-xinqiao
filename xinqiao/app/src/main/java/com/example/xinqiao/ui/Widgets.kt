package com.example.xinqiao.ui

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LoadingButton(enabled: Boolean, loading: Boolean, label: String, onClick: () -> Unit, color: Color) {
    Button(onClick = onClick, enabled = enabled && !loading, colors = ButtonDefaults.buttonColors(containerColor = color), modifier = Modifier.height(48.dp)) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            androidx.compose.foundation.layout.Spacer(Modifier.height(0.dp))
        }
        Text(label, color = Color.White)
    }
}

@Composable
fun ErrorSnackbar(hostState: SnackbarHostState) {
    SnackbarHost(hostState = hostState)
}