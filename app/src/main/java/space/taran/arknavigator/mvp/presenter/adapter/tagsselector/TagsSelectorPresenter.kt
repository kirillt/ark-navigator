package space.taran.arknavigator.mvp.presenter.adapter.tagsselector

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceKind
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.ui.resource.StringProvider
import space.taran.arknavigator.utils.Popularity
import space.taran.arknavigator.utils.TAGS_SELECTOR
import space.taran.arknavigator.utils.Tag
import java.nio.file.Path
import javax.inject.Inject
import kotlin.reflect.KSuspendFunction1

sealed class TagItem {
    data class DefaultTagItem(val tag: Tag) : TagItem()
    data class KindTagItem(val kind: ResourceKind) : TagItem()
}

class TagsSelectorPresenter(
    private val viewState: ResourcesView,
    private val prefix: Path?,
    private val scope: CoroutineScope,
    private val onSelectionChangeListener: KSuspendFunction1<Set<ResourceId>, Unit>
) {
    @Inject
    lateinit var stringProvider: StringProvider

    private var index: ResourcesIndex? = null
    private var storage: TagsStorage? = null
    private val actions = ArrayDeque<TagsSelectorAction>()
    private var filter = ""
    private val kindToString: Map<ResourceKind, String> by lazy {
        mapOf(
            ResourceKind.IMAGE to stringProvider.getString(R.string.kind_image),
            ResourceKind.DOCUMENT
                to stringProvider.getString(R.string.kind_document),
            ResourceKind.VIDEO to stringProvider.getString(R.string.kind_video)
        )
    }

    var filterEnabled = false

    var showKindTags = false

    var includedTagItems = mutableSetOf<TagItem>()
        private set
    var excludedTagItems = mutableSetOf<TagItem>()
        private set
    private var availableTagItems = listOf<TagItem>()
    private var unavailableTagItems = listOf<TagItem>()

    var selection = setOf<ResourceId>()
        private set

    // this data is used by TagsSelectorAdapter
    var includedAndExcludedTagsForDisplay = listOf<TagItem>()
        private set
    var availableTagsForDisplay = listOf<TagItem>()
        private set
    var unavailableTagsForDisplay = listOf<TagItem>()
        private set
    var isClearBtnVisible = false
        private set

    fun init(index: ResourcesIndex, storage: TagsStorage) {
        this.index = index
        this.storage = storage
    }

    fun onTagItemClick(item: TagItem) = scope.launch {
        when {
            excludedTagItems.contains(item) -> {
                actions.addLast(UncheckExcluded(item))
                uncheckTag(item)
            }
            includedTagItems.contains(item) -> {
                actions.addLast(UncheckIncluded(item))
                uncheckTag(item)
            }
            else -> {
                actions.addLast(Include(item))
                if (filterEnabled) resetFilter()
                includeTag(item)
            }
        }
    }

    fun onTagItemLongClick(item: TagItem) = scope.launch {
        when {
            includedTagItems.contains(item) -> {
                actions.addLast(UncheckAndExclude(item))
                excludeTag(item)
            }
            !excludedTagItems.contains(item) -> {
                actions.addLast(Exclude(item))
                if (filterEnabled) resetFilter()
                excludeTag(item)
            }
            else -> {
                actions.addLast(UncheckExcluded(item))
                uncheckTag(item)
            }
        }
    }

    fun onTagExternallySelect(tag: Tag) = scope.launch {
        includeTag(TagItem.DefaultTagItem(tag))
    }

    fun onClearClick() = scope.launch {
        actions.addLast(Clear(includedTagItems.toSet(), excludedTagItems.toSet()))
        includedTagItems.clear()
        excludedTagItems.clear()
        calculateTagsAndSelection()
    }

    fun onFilterChanged(filter: String) {
        this.filter = filter
        filterTags()
        viewState.drawTags()
    }

    fun onFilterToggle(enabled: Boolean) {
        if (filterEnabled != enabled) {
            viewState.setTagsFilterEnabled(enabled)
            filterEnabled = enabled
            if (enabled) {
                viewState.setTagsFilterText(filter)
                filterTags()
                viewState.drawTags()
            } else {
                resetTags()
                viewState.drawTags()
            }
        }
    }

    suspend fun calculateTagsAndSelection() {
        Log.d(TAGS_SELECTOR, "calculating tags and selection")
        if (storage == null || index == null)
            return

        val tagItemsByResources = provideTagItemsByResources()
        val allItemsTags = tagItemsByResources.values.flatten().toSet()

        // some tags could have been removed from storage
        excludedTagItems = excludedTagItems.intersect(allItemsTags).toMutableSet()
        includedTagItems = includedTagItems.intersect(allItemsTags).toMutableSet()

        val selectionAndComplementWithTags = tagItemsByResources
            .toList()
            .groupBy { (_, tags) ->
                tags.containsAll(includedTagItems) &&
                    !excludedTagItems.any { tags.contains(it) }
            }

        val selectionWithTags = (
            selectionAndComplementWithTags[true] ?: emptyList()
            ).toMap()
        val complementWithTags = (
            selectionAndComplementWithTags[false] ?: emptyList()
            ).toMap()

        selection = selectionWithTags.keys
        val tagsOfSelectedResources = selectionWithTags.values.flatten()
        val tagsOfUnselectedResources = complementWithTags.values.flatten()

        availableTagItems = (
            tagsOfSelectedResources.toSet() - includedTagItems - excludedTagItems
            ).toList()
        unavailableTagItems = (
            allItemsTags - availableTagItems - includedTagItems - excludedTagItems
            ).toList()

        val tagsOfSelectedResPopularity = Popularity
            .calculate(tagsOfSelectedResources)

        val tagsOfUnselectedResPopularity = Popularity
            .calculate(tagsOfUnselectedResources)

        val tagsPopularity = Popularity
            .calculate(tagItemsByResources.values.flatten())
        availableTagItems = availableTagItems.sortedByDescending {
            tagsOfSelectedResPopularity[it]
        }
        unavailableTagItems = unavailableTagItems.sortedByDescending {
            tagsOfUnselectedResPopularity[it]
        }

        includedAndExcludedTagsForDisplay = (includedTagItems + excludedTagItems)
            .sortedByDescending { tagsPopularity[it] }

        if (filterEnabled) filterTags()
        else resetTags()

        Log.d(TAGS_SELECTOR, "tags included: $includedTagItems")
        Log.d(TAGS_SELECTOR, "tags excluded: $excludedTagItems")
        Log.d(TAGS_SELECTOR, "tags available: $availableTagsForDisplay")
        Log.d(TAGS_SELECTOR, "tags unavailable: $unavailableTagsForDisplay")

        isClearBtnVisible = includedTagItems.isNotEmpty() ||
            excludedTagItems.isNotEmpty()

        if (allItemsTags.isEmpty())
            viewState.setTagsSelectorHintEnabled(true)
        else
            viewState.setTagsSelectorHintEnabled(false)

        viewState.drawTags()

        onSelectionChangeListener(selection)
    }

    suspend fun onBackClick(): Boolean {
        if (actions.isEmpty())
            return false

        val action = findLastActualAction() ?: return false

        when (action) {
            is Include -> {
                includedTagItems.remove(action.item)
            }
            is Exclude -> {
                excludedTagItems.remove(action.item)
            }
            is UncheckIncluded -> {
                includedTagItems.add(action.item!!)
            }
            is UncheckExcluded -> {
                excludedTagItems.add(action.item!!)
            }
            is UncheckAndExclude -> {
                excludedTagItems.remove(action.item)
                includedTagItems.add(action.item!!)
            }
            is Clear -> {
                includedTagItems = action.included.toMutableSet()
                excludedTagItems = action.excluded.toMutableSet()
            }
        }

        actions.removeLast()

        calculateTagsAndSelection()

        return true
    }

    private fun findLastActualAction(): TagsSelectorAction? {
        val tagItems = provideTagItemsByResources().values.flatten().toSet()

        while (actions.lastOrNull() != null) {
            val lastAction = actions.last()
            if (isActionActual(lastAction, tagItems)) {
                return lastAction
            } else
                actions.removeLast()
        }

        return null
    }

    private fun isActionActual(
        action: TagsSelectorAction,
        allTagItems: Set<TagItem>
    ): Boolean {
        if (action is Clear &&
            action.excluded.intersect(allTagItems).isEmpty() &&
            action.included.intersect(allTagItems).isEmpty()
        ) {
            return false
        }

        if (!allTagItems.contains(action.item))
            return false

        return true
    }

    private fun resetFilter() {
        filter = ""
        viewState.setTagsFilterText(filter)
    }

    private fun filterTags() {
        availableTagsForDisplay = availableTagItems
            .filter {
                when (it) {
                    is TagItem.DefaultTagItem -> {
                        it.tag.startsWith(filter, false)
                    }
                    is TagItem.KindTagItem -> {
                        kindToString[it.kind]!!.startsWith(filter, false)
                    }
                }
            }

        unavailableTagsForDisplay = unavailableTagItems
            .filter {
                when (it) {
                    is TagItem.DefaultTagItem -> {
                        it.tag.startsWith(filter, false)
                    }
                    is TagItem.KindTagItem -> {
                        kindToString[it.kind]!!.startsWith(filter, false)
                    }
                }
            }
    }

    private fun resetTags() {
        availableTagsForDisplay = availableTagItems
        unavailableTagsForDisplay = unavailableTagItems
    }

    private suspend fun includeTag(item: TagItem) {
        Log.d(TAGS_SELECTOR, "including tag $item")

        includedTagItems.add(item)
        excludedTagItems.remove(item)

        calculateTagsAndSelection()
    }

    private suspend fun excludeTag(item: TagItem) {
        Log.d(TAGS_SELECTOR, "excluding tag $item")

        excludedTagItems.add(item)
        includedTagItems.remove(item)

        calculateTagsAndSelection()
    }

    private suspend fun uncheckTag(item: TagItem, needToCalculate: Boolean = true) {
        Log.d(TAGS_SELECTOR, "un-checking tag $item")

        if (includedTagItems.contains(item) && excludedTagItems.contains(item)) {
            throw AssertionError("The tag is both included and excluded")
        }
        if (!includedTagItems.contains(item) && !excludedTagItems.contains(item)) {
            throw AssertionError("The tag is neither included nor excluded")
        }

        if (!includedTagItems.remove(item)) {
            excludedTagItems.remove(item)
        }

        if (needToCalculate)
            calculateTagsAndSelection()
    }

    private fun provideTagItemsByResources(): Map<ResourceId, Set<TagItem>> {
        val resources = index!!.listIds(prefix)
        val tagItemsByResources: Map<ResourceId, Set<TagItem>> =
            storage!!.groupTagsByResources(resources).map {
                it.key to it.value.map { tag -> TagItem.DefaultTagItem(tag) }.toSet()
            }.toMap()

        if (!showKindTags) return tagItemsByResources

        return tagItemsByResources.map { (id, tags) ->
            var listOfTags = tags
            val kind = index!!.getMeta(id).kind
            if (kind != null) {
                listOfTags =
                    listOfTags + TagItem.KindTagItem(kind)
            }
            id to listOfTags
        }.toMap()
    }

    fun setKindTagsSwitchState(kindTagsSwitchState: Boolean) = scope.launch {
        showKindTags = kindTagsSwitchState
        resetTags()
        calculateTagsAndSelection()
    }
}
