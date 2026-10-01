package com.example.gymcontrol.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.ui.theme.GymColors

@Composable
fun GymSidebar(
    currentSection: String,
    sections: List<String>,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(220.dp)
            .background(GymColors.Surface)
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = com.example.gymcontrol.R.drawable.logo_axolotl),
                contentDescription = "Logo Axolotl",
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "AXOLOTL",
                color = GymColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(Modifier.height(32.dp))

        sections.forEach { section ->
            val isSelected = section == currentSection
            val interactionSource = remember { MutableInteractionSource() }
            Text(
                text = section,
                color = if (isSelected) GymColors.Gold else GymColors.TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = interactionSource
                    ) { onNavigate(section) }
                    .padding(vertical = 12.dp)
            )
        }
    }
}