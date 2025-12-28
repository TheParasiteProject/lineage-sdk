/*
 * SPDX-FileCopyrightText: 2024 The Android Open Source Project
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package lineageos.datastore

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.Settings.SettingNotFoundException
import androidx.annotation.VisibleForTesting
import com.android.settingslib.datastore.Permissions
import lineageos.providers.LineageSettings.System

/**
 * [KeyValueStore] for [LineageSystem] lineage settings.
 *
 * By default, a boolean type `true` value is stored as `1` and `false` value is stored as `0`.
 */
class LineageSettingsSystemStore private constructor(contentResolver: ContentResolver) :
    SettingsStore(contentResolver) {

    override val uri: Uri
        get() = System.getUriFor("")

    override val tag: String
        get() = "LineageSettingsSystemStore"

    override fun contains(key: String): Boolean = System.getString(contentResolver, key) != null

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? =
        try {
            when (valueType) {
                Boolean::class.javaObjectType -> System.getInt(contentResolver, key) != 0
                Float::class.javaObjectType -> System.getFloat(contentResolver, key)
                Int::class.javaObjectType -> System.getInt(contentResolver, key)
                Long::class.javaObjectType -> System.getLong(contentResolver, key)
                String::class.javaObjectType -> System.getString(contentResolver, key)
                else -> throw UnsupportedOperationException("Get $key $valueType")
            }
                as T?
        } catch (_: SettingNotFoundException) {
            null
        } ?: getDefaultValue(key, valueType)

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value == null) {
            System.putString(contentResolver, key, null)
            return
        }
        when (valueType) {
            Boolean::class.javaObjectType ->
                System.putInt(contentResolver, key, if (value == true) 1 else 0)
            Float::class.javaObjectType -> System.putFloat(contentResolver, key, value as Float)
            Int::class.javaObjectType -> System.putInt(contentResolver, key, value as Int)
            Long::class.javaObjectType -> System.putLong(contentResolver, key, value as Long)
            String::class.javaObjectType -> System.putString(contentResolver, key, value as String)
            else -> throw UnsupportedOperationException("Set $key $valueType")
        }
    }

    companion object {
        @Volatile private var instance: LineageSettingsSystemStore? = null

        @JvmStatic
        fun get(context: Context): LineageSettingsSystemStore =
            instance
                ?: synchronized(this) {
                    instance
                        ?: LineageSettingsSystemStore(context.applicationContext.contentResolver)
                            .also { instance = it }
                }

        @VisibleForTesting
        fun resetInstance() {
            instance = null
        }

        /** Returns the required permissions to read [System] settings. */
        fun getReadPermissions() = Permissions.EMPTY

        /** Returns the required permissions to write [System] settings. */
        fun getWritePermissions() = Permissions.allOf(Manifest.permission.WRITE_SETTINGS)
    }
}
