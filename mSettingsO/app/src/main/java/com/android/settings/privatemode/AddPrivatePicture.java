package com.android.settings.privatemode;

import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.Images;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.privatemode.util.IconHolder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.tct.sdk.base.privacymode.TctPrivacyModeHelper;


public class AddPrivatePicture extends ListActivity {
    public static final String[] PROJECTION = new String[] {
            Images.Media._ID,
            Images.Media.DATA,
            Images.Media.SIZE,
            Images.Media.DISPLAY_NAME,
    };

    private static final int ID_COLUMN_INDEX = 0;
    private static final int DATA_COLUMN_INDEX = 1;
    private static final int SIZE_COLUMN_INDEX = 2;
    private static final int DISPLAY_NAME_COLUMN_INDEX = 3;

    private static final int LOADER_PICTURE = 67;
    private static final String TCT_IS_PRIVATE = "tct_is_private";;

    private AlbumCursorAdapter mAdapter;
    ArrayList<String> mChoiceSet = new ArrayList<String>();
    ArrayList<String> mChoiceDataSet = new ArrayList<String>();
    private QueryHandler mQueryHandler;
    private ContentResolver mResolver;
    private boolean mShouldDestroy = false;
    private MenuItem mSelectAllMenu;
    private IconHolder mIconHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_private_picture);

        mResolver = this.getContentResolver();
        mIconHolder = new IconHolder(this, mResolver);

        mAdapter = new AlbumCursorAdapter(this);
        getListView().setAdapter(mAdapter);

        mQueryHandler = new QueryHandler(this);

        loadPictures();

    }

    public void loadPictures() {
        Uri uri = MediaStore.Files.getContentUri("external");
        String orderBy = FileColumns.DATE_MODIFIED + " DESC ";
        String sel = FileColumns.MEDIA_TYPE + "=" + FileColumns.MEDIA_TYPE_IMAGE + " AND " + TCT_IS_PRIVATE + "=0"
                + " AND " +  FileColumns.STORAGE_ID + "=" + StorageVolume.STORAGE_ID_PRIMARY;
        mQueryHandler.startQuery(LOADER_PICTURE, null, uri, PROJECTION, sel, null, orderBy);
    }

    private class QueryHandler extends AsyncQueryHandler {
        protected WeakReference<AddPrivatePicture> mActivity;

        public QueryHandler(Context context) {
            super(context.getContentResolver());
            mActivity = new WeakReference<AddPrivatePicture>(
                    (AddPrivatePicture) context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (token != LOADER_PICTURE) {
                return;
            }
            // In the case of low memory, the WeakReference object may be
            // recycled.
            if (mActivity == null || mActivity.get() == null) {
                mActivity = new WeakReference<AddPrivatePicture>(
                        AddPrivatePicture.this);
            }
            final AddPrivatePicture activity = mActivity.get();
            activity.mAdapter.changeCursor(cursor);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        CheckBox checkBox = (CheckBox) v.findViewById(R.id.pick_picture_check);
        boolean isChecked = !checkBox.isChecked();
        checkBox.setChecked(isChecked);
        PictureInfo info = (PictureInfo) v.getTag();
        if (isChecked) {
            mChoiceSet.add(info.file_id);
            mChoiceDataSet.add(info.data);
        } else {
            mChoiceSet.remove(info.file_id);
            mChoiceDataSet.remove(info.data);
        }
        updateTitle();
    }

    public class AlbumCursorAdapter extends CursorAdapter {

        public AlbumCursorAdapter(Context context) {
            super(context, null, false);
        }

        @Override
        public void bindView(View v, Context context, Cursor cursor) {
            PictureInfo info = new PictureInfo();
            info.file_id = Integer.toString(cursor.getInt(ID_COLUMN_INDEX));
            info.data = cursor.getString(DATA_COLUMN_INDEX);
            v.setTag(info);

            TextView titleTextView = (TextView) v.findViewById(
                    R.id.picture_name);

            String name = cursor.getString(DISPLAY_NAME_COLUMN_INDEX);
            titleTextView.setText(name);


            ImageView thumbImageView = (ImageView) v.findViewById(
                    R.id.thumb_image);
            mIconHolder.loadDrawable(thumbImageView, info.data, cursor.getLong(ID_COLUMN_INDEX));
            CheckBox checkBox = (CheckBox) v.findViewById(R.id.pick_picture_check);
            if (mChoiceSet.contains(info.file_id)) {
                checkBox.setChecked(true);
            } else {
                checkBox.setChecked(false);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = LayoutInflater.from(context).inflate(
                    R.layout.add_private_picture_item, parent, false);
            return v;
        }
    }

    @Override
    public void onDestroy() {
        if (mQueryHandler != null) {
            mQueryHandler.removeCallbacksAndMessages(LOADER_PICTURE);
        }
        if ((mAdapter != null) && (mAdapter.getCursor() != null)) {
            mAdapter.getCursor().close();
        }
        if (mIconHolder != null) {
            mIconHolder.cleanup();
        }
        super.onDestroy();
    }

    private void selectAll() {
        Cursor cursor = mAdapter.getCursor();
        if (cursor == null) {
            return;
        }

        boolean isSelected = mChoiceSet.size() != cursor.getCount();
        mChoiceSet.clear();

        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String file_id = Integer.toString(cursor.getInt(ID_COLUMN_INDEX));

            if (isSelected) {
                mChoiceSet.add(file_id);
                mChoiceDataSet.add(cursor.getString(DATA_COLUMN_INDEX));
            } else {
                mChoiceSet.remove(file_id);
                mChoiceDataSet.remove(cursor.getString(DATA_COLUMN_INDEX));
            }
        }

        int count = getListView().getChildCount();
        for (int i = 0; i < count; i++) {
            View v = getListView().getChildAt(i);
            CheckBox checkBox = (CheckBox) v.findViewById(R.id.pick_picture_check);
            checkBox.setChecked(isSelected);
        }
        updateTitle();
    }

    public void onDone() {
        if (mChoiceSet.size() == 0) {
            finish();
            return;
        }

        TctPrivacyModeHelper mTctPrivacyModeHelper = TctPrivacyModeHelper.createHelper(this);
        mTctPrivacyModeHelper.setFilePrivateFlag(null, mChoiceDataSet, true);

        setResult(RESULT_OK);
        finish();
    }

    private void updateTitle() {
    }

    public class PictureInfo {
        String file_id;
        String data;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_private_picture_options, menu);
        mSelectAllMenu = menu.findItem(R.id.menu_select_all);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_select_all:
                selectAll();
                break;
            case R.id.menu_done:
                onDone();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
