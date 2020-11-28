package com.taran.imagemanager.mvp.presenter

import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.IFile
import com.taran.imagemanager.mvp.model.entity.Icons
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.mvp.model.repo.FilesRepo
import com.taran.imagemanager.mvp.model.repo.RoomRepo
import com.taran.imagemanager.mvp.presenter.adapter.IFileGridPresenter
import com.taran.imagemanager.mvp.view.ExplorerView
import com.taran.imagemanager.mvp.view.item.FileItemView
import com.taran.imagemanager.navigation.Screens
import com.taran.imagemanager.utils.getHash
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import javax.inject.Inject


class ExplorerPresenter(var currentFolder: Folder) : MvpPresenter<ExplorerView>() {

    @Inject
    lateinit var filesRepo: FilesRepo

    @Inject
    lateinit var roomRepo: RoomRepo

    @Inject
    lateinit var router: Router

    val fileGridPresenter = FileGridPresenter()


    inner class FileGridPresenter :
        IFileGridPresenter {

        var files = mutableListOf<IFile>()

        override fun getCount() = files.size

        override fun bindView(view: FileItemView) {
            val file = files[view.pos]
            if (file is Folder) {
                view.setText(file.name)
                view.setIcon(Icons.FOLDER, null)
            }
            if (file is Image) {
                view.setText(file.name)
                view.setIcon(Icons.IMAGE, file.path)
            }
        }

        override fun onCardClicked(pos: Int) {
            val file = files[pos]
            if (file is Folder)
                router.navigateTo(Screens.ExplorerScreen(file))
            else {
                val images = files.filterIsInstance<Image>().toMutableList()
                router.navigateTo(Screens.DetailScreen(images, pos))
            }
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()

        if (currentFolder.path != "gallery") {
            val files = filesRepo.getFilesInFolder(currentFolder.path)
            files.sortBy { it.name }
            fileGridPresenter.files = files
            viewState.updateAdapter()
            checkCurrentFolder()
        } else {
            viewState.setFabVisibility(false)
            val images = filesRepo.getImagesFromGallery()
            fileGridPresenter.files = images.toMutableList()
            viewState.updateAdapter()
        }
    }

    fun fabClicked() {
        viewState.showDialog()
    }

    fun addFolderToFavorite() {
        currentFolder.favorite = true
        roomRepo.getFolderByPath(currentFolder.path).subscribe(
            {
                it.favorite = true
                roomRepo.insertFolder(it).subscribe()
            },
            {
                roomRepo.insertFolder(currentFolder).subscribe()
            }
        )
    }

    private fun checkCurrentFolder() {
        roomRepo.getFolderByPath(currentFolder.path).subscribe(
            {
                currentFolder = it
                if (!currentFolder.processed)
                    calculateHash()
            },
            {
                calculateHash()
            }
        )
    }

    private fun calculateHash() {
        val images = fileGridPresenter.files.filterIsInstance<Image>()
        processImages(images).subscribe(
            {
                currentFolder.processed = true
                roomRepo.getFolderByPath(currentFolder.path).subscribe(
                    {
                        it.processed = true
                        roomRepo.insertFolder(it).subscribe()
                    },
                    {
                        roomRepo.insertFolder(currentFolder).subscribe()
                    }
                )
            }, {}
        )
    }

    private fun processImages(images: List<Image>) = Completable
        .create { emitter ->
            images.forEach {
                it.hash = getHash(it.path)
                roomRepo.insertImageNonRx(it)
            }
            emitter.onComplete()
        }.subscribeOn(Schedulers.io())
}