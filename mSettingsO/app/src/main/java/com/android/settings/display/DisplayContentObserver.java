/*
 Add this file for  Eye comfort mode and ReadingMode by shilei.zhang
 */

package com.android.settings.display;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

abstract class DisplayContentObserver extends ContentObserver {
    public DisplayContentObserver(Handler handler) {
        super(handler);
    }

    public void register(ContentResolver contentResolver) {
        contentResolver.registerContentObserver(Settings.System.getUriFor(
                "reading_mode_always_enable"), false, this);
        contentResolver.registerContentObserver(Settings.System.getUriFor(
                "night_display_qs"), false, this);
    }

    public void unregister(ContentResolver contentResolver) {
        contentResolver.unregisterContentObserver(this);
    }

    @Override
    public abstract void onChange(boolean selfChange, Uri uri);
}
