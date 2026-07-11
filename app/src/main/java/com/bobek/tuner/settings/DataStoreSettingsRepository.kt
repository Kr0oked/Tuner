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
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bobek.tuner.data.AppNightMode
import jakarta.inject.Inject
import kotlinx.coroutines.flow.map

private const val TAG = "DataStoreSettingsRepository"

class DataStoreSettingsRepository @Inject constructor(
    private val preferencesDataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        val NIGHT_MODE_KEY = stringPreferencesKey(PreferenceConstants.NIGHT_MODE)
    }

    override fun getNightMode() = preferencesDataStore.data
        .map { it[NIGHT_MODE_KEY] ?: AppNightMode.FOLLOW_SYSTEM.preferenceValue }
        .map { AppNightMode.forPreferenceValue(it) }

    override suspend fun setNightMode(nightMode: AppNightMode) {
        preferencesDataStore.edit { it[NIGHT_MODE_KEY] = nightMode.preferenceValue }
        Log.d(TAG, "Persisted nightMode: ${nightMode.preferenceValue}")
    }
}
