package org.zotero.android.screens.allitems.processor

import org.zotero.android.architecture.LCE2
import org.zotero.android.database.objects.Attachment
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.screens.allitems.data.ItemsError
import org.zotero.android.screens.allitems.data.ItemsFilter
import org.zotero.android.screens.itemdetails.data.DetailType
import org.zotero.android.sync.Collection
import org.zotero.android.sync.Library

interface AllItemsProcessorInterface {

    fun currentLibrary(): Library
    fun currentCollection(): Collection
    fun currentSearchTerm(): String?
    fun currentFilters(): List<ItemsFilter>

    fun show(attachment: Attachment, library: Library)
    fun triggerScreenRefresh()
    fun updateTagFilter()
    fun isEditing(): Boolean
    fun getSelectedItems(): Set<String>
    fun setSelectedItems(newItems: Set<String>)
    fun showItemDetailWithDelay(creation: DetailType.creation)
    fun updateItemCellModels(itemCellModels: List<ItemCellModel>)

    fun updateLCE(lce: LCE2)
    fun showError(error: ItemsError)
}