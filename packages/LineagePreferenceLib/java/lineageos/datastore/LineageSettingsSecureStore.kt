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
import lineageos.providers.LineageSettings.Secure

/**
 * [KeyValueStore] for [Secure] lineage settings.
 *
 * By default, a boolean type `true` value is stored as `1` and `false` value is stored as `0`.
 */
class LineageSettingsSecureStore private constructor(contentResolver: ContentResolver) :
    SettingsStore(contentResolver) {

    override val uri: Uri
        get() = Secure.getUriFor("")

    override val tag: String
        get() = "LineageSettingsSecureStore"

    override fun contains(key: String): Boolean = Secure.getString(contentResolver, key) != null

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? =
        try {
            when (valueType) {
                Boolean::class.javaObjectType -> Secure.getInt(contentResolver, key) != 0
                Float::class.javaObjectType -> Secure.getFloat(contentResolver, key)
                Int::class.javaObjectType -> Secure.getInt(contentResolver, key)
                Long::class.javaObjectType -> Secure.getLong(contentResolver, key)
                String::class.javaObjectType -> Secure.getString(contentResolver, key)
                else -> throw UnsupportedOperationException("Get $key $valueType")
            }
                as T?
        } catch (_: SettingNotFoundException) {
            null
        } ?: getDefaultValue(key, valueType)

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value == null) {
            Secure.putString(contentResolver, key, null)
            return
        }
        when (valueType) {
            Boolean::class.javaObjectType ->
                Secure.putInt(contentResolver, key, if (value == true) 1 else 0)
            Float::class.javaObjectType -> Secure.putFloat(contentResolver, key, value as Float)
            Int::class.javaObjectType -> Secure.putInt(contentResolver, key, value as Int)
            Long::class.javaObjectType -> Secure.putLong(contentResolver, key, value as Long)
            String::class.javaObjectType -> Secure.putString(contentResolver, key, value as String)
            else -> throw UnsupportedOperationException("Set $key $valueType")
        }
    }

    companion object {
        @Volatile private var instance: LineageSettingsSecureStore? = null

        @JvmStatic
        fun get(context: Context): LineageSettingsSecureStore =
            instance
                ?: synchronized(this) {
                    instance
                        ?: LineageSettingsSecureStore(context.applicationContext.contentResolver)
                            .also { instance = it }
                }

        @VisibleForTesting
        fun resetInstance() {
            instance = null
        }

        /** Returns the required permissions to read [Secure] settings. */
        fun getReadPermissions() = Permissions.EMPTY

        /** Returns the required permissions to write [Secure] settings. */
        fun getWritePermissions() = Permissions.allOf(Manifest.permission.WRITE_SECURE_SETTINGS)
    }
}
