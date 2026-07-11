/*
 * This file is part of Tuner.
 * Copyright (C) 2026 Philipp Bobek <philipp.bobek@mailbox.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tuner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bobek.tuner.ui.tuner

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bobek.tuner.R
import com.bobek.tuner.domain.DetectedNote
import kotlin.math.abs

@Composable
@PreviewScreenSizes
@OptIn(ExperimentalMaterial3Api::class)
fun TunerScreen(
    @PreviewParameter(TunerScreenViewModelProvider::class) viewModel: ITunerViewModel,
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.getTunerStateFlow().collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startListening()
    }

    DisposableEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, RECORD_AUDIO) == PERMISSION_GRANTED) {
            viewModel.startListening()
        }
        onDispose { viewModel.stopListening() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tuner)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        },
        content = { padding ->

            val cents: Int? = (state as? TunerState.Listening)?.note?.cents

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (val currentState = state) {
                        is TunerState.Idle -> IdleContent(
                            onGrantPermission = { permissionLauncher.launch(RECORD_AUDIO) }
                        )

                        is TunerState.Listening -> ListeningContent(
                            note = currentState.note
                        )
                    }
                }

                CentsMeter(
                    cents = cents,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("−50¢", style = MaterialTheme.typography.labelSmall)
                    Text("0¢", style = MaterialTheme.typography.labelSmall)
                    Text("+50¢", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    )
}

@Composable
private fun IdleContent(
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.tuner_permission_rationale),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(onClick = onGrantPermission) {
            Text(stringResource(R.string.tuner_grant_permission))
        }
    }
}

@Composable
private fun ListeningContent(
    note: DetectedNote?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val noteColor = note?.let { centsColor(it.cents) }
            ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

        if (note != null) {
            Text(
                text = "${note.name}${note.octave}",
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                color = noteColor
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(72.dp))
        }

        if (note == null) {
            Text(
                text = stringResource(R.string.tuner_listening),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Text(
                text = "%.1f Hz".format(note.frequency),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = centsLabel(note.cents),
                style = MaterialTheme.typography.bodyMedium,
                color = noteColor.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun CentsMeter(cents: Int?, modifier: Modifier = Modifier) {
    val animatedCents by animateFloatAsState(
        targetValue = cents?.toFloat() ?: 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "cents"
    )
    val needleColor = if (cents != null) centsColor(cents) else Color.Unspecified
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val centerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Canvas(modifier = modifier) {
        val width = size.width
        val centerY = size.height / 2f

        drawLine(
            color = trackColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        for (mark in listOf(-50, -25, 0, 25, 50)) {
            val x = width * (mark + 50) / 100f
            val tickHalfHeight = if (mark == 0) 10.dp.toPx() else 6.dp.toPx()
            drawLine(
                color = if (mark == 0) centerColor else trackColor,
                start = Offset(x, centerY - tickHalfHeight),
                end = Offset(x, centerY + tickHalfHeight),
                strokeWidth = if (mark == 0) 2.dp.toPx() else 1.5.dp.toPx()
            )
        }

        if (cents != null) {
            val needleX = width * (animatedCents + 50) / 100f
            drawLine(
                color = needleColor,
                start = Offset(needleX, centerY - 18.dp.toPx()),
                end = Offset(needleX, centerY + 18.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun centsColor(cents: Int): Color = when {
    abs(cents) < 5 -> MaterialTheme.colorScheme.primary
    abs(cents) < 20 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}

private fun centsLabel(cents: Int): String = when {
    cents > 0 -> "+${cents}¢"
    cents < 0 -> "${cents}¢"
    else -> "0¢"
}

private class TunerScreenViewModelProvider : PreviewParameterProvider<ITunerViewModel> {
    override val values: Sequence<ITunerViewModel> = sequenceOf(
        ComposeTunerViewModel(TunerState.Idle),
        ComposeTunerViewModel(TunerState.Listening(null)),
        ComposeTunerViewModel(
            TunerState.Listening(
                DetectedNote(name = "A", octave = 4, frequency = 440.0f, cents = 0)
            )
        ),
        ComposeTunerViewModel(
            TunerState.Listening(
                DetectedNote(name = "G#", octave = 4, frequency = 415.3f, cents = 15)
            )
        ),
        ComposeTunerViewModel(
            TunerState.Listening(
                DetectedNote(name = "C", octave = 5, frequency = 523.3f, cents = -42)
            )
        ),
    )

    override fun getDisplayName(index: Int): String? = when (index) {
        0 -> "Idle"
        1 -> "Listening - No Note"
        2 -> "Listening - In Tune"
        3 -> "Listening - Slightly Sharp"
        4 -> "Listening - Significantly Flat"
        else -> null
    }
}
