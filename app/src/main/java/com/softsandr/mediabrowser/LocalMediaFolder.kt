package com.softsandr.mediabrowser

import java.io.Serializable

data class LocalMediaFolder(var folderName: String, var folderFilePaths: Collection<String>, var isImages: Boolean)

data class LocalMedia(val path: String, val isImage: Boolean) : Serializable