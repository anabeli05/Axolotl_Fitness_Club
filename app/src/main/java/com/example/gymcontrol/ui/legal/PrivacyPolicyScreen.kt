package com.example.gymcontrol.ui.legal

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gymcontrol.R

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    LegalDocumentScreen(
        title = stringResource(R.string.privacy_policy),
        sections = LegalContent.getPoliticaDePrivacidad(),
        onBack = onBack
    )
}
