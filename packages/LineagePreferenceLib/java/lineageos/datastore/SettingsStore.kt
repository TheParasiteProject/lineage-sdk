/*
 * SPDX-FileCopyrightText: 2024 The Android Open Source Project
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package lineageos.datastore

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.util.Log
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import java.util.concurrent.ConcurrentHashMap

/** Base class of the Settings provider data stores. */
sealed class SettingsStore(protected val contentResolver: ContentResolver) :
    AbstractKeyedDataObservable<String>(), KeyValueStore {

    private val defaultValues = ConcurrentHashMap<String, Any>()

    private val contentObserver =
        object : ContentObserver(HandlerExecutor.main) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange, null)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                val key = uri?.lastPathSegment ?: return
                notifyChange(key, DataChangeReason.UPDATE)
            }
        }

    /** The URI to watch for any key change. */
    protected abstract val uri: Uri

    override fun onFirstObserverAdded() {
        Log.i(tag, "registerContentObserver")
        contentResolver.registerContentObserver(uri, true, contentObserver)
    }

    override fun onLastObserverRemoved() {
        Log.i(tag, "unregisterContentObserver")
        contentResolver.unregisterContentObserver(contentObserver)
    }

    /**
     * Sets default value for given key.
     *
     * The observers are not notified for this operation.
     */
    fun setDefaultValue(key: String, value: Any) {
        val oldValue = defaultValues.put(key, value)
        if (oldValue == null) {
            Log.d(tag, "setDefaultValue $key $value")
        } else if (oldValue != value) {
            Log.w(tag, "$key default value is changed from $oldValue to $value")
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) =
        defaultValues[key] as T?

    /** Tag for logging. */
    abstract val tag: String
}
