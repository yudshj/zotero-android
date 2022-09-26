package org.zotero.android.architecture.database.requests

import io.realm.Realm
import io.realm.RealmResults
import org.zotero.android.architecture.database.DbResponseRequest
import org.zotero.android.architecture.database.objects.RTypedTag
import kotlin.reflect.KClass

open class ReadBaseTagsToDeleteDbRequest<T : RTypedTag>(val fromTags: RealmResults<RTypedTag>) :
    DbResponseRequest<T, List<String>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm, clazz: KClass<T>?): List<String> {
        return this.fromTags.where().baseTagsToDelete().findAll().mapNotNull { it.tag?.name }
    }


}