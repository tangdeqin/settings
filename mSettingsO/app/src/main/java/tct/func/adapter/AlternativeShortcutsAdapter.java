package tct.func.adapter;

import java.util.List;
import java.util.Map;

import com.android.settings.R;
import tct.func.FuncUtilSettings;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import tct.func.FuncSettings.ShortcutsItem;
public class AlternativeShortcutsAdapter extends BaseAdapter {

    private final static String TAG = "DragAdapter";
    private boolean isItemShow = false;
    private Context context;
    private int holdPosition;
    private boolean isChanged = false;
    boolean isVisible = true;
    public List<ShortcutsItem> listData;
    private TextView item_text;
    public int remove_position = -1;
    private FuncAddListener funcAddListener;

    public AlternativeShortcutsAdapter(Context context,
            List<ShortcutsItem> listData) {
        this.context = context;
        this.listData = listData;
    }

    @Override
    public int getCount() {
        return listData == null ? 0 : listData.size();
    }

    @Override
    public ShortcutsItem getItem(int position) {
        if (listData != null && listData.size() != 0) {
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
                    R.layout.alternativeshortcuts_listview_item, null);
            holderNormal = new ViewHolderNormal();
            holderNormal.funcIcon = (ImageView) convertView
                    .findViewById(R.id.funcicon);
            holderNormal.funcText = (TextView) convertView
                    .findViewById(R.id.functext);
            holderNormal.func_addIcon = (ImageView) convertView
                    .findViewById(R.id.func_add);
            convertView.setTag(holderNormal);
        } else {
            holderNormal = (ViewHolderNormal) convertView.getTag();
        }
        int id = (int) listData.get(position).getId();

        if (id != -1) {
            Drawable mDrawable = FuncUtilSettings.getIconById(context, id);
            if (mDrawable != null) {
                holderNormal.funcIcon.setImageDrawable(mDrawable);
            }
        }
        if (id != -1) {
            holderNormal.funcText.setText(FuncUtilSettings.getNameById(context,
                    id));
        }
        final int clickposition = position;
        holderNormal.func_addIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (funcAddListener != null) {
                    funcAddListener.addFuncItem(clickposition);
                }
            }
        });
        return convertView;
    }

    public void setFuncAddListener(FuncAddListener funcAddListener) {
        this.funcAddListener = funcAddListener;
    }

    static class ViewHolderNormal {
        ImageView funcIcon;
        TextView funcText;
        ImageView func_addIcon;
    }

    public interface FuncAddListener {
        void addFuncItem(int position);
    }

}