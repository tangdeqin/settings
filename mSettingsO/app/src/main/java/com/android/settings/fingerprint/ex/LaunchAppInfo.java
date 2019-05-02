package com.android.settings.fingerprint.ex;

/**
 * Copyright (C) 2018 Tcl Corporation Limited
 * Created by jinlong.lu on 18-3-17.
 */

public class LaunchAppInfo {
    private String mLaunchType;
    private String mLaunchName;

    public LaunchAppInfo(String mLaunchType, String mLaunchName) {
        this.mLaunchType = mLaunchType;
        this.mLaunchName = mLaunchName;
    }

    public LaunchAppInfo() {
    }

    public String getLaunchType() {
        return mLaunchType;
    }

    public void setLaunchType(String mLaunchType) {
        this.mLaunchType = mLaunchType;
    }

    public String getLaunchName() {
        return mLaunchName;
    }

    public void setLaunchName(String mLaunchName) {
        this.mLaunchName = mLaunchName;
    }
}
