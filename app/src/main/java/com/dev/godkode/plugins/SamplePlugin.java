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

package com.dev.godkode.plugins;

import android.widget.Toast;

import androidx.annotation.NonNull;

import com.godkode.plugins.Plugin;
import com.godkode.plugins.PluginContext;

public class SamplePlugin implements Plugin {
    @Override
    public void onPluginLoaded(@NonNull PluginContext context) {
        Toast.makeText(context.getAppContext(), "Hello from the plugin!", Toast.LENGTH_SHORT).show();
    }
}
