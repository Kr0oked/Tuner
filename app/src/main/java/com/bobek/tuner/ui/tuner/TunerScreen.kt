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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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

            BoxWithConstraints(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                val isLandscape = maxWidth > maxHeight

                if (isLandscape) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            NoteContent(
                                state = state,
                                onGrantPermission = { permissionLauncher.launch(RECORD_AUDIO) },
                                compact = true
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            MeterContent(cents)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            NoteContent(
                                state = state,
                                onGrantPermission = { permissionLauncher.launch(RECORD_AUDIO) },
                                compact = false
                            )
                        }
                        MeterContent(cents)
                    }
                }
            }
        }
    )
}

@Composable
private fun NoteContent(
    state: TunerState,
    onGrantPermission: () -> Unit,
    compact: Boolean
) {
    when (state) {
        is TunerState.Idle -> IdleContent(onGrantPermission = onGrantPermission, compact = compact)
        is TunerState.Listening -> ListeningContent(note = state.note, compact = compact)
    }
}

@Composable
private fun MeterContent(cents: Int?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        CentsBarMeter(
            cents = cents,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
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

@Composable
private fun IdleContent(
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Column(
        modifier = modifier.padding(if (compact) 12.dp else 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.tuner_permission_rationale),
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = if (compact) 12.dp else 24.dp)
        )
        Button(onClick = onGrantPermission) {
            Text(stringResource(R.string.tuner_grant_permission))
        }
    }
}

@Composable
private fun ListeningContent(
    note: DetectedNote?,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Column(
        modifier = modifier.padding(if (compact) 8.dp else 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val noteColor = note?.let { centsColor(it.cents) }
            ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

        if (note != null) {
            Text(
                text = "${note.name}${note.octave}",
                fontSize = if (compact) 56.sp else 96.sp,
                fontWeight = FontWeight.Bold,
                color = noteColor
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(if (compact) 48.dp else 72.dp))
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

private const val MAX_CENTS = 50f
private const val TOTAL_BARS = 17
private const val MIN_HEIGHT_FRACTION = 0.28f
private const val MIN_WIDTH_FRACTION = 0.5f

@Composable
private fun CentsBarMeter(cents: Int?, modifier: Modifier = Modifier) {
    val animatedCents by animateFloatAsState(
        targetValue = cents?.toFloat() ?: 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "cents"
    )
    val hasNote = cents != null

    val inTuneColor = MaterialTheme.colorScheme.primary
    val warnColor = MaterialTheme.colorScheme.tertiary
    val outOfTuneColor = MaterialTheme.colorScheme.error
    val dimAlpha = 0.22f

    Canvas(modifier = modifier) {
        val centerIndex = TOTAL_BARS / 2
        val gap = 3.dp.toPx()
        val centerY = size.height / 2f
        val activePosition = (animatedCents + MAX_CENTS) / (2 * MAX_CENTS) * (TOTAL_BARS - 1)

        val widthFractions = FloatArray(TOTAL_BARS) { i ->
            val positionFraction = abs(i - centerIndex) / centerIndex.toFloat()
            1f - (1f - MIN_WIDTH_FRACTION) * positionFraction
        }
        val baseBarWidth = (size.width - gap * (TOTAL_BARS - 1)) / widthFractions.sum()

        var x = 0f
        for (i in 0 until TOTAL_BARS) {
            val positionFraction = abs(i - centerIndex) / centerIndex.toFloat()
            val barWidth = baseBarWidth * widthFractions[i]
            val barHeight = size.height * (1f - (1f - MIN_HEIGHT_FRACTION) * positionFraction)
            val zoneColor = when {
                positionFraction <= 0.25f -> inTuneColor
                positionFraction <= 0.65f -> warnColor
                else -> outOfTuneColor
            }
            val litFraction = if (hasNote) (1f - abs(i - activePosition) / 1.2f).coerceIn(0f, 1f) else 0f

            drawGlowingBar(x, centerY, barWidth, barHeight, zoneColor, dimAlpha, litFraction)

            x += barWidth + gap
        }
    }
}

private fun DrawScope.drawGlowingBar(
    x: Float,
    centerY: Float,
    barWidth: Float,
    barHeight: Float,
    zoneColor: Color,
    dimAlpha: Float,
    litFraction: Float
) {
    val alpha = dimAlpha + (1f - dimAlpha) * litFraction
    val cornerRadius = CornerRadius(barWidth / 2.5f)

    if (litFraction > 0.05f) {
        val glowScale = 1f + litFraction * 0.5f
        val glowWidth = barWidth * glowScale
        val glowHeight = barHeight * glowScale
        drawRoundRect(
            color = zoneColor.copy(alpha = litFraction * 0.35f),
            topLeft = Offset(x - (glowWidth - barWidth) / 2f, centerY - glowHeight / 2f),
            size = Size(glowWidth, glowHeight),
            cornerRadius = cornerRadius
        )
    }

    drawRoundRect(
        color = zoneColor.copy(alpha = alpha),
        topLeft = Offset(x, centerY - barHeight / 2f),
        size = Size(barWidth, barHeight),
        cornerRadius = cornerRadius
    )
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
