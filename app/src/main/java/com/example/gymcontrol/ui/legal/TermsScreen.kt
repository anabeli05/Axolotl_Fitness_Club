package com.example.gymcontrol.ui.legal

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gymcontrol.R

@Composable
fun TermsScreen(onBack: () -> Unit, onAccept: () -> Unit = onBack) {
    LegalDocumentScreen(
        title = stringResource(R.string.terms_title),
        lastUpdated = stringResource(R.string.terms_last_updated),
        sections = LegalContent.getTerminosYCondiciones(),
        onBack = onBack,
        onAccept = onAccept
    )
}
