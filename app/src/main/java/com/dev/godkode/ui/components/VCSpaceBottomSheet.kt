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

package com.dev.godkode.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dev.godkode.ui.theme.GodKodeTheme

@Composable
fun GodKodeBottomSheet(
    modifier: Modifier = Modifier
) {
    GodKodeTheme {
        Box(modifier) {
            Text(text = "GodKodeBottomSheet")
        }
    }
}

@Preview(name = "GodKodeBottomSheet")
@Composable
private fun PreviewGodKodeBottomSheet() {
    GodKodeBottomSheet()
}