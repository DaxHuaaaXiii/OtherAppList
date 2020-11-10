package com.example.showapplistdemo;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class AppInfoAdapter extends BaseAdapter {


    private List<AppInfo> mList;
    private Context mContext;
    private LayoutInflater mLayoutInflater;

    public AppInfoAdapter(Context context,List<AppInfo> list)
    {
        this.mContext = context;
        this.mList = list;
        this.mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        AppInfo appInfo = mList.get(position);

        View view = null;
        AppInfoHolder appInfoHolder;
        if (convertView == null)
        {
            view = mLayoutInflater.inflate(R.layout.item_app_info,parent,false);
            appInfoHolder = new AppInfoHolder();
            appInfoHolder.imageView = (ImageView) view.findViewById(R.id.item_image);
            appInfoHolder.textView = (TextView) view.findViewById(R.id.item_name);


            view.setTag(appInfoHolder);
            convertView = view;
        }else {
            appInfoHolder = (AppInfoHolder) convertView.getTag();
        }

        appInfoHolder.imageView.setImageDrawable(appInfo.getIcon());
        appInfoHolder.textView.setText(appInfo.getAppName());


        return convertView;
    }

    private class AppInfoHolder{
        ImageView imageView;
        TextView textView;
    }


}
