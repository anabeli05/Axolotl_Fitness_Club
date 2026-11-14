package com.example.gymcontrol.ui.legal

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gymcontrol.R

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit, onAccept: () -> Unit = onBack) {
    LegalDocumentScreen(
        title = stringResource(R.string.privacy_title),
        lastUpdated = stringResource(R.string.privacy_last_updated),
        sections = LegalContent.getPoliticaDePrivacidad(),
        onBack = onBack,
        onAccept = onAccept
    )
}
