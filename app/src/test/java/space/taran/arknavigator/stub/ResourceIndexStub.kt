package space.taran.arknavigator.stub

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.ResourceUpdates
import space.taran.arklib.domain.index.RootIndex

import java.nio.file.Path
import kotlin.io.path.Path

class ResourceIndexStub : ResourceIndex {
    private val resources = TestData.resourceById().toMutableMap()

    override val roots: Set<RootIndex> = setOf()

    override val updates: Flow<ResourceUpdates> =
        MutableSharedFlow()

    override suspend fun updateAll() {}

    override suspend fun allResources(): Map<ResourceId, Resource> =
        resources.toMap()

    override suspend fun getResource(id: ResourceId): Resource? =
        resources[id]

    override suspend fun allPaths(): Map<ResourceId, Path> =
        resources.mapValues { Path("") }

    override suspend fun getPath(id: ResourceId): Path? =
        Path("")
}
