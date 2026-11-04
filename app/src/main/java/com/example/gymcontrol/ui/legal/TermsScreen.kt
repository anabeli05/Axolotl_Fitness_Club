package com.example.gymcontrol.ui.legal

import androidx.compose.runtime.Composable

@Composable
fun TermsScreen(onBack: () -> Unit) {
    LegalDocumentScreen(
        title = "Términos y Condiciones",
        sections = LegalContent.terminosYCondiciones,
        onBack = onBack
    )
}
