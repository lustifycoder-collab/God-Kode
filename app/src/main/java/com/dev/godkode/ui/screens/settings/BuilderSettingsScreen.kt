/*
 * This file is part of GodKode.
 *
 * GodKode is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * GodKode is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with GodKode.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.dev.godkode.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dev.godkode.core.settings.Settings.Builder.GRADLE_ARGS
import com.dev.godkode.core.settings.Settings.Builder.JAVA_HOME
import com.dev.godkode.core.settings.Settings.Builder.ANDROID_HOME
import com.dev.godkode.core.settings.Settings.Builder.NDK_HOME
import com.dev.godkode.core.settings.Settings.Builder.TASK
import com.dev.godkode.core.settings.Settings.Builder.rememberAndroidHome
import com.dev.godkode.core.settings.Settings.Builder.rememberGradleArgs
import com.dev.godkode.core.settings.Settings.Builder.rememberJavaHome
import com.dev.godkode.core.settings.Settings.Builder.rememberNdkHome
import com.dev.godkode.core.settings.Settings.Builder.rememberTask
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.textFieldPreference

@Composable
fun BuilderSettingsScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val javaHome = rememberJavaHome()
    val androidHome = rememberAndroidHome()
    val ndkHome = rememberNdkHome()
    val task = rememberTask()
    val gradleArgs = rememberGradleArgs()

    BackHandler(onBack = onNavigateUp)

    val backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        preferenceCategory(
            key = "builder_category_toolchain",
            title = {
                Text(
                    "GodKode does not bundle the toolchain. Point it at an " +
                        "existing JDK/SDK (e.g. from Termux or AndroidIDE)."
                )
            }
        )

        textFieldPreference(
            key = JAVA_HOME.name,
            title = { Text("JAVA_HOME") },
            summary = { Text(it.ifBlank { "e.g. /data/data/com.termux/files/usr/lib/jvm-17" }) },
            rememberState = { javaHome },
            defaultValue = javaHome.value,
            textToValue = { it },
            icon = { Icon(Icons.Default.Code, contentDescription = null) },
            modifier = Modifier
                .clip(PreferenceShape.Top)
                .background(backgroundColor)
        )

        textFieldPreference(
            key = ANDROID_HOME.name,
            title = { Text("ANDROID_HOME (SDK)") },
            summary = { Text(it.ifBlank { "Android SDK root (has platform-tools/build-tools)" }) },
            rememberState = { androidHome },
            defaultValue = androidHome.value,
            textToValue = { it },
            icon = { Icon(Icons.Default.Build, contentDescription = null) },
            modifier = Modifier
                .clip(PreferenceShape.Middle)
                .background(backgroundColor)
        )

        textFieldPreference(
            key = NDK_HOME.name,
            title = { Text("NDK_HOME (optional)") },
            summary = { Text(it.ifBlank { "Leave blank for pure-Kotlin projects" }) },
            rememberState = { ndkHome },
            defaultValue = ndkHome.value,
            textToValue = { it },
            icon = { Icon(Icons.Default.Build, contentDescription = null) },
            modifier = Modifier
                .clip(PreferenceShape.Middle)
                .background(backgroundColor)
        )

        preferenceCategory(
            key = "builder_category_build",
            title = { Text("Build") }
        )

        textFieldPreference(
            key = TASK.name,
            title = { Text("Gradle task") },
            summary = { Text(it) },
            rememberState = { task },
            defaultValue = task.value,
            textToValue = { it },
            icon = { Icon(Icons.Default.PlayCircle, contentDescription = null) },
            modifier = Modifier
                .clip(PreferenceShape.Top)
                .background(backgroundColor)
        )

        textFieldPreference(
            key = GRADLE_ARGS.name,
            title = { Text("Extra gradle args") },
            summary = { Text(it) },
            rememberState = { gradleArgs },
            defaultValue = gradleArgs.value,
            textToValue = { it },
            icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
            modifier = Modifier
                .clip(PreferenceShape.Bottom)
                .background(backgroundColor)
        )
    }
}
