package com.android.settings.search;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.util.Log;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.dashboard.SiteMapManager;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import java.util.List;
import android.database.CursorWrapper;
import android.database.MergeCursor;

public class SettingsSearchProvider extends ContentProvider {

    private final static int INDEX = 0, INIT = 1, SITEMAP = 2;
    private final static String authority = "com.android.settings.search";
    private static UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        matcher.addURI(authority, "prefs_index", INDEX);
        matcher.addURI(authority, "init", INIT);
        matcher.addURI(authority, "site_map", SITEMAP);
    }

    private SQLiteDatabase database;
    private DatabaseIndexingManager mDatabaseIndexingManager;
    SearchFeatureProvider mSearchFeatureProvider;
    SiteMapManager smm;

    @Override
    public boolean onCreate() {
        database = IndexDatabaseHelper.getInstance(getContext()).getReadableDatabase();
        if (database == null) {
            Log.w("HQTEST", "Cannot indexDatabase Index as I cannot get a readable database");
        }
        //Log.i("HQTEST", Thread.currentThread().getName());     
        mSearchFeatureProvider = FeatureFactory.getFactory(getContext()).getSearchFeatureProvider();
        mDatabaseIndexingManager = mSearchFeatureProvider.getIndexingManager(getContext());
        smm = mSearchFeatureProvider.getSiteMapManager();

        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
       // Log.i("HQTEST", Thread.currentThread().getName());
        String table = getTableName(uri);
        Cursor cursor = null;
        if (table.equals("init")) {
            mDatabaseIndexingManager.indexDatabase(null);
            return null;
        }else if (table.equals("prefs_index")) {
            return database.query("prefs_index", projection, selection, selectionArgs, null, null, sortOrder);
        } else if (table.equals("site_map")) {
            cursor = database.query("prefs_index", projection, selection, selectionArgs, null, null, sortOrder);
        } else {
            Log.w("HQTEST", "table = null");
        }

        Cursor cursor2 = new CursorWrapper(cursor);

        String[] columnNames = new String[]{"1","icon","bread_crumbs","4","5","6","7"};
        MatrixCursor matrixCursor = new MatrixCursor(columnNames);
        Object[] rowData;
        String screenTitle;
        String iconUri;
        String screenClass;
        List<String> breadcrumbsArray;
        String breadcrumbs;
        String title;
        while (cursor != null && cursor.moveToNext()){
            screenTitle = cursor.getString(1);
            iconUri = cursor.getString(2);
            screenClass = cursor.getString(6);
            breadcrumbsArray = smm.buildBreadCrumb(getContext(),screenClass,screenTitle);
            breadcrumbs = breadcrumbsArray!=null ? breadcrumbsArray.toString() : null;
            //Log.e("HQTEST", "list=" + smm.buildBreadCrumb(getContext(),screenClass,screenTitle) + " icon=" + iconUri);
            if (iconUri==null || iconUri.equals("") || iconUri.equals("0")) {
                SearchIndexableResource res = SearchIndexableResources.getResourceByName(screenClass);
                if(res != null && res.iconResId != 0){
                   // Log.e("HQTEST", "res.iconResId=" + res.iconResId);
                    iconUri=res.iconResId + "";
                    title="";
                } else {
                    //Log.e("HQTEST", "res.iconResId=" + R.drawable.ic_settings);
                    title="nores";
                    iconUri=R.drawable.ic_settings + "";
                }
            } else title="";
            rowData = new Object[]{title, iconUri, breadcrumbs,"","","",""};

            matrixCursor.addRow(rowData);
        }

        return new MergeCursor(new Cursor[] {cursor2, matrixCursor}); //matrixCursor;
    }

    private String getTableName(Uri uri) {
        int code = matcher.match(uri);
        switch (code) {
            case INDEX:
                return "prefs_index";
            case INIT:
                return "init";
            case SITEMAP:
                return "site_map";
            default:
                return "init";
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
