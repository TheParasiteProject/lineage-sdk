/*
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2017 AICP
 * Copyright (C) 2022 Project Kaleidoscope
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lineageos.preference;

import android.content.Context;
import android.os.SystemProperties;
import android.util.AttributeSet;

import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

public class SystemPropertySwitchPreference extends SwitchPreferenceCompat {

    private final ConstraintsHelper mConstraints;

    public SystemPropertySwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mConstraints = new ConstraintsHelper(context, attrs, this);
        setPreferenceDataStore(new DataStore());
    }

    public SystemPropertySwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mConstraints = new ConstraintsHelper(context, attrs, this);
        setPreferenceDataStore(new DataStore());
    }

    public SystemPropertySwitchPreference(Context context) {
        super(context);
        mConstraints = new ConstraintsHelper(context, null, this);
        setPreferenceDataStore(new DataStore());
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mConstraints.onAttached();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mConstraints.onBindViewHolder(holder);
    }

    public void setAvailable(boolean available) {
        mConstraints.setAvailable(available);
    }

    public boolean isAvailable() {
        return mConstraints.isAvailable();
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        // This is what default TwoStatePreference implementation is doing without respecting
        // real default value:
        //setChecked(restoreValue ? getPersistedBoolean(mChecked)
        //        : (Boolean) defaultValue);
        // Instead, we better do
        setChecked(restoreValue ? getPersistedBoolean((Boolean) defaultValue)
                : (Boolean) defaultValue);
    }

    private class DataStore extends PreferenceDataStore {
        @Override
        public boolean getBoolean(String key, boolean defValue) {
            return SystemProperties.getBoolean(key, defValue);
        }
    
        @Override
        public int getInt(String key, int defValue) {
            return SystemProperties.getInt(key, defValue);
        }

        @Override
        public long getLong(String key, long defValue) {
            return SystemProperties.getLong(key, defValue);
        }

        @Override
        public String getString(String key, String defValue) {
            return SystemProperties.get(key, defValue);
        }

        @Override
        public void putBoolean(String key, boolean value) {
            putString(key, value ? "1" : "0");
        }

        @Override
        public void putInt(String key, int value) {
            putString(key, Integer.toString(value));
        }

        @Override
        public void putLong(String key, long value) {
            putString(key, Long.toString(value));
        }

        @Override
        public void putString(String key, String value) {
            SystemProperties.set(key, value);
        }
    }
}
