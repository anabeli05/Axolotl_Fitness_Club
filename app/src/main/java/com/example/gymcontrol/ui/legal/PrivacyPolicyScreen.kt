package com.example.gymcontrol.ui.legal

import androidx.compose.runtime.Composable

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    LegalDocumentScreen(
        title = "Política de Privacidad",
        sections = LegalContent.politicaDePrivacidad,
        onBack = onBack
    )
}
