/*
 * Copyright (C) 2017 AICP
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
import android.provider.Settings;
import android.util.AttributeSet;

import androidx.preference.PreferenceDataStore;

public class SecureSettingSeekBarPreference extends CustomSeekBarPreference {

    public SecureSettingSeekBarPreference(Context context, AttributeSet attrs, int defStyle, int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
        setPreferenceDataStore(new DataStore());
    }

    public SecureSettingSeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setPreferenceDataStore(new DataStore());
    }

    public SecureSettingSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPreferenceDataStore(new DataStore());
    }

    public SecureSettingSeekBarPreference(Context context) {
        super(context, null);
        setPreferenceDataStore(new DataStore());
    }

    private class DataStore extends PreferenceDataStore {
        @Override
        public float getFloat(String key, float defValue) {
            return Settings.Secure.getFloat(getContext().getContentResolver(), key, defValue);
        }

        @Override
        public int getInt(String key, int defValue) {
            return Settings.Secure.getInt(getContext().getContentResolver(), key, defValue);
        }

        @Override
        public long getLong(String key, long defValue) {
            return Settings.Secure.getLong(getContext().getContentResolver(), key, defValue);
        }

        @Override
        public String getString(String key, String defValue) {
            String result = Settings.Secure.getString(getContext().getContentResolver(), key);
            return result == null ? defValue : result;
        }

        @Override
        public void putFloat(String key, float value) {
            Settings.Secure.putFloat(getContext().getContentResolver(), key, value);
        }

        @Override
        public void putInt(String key, int value) {
            Settings.Secure.putInt(getContext().getContentResolver(), key, value);
        }

        @Override
        public void putLong(String key, long value) {
            Settings.Secure.putLong(getContext().getContentResolver(), key, value);
        }

        @Override
        public void putString(String key, String value) {
            Settings.Secure.putString(getContext().getContentResolver(), key, value);
        }
    }

}
