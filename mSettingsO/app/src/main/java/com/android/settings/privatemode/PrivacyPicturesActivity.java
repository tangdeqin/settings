package com.android.settings.privatemode;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract.Document;
import android.provider.MediaStore;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.Images;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.util.Log;

//import com.mediatek.omadrm.OmaDrmUtils;
//import com.mediatek.omadrm.OmaDrmStore;
//import com.mediatek.media.MtkMediaStore;
import android.drm.DrmManagerClient;
import android.drm.DrmStore;

import com.android.settings.privatemode.util.IconHolder;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.tct.sdk.base.privacymode.TctPrivacyModeHelper;
//import com.mediatek.dcfdecoder.DcfDecoder;
import com.android.settings.R;

public class PrivacyPicturesActivity extends ListActivity implements OnItemLongClickListener, OnClickListener {
    public static final String[] PROJECTION = new String[] {
            Images.Media._ID,
            Images.Media.DATA,
            Images.Media.DATE_MODIFIED,
            Images.Media.DISPLAY_NAME,
            Images.Media.IS_DRM,
            Document.COLUMN_MIME_TYPE
    };

    private static final int ID_COLUMN_INDEX = 0;
    private static final int DATA_COLUMN_INDEX = 1;
    private static final int DATE_MODIFIED_COLUMN_INDEX = 2;
    private static final int DISPLAY_NAME_COLUMN_INDEX = 3;

    private static final int LOADER_PRIVATE_PICTURES = 628;
    private static final int ADD_PRIVATE_PICTURES = 629;
    private static final int CHECK_PERMISSION_RESULT = 1;
    private static final String TCT_IS_PRIVATE = "tct_is_private";

    boolean mIsMultiChoiceMode = false;
    private boolean mIsEmpty = false;

