package com.example.gymcontrol.ui.legal

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gymcontrol.R

data class LegalSection(
    val title: String,
    val content: String
)

object LegalContent {

    @Composable
    fun getTerminosYCondiciones(): List<LegalSection> = listOf(
        LegalSection(stringResource(R.string.term_1_title), stringResource(R.string.term_1_content)),
        LegalSection(stringResource(R.string.term_2_title), stringResource(R.string.term_2_content)),
        LegalSection(stringResource(R.string.term_3_title), stringResource(R.string.term_3_content)),
        LegalSection(stringResource(R.string.term_4_title), stringResource(R.string.term_4_content)),
        LegalSection(stringResource(R.string.term_5_title), stringResource(R.string.term_5_content)),
        LegalSection(stringResource(R.string.term_6_title), stringResource(R.string.term_6_content)),
        LegalSection(stringResource(R.string.term_7_title), stringResource(R.string.term_7_content)),
        LegalSection(stringResource(R.string.term_8_title), stringResource(R.string.term_8_content)),
        LegalSection(stringResource(R.string.term_9_title), stringResource(R.string.term_9_content)),
        LegalSection(stringResource(R.string.term_10_title), stringResource(R.string.term_10_content)),
        LegalSection(stringResource(R.string.term_11_title), stringResource(R.string.term_11_content)),
        LegalSection(stringResource(R.string.term_12_title), stringResource(R.string.term_12_content)),
        LegalSection(stringResource(R.string.term_13_title), stringResource(R.string.term_13_content)),
        LegalSection(stringResource(R.string.term_14_title), stringResource(R.string.term_14_content)),
        LegalSection(stringResource(R.string.term_15_title), stringResource(R.string.term_15_content)),
        LegalSection(stringResource(R.string.term_16_title), stringResource(R.string.term_16_content)),
        LegalSection(stringResource(R.string.term_17_title), stringResource(R.string.term_17_content)),
        LegalSection(stringResource(R.string.term_18_title), stringResource(R.string.term_18_content)),
        LegalSection(stringResource(R.string.term_19_title), stringResource(R.string.term_19_content))
    )

    @Composable
    fun getPoliticaDePrivacidad(): List<LegalSection> = listOf(
        LegalSection(stringResource(R.string.privacy_1_title), stringResource(R.string.privacy_1_content)),
        LegalSection(stringResource(R.string.privacy_2_title), stringResource(R.string.privacy_2_content)),
        LegalSection(stringResource(R.string.privacy_3_title), stringResource(R.string.privacy_3_content)),
        LegalSection(stringResource(R.string.privacy_4_title), stringResource(R.string.privacy_4_content)),
        LegalSection(stringResource(R.string.privacy_5_title), stringResource(R.string.privacy_5_content)),
        LegalSection(stringResource(R.string.privacy_6_title), stringResource(R.string.privacy_6_content)),
        LegalSection(stringResource(R.string.privacy_7_title), stringResource(R.string.privacy_7_content)),
        LegalSection(stringResource(R.string.privacy_8_title), stringResource(R.string.privacy_8_content)),
        LegalSection(stringResource(R.string.privacy_9_title), stringResource(R.string.privacy_9_content)),
        LegalSection(stringResource(R.string.privacy_10_title), stringResource(R.string.privacy_10_content)),
        LegalSection(stringResource(R.string.privacy_11_title), stringResource(R.string.privacy_11_content)),
        LegalSection(stringResource(R.string.privacy_12_title), stringResource(R.string.privacy_12_content)),
        LegalSection(stringResource(R.string.privacy_13_title), stringResource(R.string.privacy_13_content)),
        LegalSection(stringResource(R.string.privacy_14_title), stringResource(R.string.privacy_14_content))
    )
}
