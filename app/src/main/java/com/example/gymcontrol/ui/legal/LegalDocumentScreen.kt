package com.example.gymcontrol.ui.legal

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import com.example.gymcontrol.ui.theme.GymColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentScreen(
    title: String,
    lastUpdated: String? = null,
    sections: List<LegalSection>,
    onBack: () -> Unit,
    onAccept: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    fun ejecutarConDelayCorto(accion: () -> Unit) {
        val ahora = SystemClock.elapsedRealtime()
        if (ahora - lastClickTime >= 350L) {
            lastClickTime = ahora
            accion()
        }
    }

    BackHandler {
        ejecutarConDelayCorto { onBack() }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GymColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        color = GymColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { ejecutarConDelayCorto { onBack() } }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.legal_back),
                            tint = GymColors.Gold
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { ejecutarConDelayCorto { onAccept() } }
                    ) {
                        Text(
                            text = stringResource(R.string.btn_accept),
                            color = GymColors.Gold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GymColors.Surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (lastUpdated != null) {
                item {
                    Text(
                        text = lastUpdated,
                        color = GymColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            items(sections) { section ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = GymColors.Border,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = GymColors.Surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = section.title,
                            color = GymColors.Gold,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = section.content,
                            color = GymColors.TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
