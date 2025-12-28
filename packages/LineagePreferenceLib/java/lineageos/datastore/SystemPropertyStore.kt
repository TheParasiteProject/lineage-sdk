/*
 * SPDX-FileCopyrightText: 2024 The Android Open Source Project
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */

package lineageos.datastore

import android.os.SystemProperties
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.Permissions
import java.util.concurrent.ConcurrentHashMap

/** [KeyValueStore] for [System Property]. */
class SystemPropertyStore : KeyValueStore, AbstractKeyedDataObservable<String>() {

    private val defaultValues = ConcurrentHashMap<String, Any>()

    private val callback: Runnable =
        object : Runnable {
            override fun run() {
                defaultValues.keys.forEach { key -> notifyChange(key, DataChangeReason.UPDATE) }
            }
        }

    val tag: String
        get() = "SystemPropertyStore"

    override fun removeObserver(key: String, observer: KeyedObserver<String>) =
        if (super.removeObserver(key, observer)) {
            defaultValues.remove(key)
            true
        } else {
            false
        }

    override fun contains(key: String): Boolean = !SystemProperties.get(key, "").isEmpty()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? =
        try {
            when (valueType) {
                Boolean::class.javaObjectType ->
                    SystemProperties.getBoolean(
                        key,
                        getDefaultValue(key, Boolean::class.javaObjectType) ?: false,
                    )
                Float::class.javaObjectType ->
                    SystemProperties.get(
                            key,
                            getDefaultValue(key, Float::class.javaObjectType).toString() ?: "0.0",
                        )
                        .toFloat()
                Int::class.javaObjectType ->
                    SystemProperties.getInt(
                        key,
                        getDefaultValue(key, Int::class.javaObjectType) ?: 0,
                    )
                Long::class.javaObjectType ->
                    SystemProperties.getLong(
                        key,
                        getDefaultValue(key, Long::class.javaObjectType) ?: 0L,
                    )
                String::class.javaObjectType ->
                    SystemProperties.get(
                        key,
                        getDefaultValue(key, String::class.javaObjectType) ?: "",
                    )
                else -> throw UnsupportedOperationException("Get $key $valueType")
            }
                as T?
        } catch (_: Exception) {
            null
        } ?: getDefaultValue(key, valueType)

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value == null) {
            SystemProperties.set(key, null)
            return
        }
        when (valueType) {
            Boolean::class.javaObjectType,
            Float::class.javaObjectType,
            Int::class.javaObjectType,
            Long::class.javaObjectType,
            String::class.javaObjectType -> SystemProperties.set(key, "${value}")
            else -> throw UnsupportedOperationException("Set $key $valueType")
        }
    }

    override fun onFirstObserverAdded() {
        Log.i(tag, "addChangeCallback")
        SystemProperties.addChangeCallback(callback)
    }

    override fun onLastObserverRemoved() {
        Log.i(tag, "removeChangeCallback")
        SystemProperties.removeChangeCallback(callback)
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

    companion object {
        @Volatile private var instance: SystemPropertyStore? = null

        @JvmStatic
        fun get(): SystemPropertyStore =
            instance
                ?: synchronized(this) { instance ?: SystemPropertyStore().also { instance = it } }

        @VisibleForTesting
        fun resetInstance() {
            instance = null
        }

        /** Returns the required permissions to read [System Property]. */
        fun getReadPermissions() = Permissions.EMPTY

        /** Returns the required permissions to write [System Property]. */
        fun getWritePermissions() = Permissions.EMPTY
    }
}
