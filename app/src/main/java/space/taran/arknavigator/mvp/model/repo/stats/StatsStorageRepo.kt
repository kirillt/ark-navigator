package space.taran.arknavigator.mvp.model.repo.stats

import space.taran.arknavigator.mvp.model.repo.FoldersRepo
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndexRepo
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorageRepo
import java.nio.file.Path
import javax.inject.Inject

class StatsStorageRepo @Inject constructor(
    private val foldersRepo: FoldersRepo,
    private val indexRepo: ResourcesIndexRepo,
    private val tagsStorageRepo: TagsStorageRepo
) {
    private val storageByRoot = mutableMapOf<Path, StatsStorage>()

    suspend fun provide(rootAndFav: RootAndFav): StatsStorage {
        val roots = foldersRepo.resolveRoots(rootAndFav)

        val shards = roots.map { root ->
            storageByRoot[root]?.let { return@map it }

            val storage = PlainStatsStorage(
                root,
                indexRepo.provide(root),
                tagsStorageRepo.provide(root),
                tagsStorageRepo.statsFlow
            )
            storageByRoot[root] = storage
            storage.init()
            storage
        }

        return AggregatedStatsStorage(shards)
    }

    suspend fun provide(root: Path): StatsStorage =
        provide(RootAndFav(root.toString(), null))
}
