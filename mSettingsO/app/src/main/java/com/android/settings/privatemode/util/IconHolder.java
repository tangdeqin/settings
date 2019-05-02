/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.privatemode.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.WeakHashMap;
import java.util.Map;

/**
 * A class that holds icons for a more efficient access.
 */
public class IconHolder {

    private static final int MAX_CACHE = 500;

    private static final int MSG_LOAD = 1;
    private static final int MSG_LOADED = 2;

    private final Map<String, Drawable> mThumbnails;

    private final WeakHashMap<ImageView, Loadable> mRequests;

    private final Context mContext;

    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;
    private ContentResolver mContentResolver;

    /**
     * This is kind of a hack, we should have a loadable for each MimeType we run into.
     * TODO: Refactor this to have different loadables
     */
    private static class Loadable {
        private Context mContext;

        String filePath;
        WeakReference<ImageView> view;
        Drawable result;
        ContentResolver mContentResolver;
        long mOrigId;

        public Loadable(Context context, ImageView view, String filepath, ContentResolver cr, long origId) {
            this.mContext = context.getApplicationContext();
            this.filePath = filepath;
            this.view = new WeakReference<ImageView>(view);
            this.result = null;
            this.mContentResolver = cr;
            this.mOrigId = origId;
        }

        public boolean load() {
            return (result = loadDrawable(filePath)) != null;
        }

        private Drawable loadDrawable(String filePath) {
            return getImageDrawable(filePath);
//            long start = System.currentTimeMillis();
//            Bitmap thumb = Images.Thumbnails.getThumbnail(mContentResolver, mOrigId,
//                    Images.Thumbnails.MINI_KIND, null);
//            long start2 = System.currentTimeMillis();
//            Log.i("ccxccx_c","time1111 = " + (start2 - start));
//            if (thumb == null) {
//                return null;
//            }
//            Drawable td = new BitmapDrawable(mContext.getResources(), thumb);
//            //return new BitmapDrawable(mContext.getResources(), thumb);
//            Log.i("ccxccx_c","time2222 = " + (System.currentTimeMillis() - start2));
//            return td;
        }

        /**
         * Method that returns a thumbnail of the picture
         *
         * @param file The path to the file
         * @return Drawable The drawable or null if cannot be extracted
         */
        private Drawable getImageDrawable(String filePath) {
            //long start = System.currentTimeMillis();
            Bitmap thumb = ThumbnailUtils.createImageThumbnail(
                    filePath,
                    ThumbnailUtils.TARGET_SIZE_MICRO_THUMBNAIL);
            //long start2 = System.currentTimeMillis();
            //Log.i("ccxccx_c","time1111 = " + (start2 - start));
            if (thumb == null) {
                return null;
            }
            return new BitmapDrawable(mContext.getResources(), thumb);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOADED:
                    processResult((Loadable) msg.obj);
                    break;
            }
        }

        private void processResult(Loadable result) {
            ImageView view = result.view.get();
            if (view == null) {
                return;
            }

            Loadable requestedForImageView = mRequests.get(view);
            if (requestedForImageView != result) {
                return;
            }

            // Cache the new drawable
            if (result.result != null) {
                mThumbnails.put(result.filePath, result.result);
            }
            view.setImageDrawable(result.result);
        }
    };

    /**
     * Constructor of <code>IconHolder</code>.
     */
    public IconHolder(Context context, ContentResolver cr) {
        super();
        this.mContext = context;
        this.mContentResolver = cr;
        this.mRequests = new WeakHashMap<ImageView, Loadable>();
        this.mThumbnails = new LinkedHashMap<String, Drawable>(MAX_CACHE, .75F, true) {
            private static final long serialVersionUID = 1L;
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Drawable> eldest) {
                return size() > MAX_CACHE;
            }
        };
    }

    /**
     * Method that returns a drawable reference of a FileSystemObject.
     *
     * @param iconView View to load the drawable into
     * @param fso The FileSystemObject reference
     * @param defaultIcon Drawable to be used in case no specific one could be found
     * @return Drawable The drawable reference
     */
    public void loadDrawable(ImageView iconView, String filePath, long origId) {
        // Is cached?
        if (this.mThumbnails.containsKey(filePath)) {
            iconView.setImageDrawable(this.mThumbnails.get(filePath));
            return;
        }

        if (mWorkerThread == null) {
            mWorkerThread = new HandlerThread("IconHolderLoader");
            mWorkerThread.start();
            mWorkerHandler = new WorkerHandler(mWorkerThread.getLooper());
        }
        Loadable previousForView = mRequests.get(iconView);
        if (previousForView != null) {
            mWorkerHandler.removeMessages(MSG_LOAD, previousForView);
        }

        Loadable loadable = new Loadable(mContext, iconView, filePath, mContentResolver, origId);
        mRequests.put(iconView, loadable);

        mWorkerHandler.obtainMessage(MSG_LOAD, loadable).sendToTarget();
    }

    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD:
                    Loadable l = (Loadable) msg.obj;
                    if (l.load()) {
                        mHandler.obtainMessage(MSG_LOADED, l).sendToTarget();
                    }
                    break;
            }
        }
    }

    /**
     * Shut down worker thread
     */
    private void shutdownWorker() {
        if (mWorkerThread != null) {
            mWorkerThread.getLooper().quit();
            mWorkerHandler = null;
            mWorkerThread = null;
        }
    }

    /**
     * Free any resources used by this instance
     */
    public void cleanup() {
        this.mRequests.clear();
        this.mThumbnails.clear();
        shutdownWorker();
    }
}
