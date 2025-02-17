/*
 * SPDX-FileCopyrightText: 2021-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.platform.internal;

import static lineageos.providers.LineageSettings.Secure.POWER_MENU_ACTIONS;
import static lineageos.providers.LineageSettings.Secure.getStringForUser;
import static lineageos.providers.LineageSettings.Secure.putStringForUser;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;

import org.lineageos.internal.util.PowerMenuConstants;

import lineageos.app.ILineageGlobalActions;
import lineageos.app.LineageContextConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @hide
 */
public class LineageGlobalActionsService extends LineageSystemService {

    private static final String TAG = "LineageGlobalActions";

    private final Context mContext;
    private final ContentResolver mContentResolver;

    private final List<String> mLocalUserConfig = new ArrayList<String>();

    // Observes user-controlled settings
    private GlobalActionsSettingsObserver mObserver;

    public LineageGlobalActionsService(Context context) {
        super(context);

        mContext = context;
        mContentResolver = mContext.getContentResolver();
    }

    private class GlobalActionsSettingsObserver extends ContentObserver {

        public GlobalActionsSettingsObserver(Context context, Handler handler) {
            super(handler);
        }

        public void observe(boolean enabled) {
        }

        @Override
        public void onChange(boolean selfChange) {
        }
    };

    private void populateUserConfig() {
        mLocalUserConfig.clear();
        mLocalUserConfig.addAll(Arrays.asList(getUserConfig()));
    }

    private String[] getUserConfig() {
        String savedActions = getStringForUser(mContentResolver,
                POWER_MENU_ACTIONS, UserHandle.USER_CURRENT);

        if (savedActions == null) {
            return mContext.getResources().getStringArray(
                    com.android.internal.R.array.config_globalActionsList);
        } else {
            return savedActions.split("\\|");
        }
    }

    private void updateUserConfigInternal(boolean enabled, String action) {
        if (enabled) {
            if (!mLocalUserConfig.contains(action)) {
                mLocalUserConfig.add(action);
            }
        } else {
            if (mLocalUserConfig.contains(action)) {
                mLocalUserConfig.remove(action);
            }
        }
        saveUserConfig();
    }

    private void saveUserConfig() {
        List<String> actions = new ArrayList<String>();
        for (String action : PowerMenuConstants.getAllActions()) {
            if (mLocalUserConfig.contains(action)) {
                actions.add(action);
            }
        }

        String s = String.join("|", actions);
        putStringForUser(mContentResolver, POWER_MENU_ACTIONS, s, UserHandle.USER_CURRENT);
    }

    @Override
    public String getFeatureDeclaration() {
        return LineageContextConstants.Features.GLOBAL_ACTIONS;
    }

    @Override
    public void onStart() {
        publishBinderService(LineageContextConstants.LINEAGE_GLOBAL_ACTIONS_SERVICE, mBinder);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_BOOT_COMPLETED) {
            populateUserConfig();

            mObserver = new GlobalActionsSettingsObserver(mContext, null);
            mObserver.observe(true);
        }
    }

    private final IBinder mBinder = new ILineageGlobalActions.Stub() {

        @Override
        public void updateUserConfig(boolean enabled, String action) {
            updateUserConfigInternal(enabled, action);
        }

        @Override
        public List<String> getLocalUserConfig() {
            populateUserConfig();
            return mLocalUserConfig;
        }

        @Override
        public String[] getUserActionsArray() {
            return getUserConfig();
        }

        @Override
        public boolean userConfigContains(String preference) {
            return getLocalUserConfig().contains(preference);
        }
    };
}
