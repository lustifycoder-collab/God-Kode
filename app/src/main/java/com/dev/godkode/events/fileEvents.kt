package com.dev.godkode.events

import com.dev.godkode.app.Folder
import com.dev.godkode.file.File

data class OnDeleteFileEvent(val file: File, val openedFolder: File)

data class OnCreateFileEvent(val file: File, val openedFolder: File)

data class OnCreateFolderEvent(val file: File, val openedFolder: File)

data class OnRefreshFolderEvent(val openedFolder: File)

data class OnRenameFileEvent(val oldFile: File, val newFile: File, val openedFolder: File)

data class OnOpenFolderEvent(val folder: Folder)
