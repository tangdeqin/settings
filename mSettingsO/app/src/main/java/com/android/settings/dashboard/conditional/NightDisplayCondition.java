/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.dashboard.conditional;

import android.graphics.drawable.Icon;
import android.provider.Settings;

import com.android.internal.app.NightDisplayController;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.display.NightDisplaySettings;

public final class NightDisplayCondition extends Condition
        implements NightDisplayController.Callback {

    private NightDisplayController mController;

    NightDisplayCondition(ConditionManager manager) {
        super(manager);
        mController = new NightDisplayController(manager.getContext());
        mController.setListener(this);
    }

    @Override
    public int getMetricsConstant() {
        return MetricsEvent.SETTINGS_CONDITION_NIGHT_DISPLAY;
    }

    @Override
    public Icon getIcon() {
        //Begin modify by shilei.zhang for eye_comfort_mode ergo dev XR7033708 on 2018/09/25
        return Icon.createWithResource(mManager.getContext(), R.drawable.ic_settings_eye_comfort);
        //End modify by shilei.zhang for eye_comfort_mode ergo dev XR7033708 on 2018/09/25
    }

    @Override
    public CharSequence getTitle() {
        //Begin modify by shilei.zhang for eye_comfort_mode ergo dev XR7033708 on 2018/09/25
        //return mManager.getContext().getString(R.string.condition_night_display_title);
        return mManager.getContext().getString(R.string.condition_eye_comfort_mode_title);
        //End modify by shilei.zhang for eye_comfort_mode ergo dev XR7033708 on 2018/09/25
    }

    @Override
    public CharSequence getSummary() {
        return mManager.getContext().getString(R.string.condition_night_display_summary);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[] { mManager.getContext().getString(R.string.condition_turn_off) };
    }

    @Override
    public void onPrimaryClick() {
        //Begin modify by shilei.zhang for eye_comfort_mode ergo dev XR7033708 on 2018/09/25
        Utils.startWithFragment(mManager.getContext(), NightDisplaySettings.class.getName(), null,
                null, 0, R.string.eye_comfort_mode_title, null, MetricsEvent.DASHBOARD_SUMMARY);
        //Begin modify by shilei.zhang for eye_comfort_mode ergo dev XR7033708 on 2018/09/25
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            mController.setActivated(false);
        } else {
            throw new IllegalArgumentException("Unexpected index " + index);
        }
    }

    @Override
    public void refreshState() {
        setActive(mController.isActivated());
    }

    @Override
    public void onActivated(boolean activated) {
        refreshState();
    }
}
