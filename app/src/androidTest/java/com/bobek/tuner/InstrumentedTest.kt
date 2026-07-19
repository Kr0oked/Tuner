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

package com.bobek.tuner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Test

@LargeTest
class InstrumentedTest : AbstractAndroidTest() {

    @Before
    fun setup() {
        composeTestRule.waitForIdle()
    }

    @Test
    fun initialState() {
        onTopBarTitle().assertIsDisplayed()
        onSettingsButton().assertIsDisplayed()
        onPermissionRationaleText().assertIsDisplayed()
        onGrantPermissionButton().assertIsDisplayed()
    }

    @Test
    fun navigatingToSettingsAndBackShowsTunerScreenAgain() {
        onSettingsButton().performClick()
        composeTestRule.waitForIdle()
        onTopBarTitle(R.string.settings).assertIsDisplayed()

        pressBack()
        composeTestRule.waitForIdle()
        onTopBarTitle().assertIsDisplayed()
    }

    @Test
    fun changingNightModeUpdatesSelectedTheme() {
        openSettings()

        onNightModeOption(R.string.night_mode_follow_system).assertIsDisplayed()

        selectNightMode(R.string.night_mode_yes)
        onNightModeOption(R.string.night_mode_yes).assertIsDisplayed()

        selectNightMode(R.string.night_mode_follow_system)
        onNightModeOption(R.string.night_mode_follow_system).assertIsDisplayed()
    }

    @Test
    fun navigatingToLicenseShowsGplLicenseTextAndBackReturnsToSettings() {
        openSettings()

        onLicenseListItem().performClick()
        composeTestRule.waitForIdle()

        onTopBarTitle(R.string.license_name).assertIsDisplayed()
        waitUntilTextExists("GNU GENERAL PUBLIC LICENSE")

        pressBack()
        composeTestRule.waitForIdle()
        onTopBarTitle(R.string.settings).assertIsDisplayed()
    }

    @Test
    fun navigatingToThirdPartyLicenseShowsApacheLicenseForMaterialSymbols() {
        openSettings()

        onThirdPartyLicensesListItem().performClick()
        composeTestRule.waitForIdle()
        onTopBarTitle(R.string.third_party_licenses).assertIsDisplayed()

        scrollToListItem("Material Symbols")
        onListItem("Material Symbols").performClick()
        composeTestRule.waitForIdle()

        onTopBarTitle("Material Symbols").assertIsDisplayed()
        waitUntilTextExists("Apache License")

        pressBack()
        composeTestRule.waitForIdle()
        onTopBarTitle(R.string.third_party_licenses).assertIsDisplayed()
    }
}
