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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Label
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bobek.tuner.R
import com.bobek.tuner.domain.DEFAULT_REFERENCE_PITCH
import com.bobek.tuner.domain.MAX_REFERENCE_PITCH
import com.bobek.tuner.domain.MIN_REFERENCE_PITCH
import kotlinx.coroutines.flow.drop
import kotlin.math.roundToInt

private const val REFERENCE_PITCH_STEPS = MAX_REFERENCE_PITCH - MIN_REFERENCE_PITCH - 1

@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun ReferencePitchDialog(
    viewModel: ITunerViewModel = ComposeTunerViewModel(),
    onDismiss: () -> Unit = {}
) {
    val referencePitch by viewModel.getReferencePitchFlow().collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val sliderState = remember {
        SliderState(
            value = referencePitch.toFloat(),
            steps = REFERENCE_PITCH_STEPS,
            valueRange = MIN_REFERENCE_PITCH.toFloat()..MAX_REFERENCE_PITCH.toFloat()
        )
    }

    LaunchedEffect(referencePitch) {
        sliderState.value = referencePitch.toFloat()
    }

    LaunchedEffect(sliderState) {
        snapshotFlow { sliderState.value }
            .drop(1)
            .collect { viewModel.setReferencePitch(it.roundToInt()) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reference_pitch)) },
        text = {
            Column {
                Slider(
                    state = sliderState,
                    interactionSource = interactionSource,
                    thumb = { state ->
                        Label(
                            label = {
                                PlainTooltip { Text(state.value.roundToInt().toString()) }
                            },
                            interactionSource = interactionSource
                        ) {
                            SliderDefaults.Thumb(interactionSource = interactionSource)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                )
                Text(
                    text = stringResource(R.string.reference_pitch_value, referencePitch),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.setReferencePitch(DEFAULT_REFERENCE_PITCH) }) {
                Text(stringResource(R.string.reset_to_default))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}
