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

package com.bobek.tuner.settings

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bobek.tuner.data.AppNightMode
import com.bobek.tuner.domain.DEFAULT_REFERENCE_PITCH
import jakarta.inject.Inject
import kotlinx.coroutines.flow.map

private const val TAG = "DataStoreSettingsRepository"

class DataStoreSettingsRepository @Inject constructor(
    private val preferencesDataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        val NIGHT_MODE_KEY = stringPreferencesKey(PreferenceConstants.NIGHT_MODE)
        val REFERENCE_PITCH_KEY = intPreferencesKey(PreferenceConstants.REFERENCE_PITCH)
        val SOLFEGE_NOTATION_KEY = booleanPreferencesKey(PreferenceConstants.SOLFEGE_NOTATION)
    }

    override fun getNightMode() = preferencesDataStore.data
        .map { it[NIGHT_MODE_KEY] ?: AppNightMode.FOLLOW_SYSTEM.preferenceValue }
        .map { AppNightMode.forPreferenceValue(it) }

    override suspend fun setNightMode(nightMode: AppNightMode) {
        preferencesDataStore.edit { it[NIGHT_MODE_KEY] = nightMode.preferenceValue }
        Log.d(TAG, "Persisted nightMode: ${nightMode.preferenceValue}")
    }

    override fun getReferencePitch() = preferencesDataStore.data
        .map { it[REFERENCE_PITCH_KEY] ?: DEFAULT_REFERENCE_PITCH }

    override suspend fun setReferencePitch(referencePitch: Int) {
        preferencesDataStore.edit { it[REFERENCE_PITCH_KEY] = referencePitch }
        Log.d(TAG, "Persisted referencePitch: $referencePitch")
    }

    override fun getSolfegeNotation() = preferencesDataStore.data
        .map { it[SOLFEGE_NOTATION_KEY] ?: false }

    override suspend fun setSolfegeNotation(solfegeNotation: Boolean) {
        preferencesDataStore.edit { it[SOLFEGE_NOTATION_KEY] = solfegeNotation }
        Log.d(TAG, "Persisted solfegeNotation: $solfegeNotation")
    }
}
