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

package com.bobek.tuner.ui

import android.content.res.Resources
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bobek.tuner.R
import com.bobek.tuner.data.AppNightMode
import com.bobek.tuner.ui.licenses.ThirdPartyLicenseScreen
import com.bobek.tuner.ui.licenses.ThirdPartyLicenseScreenState
import com.bobek.tuner.ui.licenses.ThirdPartyLicensesScreen
import com.bobek.tuner.ui.settings.SettingsScreen
import com.bobek.tuner.ui.theme.AppTheme
import com.bobek.tuner.ui.tuner.ComposeTunerViewModel
import com.bobek.tuner.ui.tuner.ITunerViewModel
import com.bobek.tuner.ui.tuner.TunerScreen
import de.philipp_bobek.oss_licenses_parser.OssLicensesParser
import de.philipp_bobek.oss_licenses_parser.ThirdPartyLicenseMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val MANUAL_LICENSE_RESOURCES = mapOf(
    "Material Symbols" to R.raw.license_apache_2_0
)

@Composable
@PreviewScreenSizes
fun MainContent(
    appViewModel: IAppViewModel = ComposeAppViewModel(),
    tunerViewModel: ITunerViewModel = ComposeTunerViewModel()
) {
    val navController = rememberNavController()
    val nightMode by appViewModel.getNightModeFlow().collectAsState()

    val isDarkTheme = when (nightMode) {
        AppNightMode.NO -> false
        AppNightMode.YES -> true
        AppNightMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }

    AppTheme(darkTheme = isDarkTheme) {
        NavHost(navController = navController, startDestination = "tuner") {
            composable("tuner") {
                TunerScreen(
                    viewModel = tunerViewModel,
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = appViewModel,
                    onBackClick = { navController.popBackStack() },
                    onThirdPartyLicensesClick = { navController.navigate("licenses") }
                )
            }
            composable("licenses") {
                val resources = LocalResources.current
                val libraryNames by produceState(initialValue = emptyList()) {
                    value = withContext(Dispatchers.IO) {
                        getLibraryNames(resources)
                    }
                }

                ThirdPartyLicensesScreen(
                    libraryNames = libraryNames,
                    onBackClick = { navController.popBackStack() },
                    onLibraryClick = { libraryName ->
                        navController.navigate("license/${Uri.encode(libraryName)}")
                    }
                )
            }
            composable("license/{libraryName}") { backStackEntry ->
                val libraryName = Uri.decode(backStackEntry.arguments?.getString("libraryName") ?: "")
                val resources = LocalResources.current
                val licenseContent by produceState(initialValue = "", key1 = libraryName) {
                    value = withContext(Dispatchers.IO) {
                        getLicenseContent(resources, libraryName)
                    }
                }

                ThirdPartyLicenseScreen(
                    state = ThirdPartyLicenseScreenState(
                        libraryName = libraryName,
                        licenseContent = licenseContent,
                    ),
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun getLibraryNames(resources: Resources): List<String> {
    val ossLicenseNames = resources
        .openRawResource(R.raw.third_party_license_metadata)
        .use(OssLicensesParser::parseMetadata)
        .map { it.libraryName }

    return (ossLicenseNames + MANUAL_LICENSE_RESOURCES.keys).sorted()
}

private fun getLicenseContent(resources: Resources, libraryName: String): String {
    val manualResourceId = MANUAL_LICENSE_RESOURCES[libraryName]

    return if (manualResourceId != null) {
        resources.openRawResource(manualResourceId).bufferedReader().readText()
    } else {
        resources
            .openRawResource(R.raw.third_party_license_metadata)
            .use(OssLicensesParser::parseMetadata)
            .find { it.libraryName == libraryName }
            ?.let { getLicenseContent(resources, it) }
            ?: ""
    }
}

private fun getLicenseContent(resources: Resources, metadata: ThirdPartyLicenseMetadata): String =
    resources
        .openRawResource(R.raw.third_party_licenses)
        .use { OssLicensesParser.parseLicense(metadata, it) }
        .licenseContent
