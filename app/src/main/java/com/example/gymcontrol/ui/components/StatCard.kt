package com.example.gymcontrol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.ui.theme.GymColors

enum class StatIconType { PERSON, MONEY }

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = GymColors.TextPrimary,
    iconType: StatIconType? = null,
    iconColor: Color = GymColors.Gold
) {
    val borderBrush = Brush.linearGradient(
        colors = listOf(GymColors.Purple, GymColors.Gold)
    )

    Box(
        modifier = modifier
            .background(GymColors.Surface, RoundedCornerShape(16.dp))
            .border(1.5.dp, borderBrush, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        if (iconType != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 10.dp)
                    .size(55.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (iconType) {
                        StatIconType.PERSON -> Icons.Filled.Person
                        StatIconType.MONEY -> Icons.Filled.AttachMoney
                    },
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(35.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = GymColors.TextSecondary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}