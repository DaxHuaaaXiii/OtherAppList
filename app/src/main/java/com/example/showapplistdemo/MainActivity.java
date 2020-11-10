package com.example.showapplistdemo;


import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    List<AppInfo> mList = new ArrayList<>();  //存储包名
    AppInfoAdapter mAdapter;
    TextView textView;
    PackageInfo packageInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);

        getData();
        listView = findViewById(R.id.activity_app_info_list);
        textView = findViewById(R.id.item_name);

        mAdapter = new AppInfoAdapter(this, mList);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mAdapter = (AppInfoAdapter) listView.getAdapter();
                AppInfo appInfo = (AppInfo) mAdapter.getItem(position);

                Intent intent;
                intent = getPackageManager().getLaunchIntentForPackage(appInfo.getPackageName());
                if (intent != null){
                    startActivity(intent);
                }else{
                    Toast.makeText(MainActivity.this,"这个是系统插件，匹配不对，不会跳转",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


    public void getData() {
        //获取所有已经程序的包信息
        List<PackageInfo> packageInfos = getPackageManager().getInstalledPackages(0);
        if (packageInfos != null) {
            for (int i = 0; i < packageInfos.size(); i++) {
                packageInfo = packageInfos.get(i);

                if ((ApplicationInfo.FLAG_SYSTEM & packageInfo.applicationInfo.flags) != 0){
                    continue;
                }
                AppInfo appInfo = new AppInfo();
                appInfo.setAppName(packageInfo.applicationInfo.loadLabel(getPackageManager()).toString());
                appInfo.setPackageName(packageInfo.packageName);
//            appInfo.setVersionName(packageInfo.versionName);
//            appInfo.setClassName(packageInfo.applicationInfo.name);
                appInfo.setIcon(packageInfo.applicationInfo.loadIcon(getPackageManager()));
//            appInfo.getPackageName();

                mList.add(appInfo);
            }
        }
    }


}
