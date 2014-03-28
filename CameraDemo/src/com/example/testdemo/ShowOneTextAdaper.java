
package com.example.testdemo;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

public class ShowOneTextAdaper extends BaseAdapter {
    private Context mContext;
    private List<? extends ShowTextBean> mData;
    private int itemRes;

    public ShowOneTextAdaper(Context context, int itemRes, List<? extends ShowTextBean> mData) {
        mContext = context;
        this.mData = mData;
        this.itemRes = itemRes;
    }

    public void setData(List<ShowTextBean> mData) {
        this.mData = mData;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public ShowTextBean getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(itemRes, null);
        }
        try {
            // If no custom field is assigned, assume the whole resource is
            // a TextView
            TextView text = (TextView) convertView;
            ShowTextBean item = getItem(position);
            text.setText(item.getShowText());
        } catch (ClassCastException e) {
            Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
            throw new IllegalStateException(
                    "ArrayAdapter requires the resource ID to be a TextView", e);
        }

        return convertView;
    }

}
