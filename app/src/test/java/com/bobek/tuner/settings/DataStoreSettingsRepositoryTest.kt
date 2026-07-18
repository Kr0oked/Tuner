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

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.bobek.tuner.data.AppNightMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun createRepository(): DataStoreSettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_preferences.preferences_pb") }
        )
        return DataStoreSettingsRepository(dataStore)
    }

    @Test
    fun nightModeDefaultIsFollowSystem() = testScope.runTest {
        val repo = createRepository()
        assertEquals(AppNightMode.FOLLOW_SYSTEM, repo.getNightMode().first())
    }

    @Test
    fun nightModeRoundTrip() = testScope.runTest {
        val repo = createRepository()
        repo.setNightMode(AppNightMode.YES)
        assertEquals(AppNightMode.YES, repo.getNightMode().first())
    }

    @Test
    fun allNightModesRoundTrip() = testScope.runTest {
        val repo = createRepository()
        for (nightMode in AppNightMode.entries) {
            repo.setNightMode(nightMode)
            assertEquals(nightMode, repo.getNightMode().first())
        }
    }
}
