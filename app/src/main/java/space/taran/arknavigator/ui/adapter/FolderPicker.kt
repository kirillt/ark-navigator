package space.taran.arknavigator.ui.adapter

import space.taran.arknavigator.databinding.DialogRootsNewBinding
import space.taran.arknavigator.mvp.presenter.adapter.FoldersWalker
import space.taran.arknavigator.mvp.presenter.adapter.ItemClickHandler
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.listDirectoryEntries

@OptIn(ExperimentalPathApi::class)
class FolderPicker(
    paths: List<Path>,
    handler: ItemClickHandler<Path>,
    private val dialogBinding: DialogRootsNewBinding)
    : FilesReversibleRVAdapter<Path, Path>(FoldersWalker(paths, handler)) {

    init {
        dialogBinding.rvRootsDialog.adapter = this
        dialogBinding.tvRootsDialogPath.text = super.getLabel().toString()
    }

    var currentLabel: Path? = null

    override fun backClicked(): Path? {
        currentLabel = super.backClicked()
        if (currentLabel != null) {
            dialogBinding.tvRootsDialogPath.text = currentLabel.toString()
        }
        return currentLabel
    }

    fun updatePath(path: Path) {
        val children = path.listDirectoryEntries().sorted()
        this.updateItems(path, children)

        dialogBinding.tvRootsDialogPath.text = path.toString()
    }
}