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

package com.dev.godkode.plugins.impl

import com.blankj.utilcode.util.FileUtils
import com.dev.godkode.ui.screens.file.FileExplorerViewModel
import com.godkode.plugins.Workspace
import java.io.File

class WorkspaceImpl(
    private val fileExplorerViewModel: FileExplorerViewModel
) : Workspace {
    override fun getProjectFiles(): MutableList<File> {
        return fileExplorerViewModel.openedFolder.value?.asRawFile()?.listFiles()?.toMutableList()
            ?: mutableListOf()
    }

    override fun getRootDirectory(): File {
        return fileExplorerViewModel.openedFolder.value!!.asRawFile()!!
    }

    override fun createFile(path: String): Boolean {
        return FileUtils.createOrExistsFile(path)
    }
}