    ArrayList<String> mChoiceSet = new ArrayList<String>();
    ArrayList<String> mChoiceDataSet = new ArrayList<String>();
    private TextView mCountView;
    private LinearLayout mEmptyView;
    private QueryHandler mQueryHandler;
    private ContentResolver mResolver;
    private AlbumCursorAdapter mAdapter;
    private boolean mShouldDestroy = false;
    private MenuItem mSelectAllMenu;
    private TextView mSelectCount;
    private SimpleDateFormat mDateFormat;
    private IconHolder mIconHolder;
    private TctPrivacyModeHelper mTctPrivacyModeHelper;
    private DrmManagerClient mOmaDrmClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, CHECK_PERMISSION_RESULT);
        } else {
            init();
        }
    }

    private void makeupNoPrivatePicturePrompt(){
        TextView noPrivatePicturePrompt = (TextView)findViewById(R.id.no_private_picture_prompt);
        noPrivatePicturePrompt.setText((String)getResources().getText(R.string.no_private_picture_prompt));
    }

    private void init() {
        setContentView(R.layout.private_picture);

        makeupNoPrivatePicturePrompt();

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        mEmptyView = (LinearLayout) findViewById(R.id.no_picture);
        mCountView = (TextView)findViewById(R.id.private_picture_count);

        mResolver = this.getContentResolver();
        String strDateFormat = android.provider.Settings.System.getString(mResolver,android.provider.Settings.System.DATE_FORMAT);
        if (TextUtils.isEmpty(strDateFormat)) {
            strDateFormat = "dd.MM.yyyy HH:mm:ss"; //"dd/MM/yyyy HH:mm:ss"
        }
        mDateFormat = new SimpleDateFormat(strDateFormat);
        mIconHolder = new IconHolder(this, mResolver);
        mTctPrivacyModeHelper = TctPrivacyModeHelper.createHelper(this);

        mAdapter = new AlbumCursorAdapter(this);
        getListView().setAdapter(mAdapter);
        getListView().setOnItemLongClickListener(this);

        mQueryHandler = new QueryHandler(this);

        mOmaDrmClient = new DrmManagerClient(this);
        loadPictures();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if(CHECK_PERMISSION_RESULT == requestCode){
            if(this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                finish();
            }
        }
    }

    public void loadPictures() {
        Uri uri = MediaStore.Files.getContentUri("external");
        String orderBy = FileColumns.DATE_MODIFIED + " DESC ";
        String sel = FileColumns.MEDIA_TYPE + "=" + FileColumns.MEDIA_TYPE_IMAGE + " AND " + TCT_IS_PRIVATE + "=1";
        mQueryHandler.startQuery(LOADER_PRIVATE_PICTURES, null, uri, PROJECTION, sel, null, orderBy);
    }

    private void setPicturesPrivate(Intent intent) {
        ArrayList<CharSequence> list = intent.getCharSequenceArrayListExtra("photo_path");
        if(null != list) {
            Log.d("privacyModeTrace", "setPicturesPrivate size - " + list.size());
            long startTime = System.currentTimeMillis();
            ArrayList<String> paths = new ArrayList<>();
            for (CharSequence i : list) {
                paths.add((String) i);
            }
            mTctPrivacyModeHelper.setFilePrivateFlag(null, paths, true);
            Log.d("privacyModeTrace", "setPicturesPrivate cost time -" + (System.currentTimeMillis() - startTime));
        }
    }

    private class QueryHandler extends AsyncQueryHandler {
        protected WeakReference<PrivacyPicturesActivity> mActivity;

        public QueryHandler(Context context) {
            super(context.getContentResolver());
            mActivity = new WeakReference<PrivacyPicturesActivity>(
                    (PrivacyPicturesActivity) context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (token != LOADER_PRIVATE_PICTURES) {
                return;
            }
            // In the case of low memory, the WeakReference object may be
            // recycled.
            if (mActivity == null || mActivity.get() == null) {
                mActivity = new WeakReference<PrivacyPicturesActivity>(
                        PrivacyPicturesActivity.this);
            }
            final PrivacyPicturesActivity activity = mActivity.get();
            int num = 0;
            if (cursor != null) {
                num = cursor.getCount();
            }
            if (num == 0) {
                showEmptyView();
                mIsEmpty = true;
                return;
            }
            mIsEmpty = false;
            activity.mAdapter.changeCursor(cursor);
            String s = getString(R.string.private_picture_fm, num);
            mCountView.setText(s);
            showListView();
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (!mIsMultiChoiceMode) {
            return;
        }
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
        updateMultiSelectTitle();
    }

    public class AlbumCursorAdapter extends CursorAdapter {

        public AlbumCursorAdapter(Context context) {
            super(context, null, false);
        }

        public void bindView(View v, Context context, Cursor cursor) {
            PictureInfo info = new PictureInfo();
            info.file_id = Integer.toString(cursor.getInt(ID_COLUMN_INDEX));
            info.data = cursor.getString(DATA_COLUMN_INDEX);
            v.setTag(info);

            TextView titleTextView = (TextView) v.findViewById(
                    R.id.picture_name);
            String name = cursor.getString(DISPLAY_NAME_COLUMN_INDEX);
            titleTextView.setText(name);
            TextView timeTextView = (TextView) v.findViewById(
                    R.id.picture_timestamp);
            long dateModifiedInSeconds = cursor.getLong(DATE_MODIFIED_COLUMN_INDEX);
            timeTextView.setText(mDateFormat.format(new Date(dateModifiedInSeconds*1000)));

            ImageView thumbImageView = (ImageView) v.findViewById(
                    R.id.thumb_image);

            if(isDrmFile(cursor)){
                showDrmImage(thumbImageView, info.data);
                showDrmIcon(cursor, v);
            }else {
                mIconHolder.loadDrawable(thumbImageView, info.data, cursor.getLong(ID_COLUMN_INDEX));
                hideDrmIcon(cursor, v);
            }

            CheckBox checkBox = (CheckBox) v.findViewById(R.id.pick_picture_check);
            if (mIsMultiChoiceMode) {
                if (mChoiceSet.contains(info.file_id)) {
                    checkBox.setChecked(true);
                } else {
                    checkBox.setChecked(false);
                }
                checkBox.setVisibility(View.VISIBLE);
            } else {
                checkBox.setChecked(false);
                checkBox.setVisibility(View.GONE);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = LayoutInflater.from(context).inflate(
                    R.layout.private_picture_item, parent, false);
            return v;
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsMultiChoiceMode) {
            exitMultiSelectMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == ADD_PRIVATE_PICTURES) && (resultCode == RESULT_OK)) {
            if(null != data){
                setPicturesPrivate(data);
            }
            loadPictures();
        }
    }

    @Override
    public void onDestroy() {
        if (mQueryHandler != null) {
            mQueryHandler.removeCallbacksAndMessages(LOADER_PRIVATE_PICTURES);
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
        updateMultiSelectTitle();
    }

    private void removePrivatePictures() {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.remove)
                .setMessage(R.string.remove_msg)
                .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mTctPrivacyModeHelper.setFilePrivateFlag(null, mChoiceDataSet, false);
                        loadPictures();
                        exitMultiSelectMode();
                    }
                })
                .setNegativeButton(R.string.dlg_cancel, null)
                .show();
    }

    public void onDone() {
        Uri uri = MediaStore.Files.getContentUri("external");
        ContentValues values = new ContentValues(1);
        values.put(TCT_IS_PRIVATE, 0);
        final String FILE_KEY_IN = Images.Media._ID + " IN (";
        StringBuilder selection = new StringBuilder(FILE_KEY_IN);
        boolean first = true;
        boolean firstBuket = true;
        for (int i = 0; i < mChoiceSet.size(); i++) {
            if (first) {
                first = false;
            } else {
                selection.append(',');
            }
            selection.append(mChoiceSet.get(i));
        }
        selection.append(')');
        String sel = selection.toString();
        mResolver.update(uri, values, selection.toString(), null);
    }

    public void showEmptyView() {
        mEmptyView.setVisibility(View.VISIBLE);
        mCountView.setVisibility(View.GONE);
        getListView().setVisibility(View.GONE);
    }

    public void showListView() {
        mEmptyView.setVisibility(View.GONE);
        mCountView.setVisibility(View.VISIBLE);
        getListView().setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
                                   int position, long id) {
        if (mIsMultiChoiceMode) {
            return false;
        }
        mChoiceSet.clear();
        mChoiceDataSet.clear();
        mChoiceSet.add(((PictureInfo) view.getTag()).file_id);
        mChoiceDataSet.add(((PictureInfo) view.getTag()).data);
        enterMultiSelectMode();
        return true;

    }

    private void enterMultiSelectMode() {
        if (mIsMultiChoiceMode) {
            return;
        }
        mIsMultiChoiceMode = true;
        mAdapter.notifyDataSetChanged();

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.list_actionbar);
        
        
        ImageView mBackView = (ImageView) actionBar.getCustomView().findViewById(R.id.back);
        mBackView.setOnClickListener(this);
        
        mSelectCount = (TextView) actionBar.getCustomView().findViewById(R.id.select_count);
        updateMultiSelectTitle();
    }

    private void exitMultiSelectMode() {
        if (mIsMultiChoiceMode) {
            mIsMultiChoiceMode = false;
            mChoiceSet.clear();
            mChoiceDataSet.clear();
            mAdapter.notifyDataSetChanged();
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowCustomEnabled(false);
            actionBar.setTitle(R.string.private_picture);
        }
    }

    private void updateMultiSelectTitle() {
        mSelectCount.setText(mChoiceSet.size() + "");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.private_picture_options, menu);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem addMenu = menu.findItem(R.id.menu_add);
        MenuItem selectItem = menu.findItem(R.id.menu_select_item);
        MenuItem selectAll = menu.findItem(R.id.menu_select_all);
        MenuItem removeMenu = menu.findItem(R.id.menu_remove);
        if (mIsMultiChoiceMode) {
            addMenu.setVisible(false);
            selectItem.setVisible(false);
            selectAll.setVisible(true);
            if (mChoiceSet.size() == 0) {
                removeMenu.setVisible(false);
            } else if (mAdapter.getCount() == mChoiceSet.size()) {
                selectAll.setVisible(false);
                removeMenu.setVisible(true);
            } else {
                selectAll.setVisible(true);
                removeMenu.setVisible(true);
            }
        } else {
            addMenu.setVisible(true);
            removeMenu.setVisible(false);
            if (mIsEmpty) {
                selectItem.setVisible(false);
                selectAll.setVisible(false);
            } else {
                selectItem.setVisible(true);
                selectAll.setVisible(true);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                startAddPrivatePictures();
                break;
            case R.id.menu_remove:
                removePrivatePictures();
                break;
            case R.id.menu_select_item:
                enterMultiSelectMode();
                break;
            case R.id.menu_select_all:
                enterMultiSelectMode();
                selectAll();
                break;
            case android.R.id.home:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public class PictureInfo {
        String file_id;
        String data;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                onBackPressed();
                break;
        }
    }

    private void startAddPrivatePictures(){
        try {
            Intent intent = new Intent("com.tct.gallery.GET_CONTENT");
            intent.putExtra("SELECTION_TAG", 2);
            startActivityForResult(intent, ADD_PRIVATE_PICTURES);
        }catch (ActivityNotFoundException e){
            Intent intent = new Intent("com.tct.privacymode.action.ADD_PRIVACY_ALBUM");
            startActivityForResult(intent, ADD_PRIVATE_PICTURES);
        }
    }

    //for drm function ----------------------- begin
    private void showDrmIcon(Cursor cursor, View v){
        ImageView iconDrm = (ImageView) v.findViewById(R.id.icon_drm);
        boolean isDrm = isDrmFile(cursor);
        if(isDrm){
            int right = getRightsStatus(cursor);
            int lockResId = 0;//com.mediatek.internal.R.drawable.drm_red_lock;
            if(DrmStore.RightsStatus.RIGHTS_VALID == right){
                lockResId = 0;//com.mediatek.internal.R.drawable.drm_green_lock;
            }
            iconDrm.setVisibility(View.VISIBLE);
            iconDrm.setImageResource(lockResId);
        }
        else {
            iconDrm.setVisibility(View.GONE);
        }
    }

    private void hideDrmIcon(Cursor cursor, View v){
        ImageView iconDrm = (ImageView) v.findViewById(R.id.icon_drm);
        iconDrm.setVisibility(View.GONE);
    }

    private boolean isDrmFile(Cursor cursor) {
        final int index = cursor.getColumnIndex(MediaStore.MediaColumns.IS_DRM);
        return (index != -1 && cursor.getInt(index) > 0);
    }

    public static String getCursorString(Cursor cursor, String columnName) {
        final int index = cursor.getColumnIndex(columnName);
        return (index != -1) ? cursor.getString(index) : null;
    }

    private int getRightsStatus(Cursor cursor){
        int right = DrmStore.RightsStatus.RIGHTS_INVALID;
        String data = getCursorString(cursor, MediaStore.MediaColumns.DATA);
        if (data != null) {
            right = mOmaDrmClient.checkRightsStatus(data);
        }
        return right;
        //return 0;
    }

    private void showDrmImage(ImageView view, String filepath){
        new ShowDrmImageAsyncTask(view, filepath, getApplicationContext()).execute();
    }
    //for drm function ----------------------- end
}

class ShowDrmImageAsyncTask extends AsyncTask<Void, Void, Bitmap>{
    private String mFilePath;
    private WeakReference<ImageView> mThumb;
    //private WeakReference<Context> mContext;
    public ShowDrmImageAsyncTask(ImageView thumb, String filePath, Context context) {
        mFilePath = filePath;
        mThumb = new WeakReference<ImageView>(thumb);
        //mContext = new WeakReference<Context>(context);
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        /*DcfDecoder decoder = new DcfDecoder();
        byte[] data = decoder.forceDecryptFile(mFilePath, false);
        if(null != data){
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        }else {
            return null;
        }*/
        return  null;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (mThumb.get() != null) {
            if(null != result){
                mThumb.get().setImageBitmap(result);
            }else {
                int ic_doc_image = mThumb.get().getContext().getResources().getIdentifier("ic_doc_image","drawable","android");
                mThumb.get().setImageResource(ic_doc_image);
            }
        }

    }
}
