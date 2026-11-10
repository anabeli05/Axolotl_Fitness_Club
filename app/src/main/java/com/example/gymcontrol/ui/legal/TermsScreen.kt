package com.example.gymcontrol.ui.legal

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gymcontrol.R

@Composable
fun TermsScreen(onBack: () -> Unit) {
    LegalDocumentScreen(
        title = stringResource(R.string.terms_and_conditions),
        sections = LegalContent.getTerminosYCondiciones(),
        onBack = onBack
    )
}
