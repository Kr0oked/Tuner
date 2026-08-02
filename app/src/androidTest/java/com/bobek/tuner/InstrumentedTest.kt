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

import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class InstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

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

        onThirdPartyLicensesListItem().performScrollTo().performClick()
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

    private fun openSettings() {
        composeTestRule.waitForIdle()
        onSettingsButton().performClick()
        composeTestRule.waitForIdle()
    }

    private fun selectNightMode(@StringRes labelResId: Int) {
        composeTestRule.waitForIdle()
        onNightModeListItem().performClick()
        composeTestRule.waitForIdle()
        onNightModeOption(labelResId).performClick()
        composeTestRule.waitForIdle()
    }

    private fun onTopBarTitle(@StringRes titleResId: Int = R.string.tuner): SemanticsNodeInteraction =
        onTopBarTitle(getString(titleResId))

    private fun onTopBarTitle(title: String): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(title)

    private fun onSettingsButton(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithContentDescription(getString(R.string.settings))

    private fun onPermissionRationaleText(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(getString(R.string.tuner_permission_rationale))

    private fun onGrantPermissionButton(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(getString(R.string.tuner_grant_permission))

    private fun onLicenseListItem(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(getString(R.string.license))

    private fun onThirdPartyLicensesListItem(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(getString(R.string.third_party_licenses))

    private fun onNightModeListItem(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(getString(R.string.night_mode))

    private fun onNightModeOption(@StringRes labelResId: Int): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(getString(labelResId))

    @Suppress("SameParameterValue")
    private fun onListItem(text: String): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(text)

    @Suppress("SameParameterValue")
    private fun scrollToListItem(text: String) {
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
    }

    private fun waitUntilTextExists(text: String, timeoutMillis: Long = 5_000) {
        composeTestRule.waitUntilAtLeastOneExists(hasText(text, substring = true), timeoutMillis = timeoutMillis)
    }

    private fun getString(@StringRes resId: Int): String = composeTestRule.activity.getString(resId)
}
