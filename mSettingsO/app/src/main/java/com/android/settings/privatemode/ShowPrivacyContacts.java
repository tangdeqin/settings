package com.android.settings.privatemode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.provider.Settings.System;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
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
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Button;
import android.Manifest;
import com.android.settings.R;
import com.android.settings.privatemode.util.photo.ContactPhotoManager;
import com.android.settings.privatemode.util.photo.ContactPhotoManager.DefaultImageRequest;

public class ShowPrivacyContacts extends ListActivity implements OnQueryTextListener, OnItemLongClickListener, OnClickListener {
    public static final String ACTION_MULTI_PICK_PRIVATE_CONTACTS = "com.android.contacts.action.ACTION_MULTI_PICK_PRIVATE_CONTACTS"; //modified by dongchi.chen for XR5574098
    private static final int LOAD_PRIVATE_CONTACTS = 66;
    private static final int ADD_PRIVATE_CONTACTS = 67;

    static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] {
        Contacts._ID,
        Contacts.DISPLAY_NAME_PRIMARY,
        Contacts.DISPLAY_NAME_ALTERNATIVE,
        Contacts.PHOTO_ID,
        Contacts.LOOKUP_KEY
    };

    private static final int CONTACT_ID_COLUMN_INDEX = 0;
    private static final int DISPLAY_NAME_PRIMARY_COLUMN_INDEX = 1;
    private static final int DISPLAY_NAME_ALTERNATIVE_COLUMN_INDEX = 2;
    private static final int PHOTO_ID_COLUMN_INDEX = 3;
    private static final int LOOKUP_KEY_COLUMN_INDEX = 4;

    private static final int CHECK_PERMISSION_RESULT = 1;

    private static final String IS_PRIVATE = "is_private"; //modified by dongchi.chen for XRP23153

    private ContactItemListAdapter mAdapter;
    private QueryHandler mQueryHandler;
    private Context mContext;
    private TextView mCountView;
    private TextView mIntroView;
    private SearchView mSearchView;
    private View mEmptyView;

    private boolean mIsMultiChoiceMode = false;
    private boolean mIsEmpty = false;
    private ArrayList<String> mChoiceSet = new ArrayList<String>();
    private TextView mSelectCount;
    private CheckBox mSelectAllCheckBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, CHECK_PERMISSION_RESULT);
        } else {
            init();
        }
    }

    private void init() {
        setContentView(R.layout.privacy_contacts);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        mEmptyView = findViewById(R.id.no_contact);
        mCountView = (TextView)findViewById(R.id.privacy_contacts_count);
        mIntroView = (TextView)findViewById(R.id.privacy_contacts_intro);

        mAdapter = new ContactItemListAdapter(this);
        getListView().setAdapter(mAdapter);
        getListView().setOnItemLongClickListener(this);

        mQueryHandler = new QueryHandler(this);
        mContext = getApplicationContext();
        loadPrivateContacts();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                            int[] grantResults) {
        if(CHECK_PERMISSION_RESULT == requestCode){
            if(this.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        if(mQueryHandler != null ){
            mQueryHandler.removeCallbacksAndMessages(LOAD_PRIVATE_CONTACTS);
        }
        if (mAdapter != null && mAdapter.getCursor() != null) {
            mAdapter.getCursor().close();
        }
        super.onDestroy();
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
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (!mIsMultiChoiceMode) {
            return;
        }
        if (mChoiceSet.contains((String) v.getTag())) {
            mChoiceSet.remove((String) v.getTag());
        } else {
            mChoiceSet.add((String) v.getTag());
        }
        mAdapter.notifyDataSetChanged();
        updateMultiSelectTitle();
    }

    public void loadPrivateContacts() {
        Uri uri = Contacts.CONTENT_URI;
        mQueryHandler.startQuery(LOAD_PRIVATE_CONTACTS, null, uri, CONTACTS_SUMMARY_PROJECTION, IS_PRIVATE + " =?",
                new String[]{String.valueOf(1)}, RawContacts.SORT_KEY_PRIMARY);
    }

    private class QueryHandler extends AsyncQueryHandler {
        protected WeakReference<ShowPrivacyContacts> mActivity;

        public QueryHandler(Context context) {
            super(context.getContentResolver());
            mActivity = new WeakReference<ShowPrivacyContacts>(
                    (ShowPrivacyContacts) context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (token != LOAD_PRIVATE_CONTACTS) {
                return;
            }
            // In the case of low memory, the WeakReference object may be
            // recycled.
            if (mActivity == null || mActivity.get() == null) {
                mActivity = new WeakReference<ShowPrivacyContacts>(
                        ShowPrivacyContacts.this);
            }
            final ShowPrivacyContacts activity = mActivity.get();
            int num = 0;
            if (cursor != null) {
                num = cursor.getCount();
            }
            if (num == 0) {
                mIsEmpty = true;
                showEmptyView();
                return;
            }
            mIsEmpty = false;
            activity.mAdapter.changeCursor(cursor);
            String s = getString(R.string.private_contact_fm, num);
            mCountView.setText(s);
            showListView();
        }
    }

    private final class ContactItemListAdapter extends CursorAdapter {
        Context mContext;
        protected LayoutInflater mInflater;
        private ContactPhotoManager mContactPhotoManager;

        public ContactItemListAdapter(Context context) {
            super(context, null, false);

            mContext = context;
            mInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mContactPhotoManager = ContactPhotoManager.getInstance(mContext);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            long contactId= cursor.getLong(CONTACT_ID_COLUMN_INDEX);
            String lookupKey = cursor.getString(LOOKUP_KEY_COLUMN_INDEX);
            String name = cursor.getString(DISPLAY_NAME_PRIMARY_COLUMN_INDEX);

            view.setTag(Long.toString(contactId));
            ((TextView) view.findViewById(R.id.contact_name))
                    .setText(name == null ? mContext.getText(R.string.missing_name) : name);

            long photoId = 0;
            if (!cursor.isNull(PHOTO_ID_COLUMN_INDEX)) {
                photoId = cursor.getLong(PHOTO_ID_COLUMN_INDEX);
            }

            ImageView photo = ((ImageView) view.findViewById(R.id.contact_photo));
            photo.setVisibility(View.VISIBLE);

            DefaultImageRequest request = null;
            if (photoId == 0) {
                request = new DefaultImageRequest(name, lookupKey, true);
            }

            //mContactPhotoManager.loadThumbnail(photo, photoId, false, true, request);
            view.setBackgroundColor(mContext.getResources().getColor(R.color.background_primary));

            if (mIsMultiChoiceMode) {
                if (mChoiceSet.contains(Long.toString(contactId))) {
                    photo.setImageResource(R.drawable.default_check_image);
                    view.setBackgroundColor(mContext.getResources().getColor(R.color.select_item_background));
                } else {
                    mContactPhotoManager.loadThumbnail(photo, photoId, false, true, request);
                }
            } else {
                mContactPhotoManager.loadThumbnail(photo, photoId, false, true, request);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = mInflater
                        .inflate(R.layout.privacy_contacts_item, parent, false);
            return v;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == ADD_PRIVATE_CONTACTS) && (resultCode == RESULT_OK)) {
            if (data != null) {
                if (data.getLongArrayExtra("com.tct.contacts.list.pickcontactsresult") != null) {
                    long[] ids = data.getLongArrayExtra("com.tct.contacts.list.pickcontactsresult");
                    Log.d("TestLog", "result size == " + ids.length);
                    if (ids.length > 0) {
                        ContentValues values = new ContentValues(1);
                        values.put(IS_PRIVATE, 1);
                        String CONTACT_KEY_IN = Contacts._ID + " IN (";
                        StringBuilder sel = new StringBuilder(CONTACT_KEY_IN);
                        boolean first = true;
                        for (long i : ids) {
                            if (!first) {
                                sel.append(',');
                            } else {
                                first = false;
                            }
                            String contactId = new Long(i).toString();
                            sel.append(contactId);
                        }
                        sel.append(')');
                        this.getContentResolver().update(Contacts.CONTENT_URI, values, sel.toString(), null);
                        loadPrivateContacts();
                    }
                }
            }
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        doFilter(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        doFilter(newText);
        return true;
    }

    public void doFilter(String s) {
        if (TextUtils.isEmpty(s)) {
            loadPrivateContacts();
            return;
        }

        Uri uri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(s.toString()));
        mQueryHandler.startQuery(LOAD_PRIVATE_CONTACTS, null, uri, CONTACTS_SUMMARY_PROJECTION, IS_PRIVATE + " =?",
                new String[]{String.valueOf(1)}, RawContacts.SORT_KEY_PRIMARY);
    }

    public void showEmptyView() {
        mEmptyView.setVisibility(View.VISIBLE);
        mCountView.setVisibility(View.GONE);
        mIntroView.setVisibility(View.GONE);
        getListView().setVisibility(View.GONE);
    }

    public void showListView() {
        mEmptyView.setVisibility(View.GONE);
        mCountView.setVisibility(View.VISIBLE);
        mIntroView.setVisibility(View.VISIBLE);
        getListView().setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        if (mIsMultiChoiceMode) {
            return false;
        }
        mChoiceSet.clear();
        mChoiceSet.add((String) view.getTag());
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
            mAdapter.notifyDataSetChanged();

            ActionBar actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowCustomEnabled(false);
            actionBar.setTitle(R.string.private_contact);
        }
    }

    private void updateMultiSelectTitle() {
        mSelectCount.setText(mChoiceSet.size() + "");
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
            long contactId = cursor.getLong(CONTACT_ID_COLUMN_INDEX);

            if (isSelected) {
                mChoiceSet.add(Long.toString(contactId));
            } else {
                mChoiceSet.remove(Long.toString(contactId));
            }
        }
        updateMultiSelectTitle();
        mAdapter.notifyDataSetChanged();
    }

    public void remove() {
        if (mChoiceSet.size() == 0) {
            return;
        }
        ContentValues values = new ContentValues(1);
        values.put(IS_PRIVATE, 0);
        String CONTACT_KEY_IN = Contacts._ID + " IN (";
        StringBuilder sel = new StringBuilder(CONTACT_KEY_IN);
        boolean first = true;
        for (int i = 0; i < mChoiceSet.size(); i++) {
            if (!first) {
                sel.append(',');
            } else {
                first = false;
            }
            sel.append(mChoiceSet.get(i));
        }
        sel.append(')');
        this.getContentResolver().update(Contacts.CONTENT_URI, values, sel.toString(), null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.private_contact_options, menu);
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
                Intent intent = new Intent(ACTION_MULTI_PICK_PRIVATE_CONTACTS, Contacts.CONTENT_URI);
                startActivityForResult(intent, ADD_PRIVATE_CONTACTS);
                break;
            case R.id.menu_select_item:
                enterMultiSelectMode();
                break;
            case R.id.menu_select_all:
                enterMultiSelectMode();
                selectAll();
                break;
            case R.id.menu_remove:
                removePrivateContacts();
                break;
            case android.R.id.home:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.back:
                onBackPressed();
                break;
        }
    }

    private void removePrivateContacts() {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
        .setCancelable(false)
        .setTitle(R.string.remove)
        .setMessage(R.string.remove_msg)
        .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                remove();
                if (mChoiceSet.size() > 0) {
                    loadPrivateContacts();
                }
                exitMultiSelectMode();
            }
        })
        .setNegativeButton(R.string.dlg_cancel, null)
        .show();
    }
}
