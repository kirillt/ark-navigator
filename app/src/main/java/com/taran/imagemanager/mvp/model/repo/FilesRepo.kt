package com.taran.imagemanager.mvp.model.repo

import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.IFile
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.mvp.model.file.FileProvider
import com.taran.imagemanager.utils.checkInternalStorage
import java.io.File

class FilesRepo(val fileProvider: FileProvider) {

    fun getImagesInFolder(path: String): MutableList<Image> {
        val directory = File(path)
        val inputFiles = directory.listFiles()

        return filterImages(inputFiles).toMutableList()
    }

    fun getFilesInFolder(path: String): MutableList<IFile> {
        val directory = File(path)
        val inputFiles = directory.listFiles()


        val folders = filterFolders(inputFiles)
        val images = filterImages(inputFiles)

        val files = mutableListOf<IFile>()
        files.addAll(folders)
        files.addAll(images)

        return files
    }

    fun getExternalStorage(): String {
        return fileProvider.getExternalStorage()
    }

    fun getExtSdCards(): List<Folder> {
        return fileProvider.getExtSdCards().map { checkInternalStorage(it) }
    }

    fun getImagesFromGallery(): List<Image> {
        return fileProvider.getImagesFromGallery()
    }

    private fun filterImages(files: Array<File>?): List<Image> {
        return files?.filter { file ->
            val fp = file.absolutePath
            fp.endsWith(".jpg") || fp.endsWith(".png") || fp.endsWith(".jpeg")
        }?.map { file ->
            Image(name = file.name, path =  file.absolutePath)
        }?: listOf()
    }

    private fun filterFolders(files: Array<File>?): List<Folder> {
        return files?.filter { file ->
            file.isDirectory
        }?.map { file ->
            Folder(name = file.name, path =  file.absolutePath)
        }?: listOf()
    }
}