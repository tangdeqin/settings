package com.android.settings.privatemode;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceViewHolder;

import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class LongTitleSwitchPreference extends SwitchPreference {

    public LongTitleSwitchPreference(Context context) {
        super(context);
    }

    public LongTitleSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LongTitleSwitchPreference(Context context, AttributeSet attrs,
                                     int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LongTitleSwitchPreference(Context context, AttributeSet attrs,
                                     int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView titleView = (TextView) holder.findViewById(android.R.id.title);
        titleView.setSingleLine(false);
    }
}
