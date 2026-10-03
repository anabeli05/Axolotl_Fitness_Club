package com.example.gymcontrol.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

val LocalOnLogout = staticCompositionLocalOf<() -> Unit> { {} }