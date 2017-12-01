/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.LoaderManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
import android.net.INetworkStatsSession;
import android.os.Bundle;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.applications.AppInfoDashboardFragment;
import com.android.settings.datausage.AppDataUsage;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
public class AppDataUsagePreferenceControllerTest {

    @Mock
    private LoaderManager mLoaderManager;
    @Mock
    private AppInfoDashboardFragment mFragment;

    private Context mContext;
    private AppDataUsagePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        mController = spy(
                new AppDataUsagePreferenceController(mContext, mFragment, null /* lifecycle */));
    }

    @Test
    public void getAvailabilityStatus_bandwidthControlEnabled_shouldReturnAvailable() {
        doReturn(true).when(mController).isBandwidthControlEnabled();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(mController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_bandwidthControlDisabled_shouldReturnDisabled() {
        doReturn(false).when(mController).isBandwidthControlEnabled();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(mController.DISABLED_UNSUPPORTED);
    }

    @Test
    public void onResume_noSession_shouldNotRestartDataLoader() {
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();

        mController.onResume();

        verify(mLoaderManager, never()).restartLoader(
                AppInfoDashboardFragment.LOADER_CHART_DATA, Bundle.EMPTY, mController);
    }

    @Test
    public void onResume_hasSession_shouldRestartDataLoader() {
        final ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(connectivityManager);
        when(connectivityManager.isNetworkSupported(anyInt())).thenReturn(true);
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();
        ReflectionHelpers.setField(mController, "mStatsSession", mock(INetworkStatsSession.class));
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = new ApplicationInfo();
        when(mFragment.getAppEntry()).thenReturn(appEntry);

        mController.onResume();

        verify(mLoaderManager).restartLoader(
                eq(AppInfoDashboardFragment.LOADER_CHART_DATA), any(Bundle.class), eq(mController));
    }

    @Test
    public void onPause_shouldDestroyDataLoader() {
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();

        mController.onPause();

        verify(mLoaderManager).destroyLoader(AppInfoDashboardFragment.LOADER_CHART_DATA);
    }

    @Test
    public void getDetailFragmentClass_shouldReturnAppDataUsage() {
        assertThat(mController.getDetailFragmentClass()).isEqualTo(AppDataUsage.class);
    }

    @Test
    public void updateState_shouldUpdatePreferenceSummary() {
        final Preference preference = mock(Preference.class);

        mController.updateState(preference);

        verify(preference).setSummary(any());
    }

}
