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

package com.dev.godkode.ui.screens.editor.components.drawer

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.dev.godkode.activities.Editor.LocalEditorDrawerNavController
import com.dev.godkode.activities.SettingsActivity
import com.dev.godkode.activities.TerminalActivity
import com.dev.godkode.app.drawables
import com.dev.godkode.extensions.open
import com.dev.godkode.resources.R
import com.dev.godkode.ui.navigateSingleTop
import com.dev.godkode.ui.screens.EditorDrawerScreens

@Composable
fun NavRail(
    modifier: Modifier = Modifier,
    selectedItemIndex: Int
) {
    val navigationRailItems = listOf(
        stringResource(R.string.files),
        stringResource(R.string.git),
        stringResource(R.string.ai_chat),
        stringResource(R.string.terminal),
        stringResource(R.string.settings)
    )
    val navRailItemIconsUnselected = listOf(
        Icons.Outlined.Folder,
        ImageVector.vectorResource(drawables.ic_git),
        Icons.AutoMirrored.Outlined.Chat,
        Icons.Outlined.Terminal,
        Icons.Outlined.Settings
    )
    val navRailItemIconsSelected = listOf(
        Icons.Rounded.Folder,
        ImageVector.vectorResource(drawables.ic_git),
        Icons.AutoMirrored.Rounded.Chat,
        Icons.Rounded.Terminal,
        Icons.Rounded.Settings
    )

    val context = LocalContext.current
    val navController = LocalEditorDrawerNavController.current

    NavigationRail(
        modifier = modifier.widthIn(max = 60.dp)
    ) {
        navigationRailItems.fastForEachIndexed { index, name ->
            NavigationRailItem(
                icon = {
                    Icon(
                        imageVector = if (selectedItemIndex == index) navRailItemIconsSelected[index] else navRailItemIconsUnselected[index],
                        contentDescription = name,
                        modifier = Modifier.size(20.dp),
                    )
                },
                label = {
                    Text(
                        text = name,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                alwaysShowLabel = true,
                selected = selectedItemIndex == index,
                onClick = {
                    when (index) {
                        0 -> navController.navigateSingleTop(EditorDrawerScreens.FileExplorer)
                        1 -> navController.navigateSingleTop(EditorDrawerScreens.GitManager)
                        2 -> navController.navigateSingleTop(EditorDrawerScreens.AiChat)
                        3 -> context.open(TerminalActivity::class.java)
                        4 -> context.open(SettingsActivity::class.java)
                    }
                }
            )
        }
    }
}