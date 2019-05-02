package tct.func.adapter;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.settings.R;
import tct.func.FuncUtilSettings;
import tct.func.FuncSettings.ShortcutsItem;
public class DragAdapter extends BaseAdapter {
    private final static String TAG = "DragAdapter";
    private boolean isItemShow = false;
    private Context context;
    private Resources mRes;
    private int holdPosition;
    private boolean isChanged = false;
    boolean isVisible = true;
    public List<ShortcutsItem> listData;
    private TextView item_text;
    public int remove_position = -1;
    private FuncEditItemListener funcEditItemListener;
    private String func_start_music_playlistStr;
    private String func_navigate_homeStr;

    public DragAdapter(Context context, List<ShortcutsItem> listData) {
        this.context = context;
        this.listData = listData;
        mRes = context.getResources();
        func_start_music_playlistStr = mRes
                .getString(R.string.func_start_music_playlist);
        func_navigate_homeStr = mRes.getString(R.string.func_navigate_home);
    }

    @Override
    public int getCount() {
        return listData == null ? 0 : listData.size();
    }

    @Override
    public ShortcutsItem getItem(int position) {
        if (listData != null && listData.size() != 0
                && position < listData.size()) {
            return listData.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolderNormal holderNormal = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.drag_listview_item, null);
            holderNormal = new ViewHolderNormal();
            holderNormal.funcIcon = (ImageView) convertView
                    .findViewById(R.id.funcicon);
            holderNormal.funcText = (TextView) convertView
                    .findViewById(R.id.functext);
            holderNormal.fun_editIcon = (ImageView) convertView
                    .findViewById(R.id.fun_edit);
            holderNormal.func_removeIcon = (ImageView) convertView
                    .findViewById(R.id.click_remove);
            convertView.setTag(holderNormal);
        } else {
            holderNormal = (ViewHolderNormal) convertView.getTag();
        }
        int id = (int) listData.get(position).getId();
        String packageName = (String) listData.get(position).getPackageName();
        int type = -1;
        if (id != -1) {
            String appname = FuncUtilSettings.getNameById(context, id);
            holderNormal.funcText.setText(appname);
            //Begin deleted by zengjie for XR5488573 on 11/3/17
            //if (TextUtils.equals(appname, func_start_music_playlistStr)) {
            //                holderNormal.fun_editIcon.setVisibility(View.VISIBLE);
            //                type = FuncUtilSettings.START_MUSIC_PLAYLIST_ID;
            //} else
            //End deleted by zengjie for XR5488573 on 11/3/17
            if (TextUtils.equals(appname, func_navigate_homeStr)) {
                holderNormal.fun_editIcon.setVisibility(View.VISIBLE);
                type = FuncUtilSettings.NAVIGATE_HOME_ID;
                //added by zengjie for task 4264377 on 20170410 begin
            } else if (TextUtils.equals(appname, mRes.getString(R.string.func_call_a_contact))) {
                holderNormal.fun_editIcon.setVisibility(View.VISIBLE);
                type = FuncUtilSettings.CALL_A_CONTACT_ID;
            }//added by zengjie for task 4264377 on 20170410 end
            else {
                holderNormal.fun_editIcon.setVisibility(View.GONE);
            }
        } else {
            // porting from defect 987432 begin
            holderNormal.funcText.setText(FuncUtilSettings.getAppShortcutsName(
                    context, packageName,
                    (String) listData.get(position).getMainClassName()));
            // porting from defect 987432 end
            holderNormal.fun_editIcon.setVisibility(View.GONE);
        }
        if (id != -1) {
            Drawable mDrawable = FuncUtilSettings.getIconById(context, id);
            if (mDrawable != null) {
                holderNormal.funcIcon.setImageDrawable(mDrawable);
            }
        } else {
            // porting from defect 987432 begin
            Drawable drawable = FuncUtilSettings.getAppShortcutsDrawable(
                    context, (String) listData.get(position).getPackageName(),
                    (String) listData.get(position).getMainClassName());
            // porting from defect 987432 end
            if (drawable != null)
                holderNormal.funcIcon.setImageDrawable(drawable);
        }
        final int clickposition = position;
        final int goType = type;
        holderNormal.fun_editIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (funcEditItemListener != null && goType != -1) {
                    funcEditItemListener.editFuncItem(goType);
                }
            }
        });
        return convertView;
    }

    public void setFuncEditItemListener(
            FuncEditItemListener funcEditItemListener) {
        this.funcEditItemListener = funcEditItemListener;
    }

    static class ViewHolderNormal {
        ImageView funcIcon;
        TextView funcText;
        ImageView fun_editIcon;
        ImageView func_removeIcon;
    }

    public interface FuncEditItemListener {
        void editFuncItem(int type);
    }
}