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
package com.dev.godkode.plugins

import android.content.Context
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ToastUtils
import com.dev.godkode.PluginConstants
import com.dev.godkode.extensions.extractZipFile
import com.dev.godkode.extensions.toFile
import com.dev.godkode.plugins.internal.PluginInfo
import com.dev.godkode.utils.runOnUiThread
import com.dev.godkode.utils.showShortToast
import com.godkode.plugins.Plugin
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PluginLoader {
    fun loadPlugins(context: Context): List<Pair<PluginInfo, Plugin>> {
        val plugins = mutableListOf<Plugin>()
        val pluginInfos = mutableListOf<PluginInfo>()

        val pluginsPath = PluginConstants.PLUGIN_HOME_PATH.toFile()
        FileUtils.createOrExistsDir(pluginsPath)

        pluginsPath.listFiles()?.forEach { file ->
            val properties = file.resolve("plugin.properties")
            if (!properties.exists()) {
                throw IllegalArgumentException("Plugin directory ${file.name} does not contain plugin.properties")
            }

            val pluginInfo = PluginInfo(properties)
            pluginInfo.pluginFileName?.let {
                val jarFilePath = file.resolve(it).apply {
                    setWritable(false)
                    setReadable(true, true)
                }

                val dexClassLoader = DexClassLoader(
                    jarFilePath.absolutePath,
                    null,
                    null,
                    context.applicationContext.classLoader
                )

                val pluginClass = dexClassLoader.loadClass(pluginInfo.mainClass)

                if (Plugin::class.java.isAssignableFrom(pluginClass)) {
                    val constructor = pluginClass.getConstructor()
                    plugins.add(constructor.newInstance() as Plugin)
                    pluginInfos.add(pluginInfo)
                } else {
                    throw IllegalArgumentException("Class does not implement Plugin interface")
                }
            } ?: runOnUiThread {
                showShortToast(context, "Plugin file not found for ${file.name}")
            }
        }

        return pluginInfos.zip(plugins)
    }

    suspend fun extractPluginZip(pluginZipFile: File): File {
        return withContext(Dispatchers.IO) {
            val path = "${PluginConstants.PLUGIN_HOME_PATH}/${pluginZipFile.nameWithoutExtension}"
            val internalFile = path.toFile()
            runCatching {
                FileUtils.createOrExistsDir(internalFile)
                pluginZipFile.extractZipFile(internalFile)
            }.onFailure {
                ToastUtils.showShort(it.message)
            }

            val properties = internalFile.resolve("plugin.properties")
            if (!properties.exists()) {
                throw IllegalArgumentException("Plugin directory ${internalFile.name} does not contain plugin.properties")
            }

            val pluginInfo = PluginInfo(properties)
            internalFile.apply {
                if (pluginInfo.name.isNullOrBlank()) {
                    throw NullPointerException("Plugin name is empty.")
                }
                FileUtils.rename(this, pluginInfo.name)
            }
        }
    }
}
