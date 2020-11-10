package com.by.bledemo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ScreenCaputre.ScreenCaputreListener{

    public static MainActivity mainActivity;
    private final String MainTAG = MainActivity.class.getSimpleName();
    private Handler mHandler = new Handler();
    private boolean mScanning;
    // ************************************** 常量
    // 服务 - 云台的服务UUID
    final UUID UUID_SMART_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    // 特征 - 用于发送数据到设备
    final UUID UUID_SMART_ChARA_WRITE = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    // 特征 - 用于接收设备推送的数据
    final UUID UUID_SMART_ChARA_NOTIFICATION = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb");
    // 描述 - 接收特征的描述
    final UUID UUID_SMART_ChARA_NOTIFICATION_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    // 字节 - 云台发送过来的数据的前缀字节 - 握手类型
    final byte CMD_PrefixByte_HandData = (byte)0xFF;
    // 字节 - 云台发送过来的数据的前缀字节 - 指令数据
    final byte CMD_PrefixByte_NormalData = (byte)0x55;

    // ************************************** 蓝牙相关管理
    // 移动设备的本地的蓝牙适配器, 通过该蓝牙适配器可以对蓝牙进行基本操作,
    private BluetoothAdapter mBluetoothAdapter;
    // 当前设备的蓝牙操作管理对象
    private BluetoothGatt mBluetoothGatt;
//    // 云台指定的服务
//    private BluetoothGattService smart_Service;
//    // 云台指定的通知/读取的特征
//    private BluetoothGattCharacteristic smart_noticeCharacteristic;
    // 云台指定的写入的特征
    public BluetoothGattCharacteristic smart_writeCharacteristic;
    // ************************************** 组件
    private TextView deviceName;
    // 开始扫描按钮
    private Button start_scan_Btn;
    // 停止扫描按钮
    private Button stop_scan_Btn;
    // 连接按钮
    private Button connectBtn;
    // 断开连接并释放按钮
    private Button releaseBtn;
    // 发送数据按钮
    private Button sendDataBtn;
    // ************************************** 其他
    // 可变数组
    private ArrayList<String> deviceFindedMacArray;




    private ScreenCaputre screenCaputre;

    private static final int REQUEST_CODE = 2;//开启录屏
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mediaProjection;
    private ImageView imageView;


    //跳转
    ListView listView;
    List<AppInfo> mList = new ArrayList<>();  //存储包名
    AppInfoAdapter mAdapter;
    TextView textView;
    PackageInfo packageInfo;


    private PackageManager packageManager;


    // ******************************************** 系统回调函数

    // 构造函数
    public MainActivity() {
        mainActivity = this;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //-----------------------------------------------------
        //绑定后台无声音乐服务
        Intent intent = new Intent(this,LiveService.class);
        startService(intent);

        //------------------------------------------------------

        int intA = -5;
        short shorA = -5;//(short)intA;
        Log.e(MainTAG, "shorA: " + shorA );

        String binaryA =  Integer.toBinaryString(shorA);
        Log.e(MainTAG, "binaryA: " + binaryA );

        //------------------------------------------------------
        //录屏
        imageView = findViewById(R.id.imageview3);
        if(!this.isIgnoringBatteryOptimizations()){
            this.requestIgnoreBatteryOptimizations();

        }

        //------------------------------------------------------

        Log.d(MainTAG, "开始");

        // 如果系统版本大于23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 动态申请权限

            // 粗略位置定位权限申请
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }

            // 声音权限申请
            if (this.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }

        }

        connectBtn = findViewById(R.id.connect_btn);
        releaseBtn = findViewById(R.id.release_btn);
        sendDataBtn = findViewById(R.id.sendData_btn);

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String deviceMac = deviceFindedMacArray.get(0);
                // 连接设备
                connectToDevice(deviceMac);
            }
        });
//
//
        releaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 停止连接 释放资源
                releaseBlueTooth(null);
            }
        });
//
        sendDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(MainTAG, "点击发送数据");
                // 发送数据
                sendFollowData(200, 0, 20);
            }
        });


        // 初始化数组
        deviceFindedMacArray = new ArrayList<String>();

        // 获取全局蓝牙管理类
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        // 获取本地的蓝牙适配器对象(基本所有操作都是基于它的)
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // 如果不支持蓝牙
        if(mBluetoothAdapter == null){
            Log.d(MainTAG, "设备应该不支持蓝牙功能，不做操作");
            return;
        }

        // 如果蓝牙未开启，向用户申请开启
        if (!mBluetoothAdapter.isEnabled()) {
            // Toast.makeText(this,"请同意打开蓝牙",Toast.LENGTH_LONG).show();
            // 发送蓝牙开启申请
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);

        // 如果已经开启蓝牙
        }else{

        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(MainTAG, "执行 - onActivityResult");
        // 如果是蓝牙开启相关
        if(requestCode == 1){
            // 如果是已经开启蓝牙
            if(resultCode == Activity.RESULT_OK){

            }else{
                Log.d(MainTAG, "用户未同意开启蓝牙，不做任何操作");
                finish();
            }
        }


        if (resultCode != RESULT_OK || requestCode != REQUEST_CODE) return;
        mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            return;
        }
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        float density = dm.density;
        int screenWidth = (int) (width/density);
        int screenHeight = (int)(height/density);
        screenCaputre = new ScreenCaputre(screenWidth, screenHeight, mediaProjection,mainActivity);
        screenCaputre.setScreenCaputreListener(this);
        screenCaputre.start();

    }

    // 用户确认权限后回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    // ******************************************** 开始扫描设备 / 停止扫描设备

    // 开始扫描设备
    public void startScanDevice(View view){
        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
            Log.d(MainTAG, "蓝牙 - 开始扫描异常");
            return;
        }

        if(mBluetoothAdapter.isDiscovering()){
            // mBluetoothAdapter.cancelDiscovery();
            Log.d(MainTAG, "蓝牙 - 已经在扫描中");
            return;
        }

        // 监听找到蓝牙设备的广播
        // 为了发现可用的蓝牙的设备，必须在应用中注册ACTION_FOUND的广播
        registerReceiver(bluetoothDeviceHadFindReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        new Thread(){
            @Override
            public void run() {
                //需要在子线程中处理的逻辑
                // 开始扫描设备
                mBluetoothAdapter.startDiscovery();
            }
        }.start();

    }

    // 停止扫描
    public void stopScanDevice(View view){
        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
            Log.d(MainTAG, "蓝牙 - 停止扫描异常");
            return;
        }

        Log.d(MainTAG, "蓝牙 - 停止扫描");

        // 取消监听广播
        unregisterReceiver(bluetoothDeviceHadFindReceiver);

        // 停止扫描设备
        mBluetoothAdapter.cancelDiscovery();

    }

    // 如果查找到可用的设备会触发这个广播，这个广播中带有EXTRA_DEVICE的设备信息
    // BroadcastReceiver 是广播接收器，Android四大组件之一，没有可视化界面，用于不同组件和多线程之间的通信
    final BroadcastReceiver bluetoothDeviceHadFindReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 获取是什么类型的广播
            String action = intent.getAction();
            // 如果是找到可用的蓝牙设备
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                // 获取可用的蓝牙设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 设备的名称
                String deviceName = device.getName();
                // 设备的mac地址
                String deviceAddress =  device.getAddress();

                Log.d(MainTAG, "有设备： ");

                // 没有名称的直接返回
                if(deviceName == null){ return; }
                // 如果不是指定设备，直接返回
                if(!deviceName.equals("stalibizer_E001")){ return; } // 必须使用stalibizer_E003这个名字过滤，不要使用Smart XR，不然会连不上设备

                boolean isHadFoundSameDevice = false;
                for (int i = 0; i < deviceFindedMacArray.size(); i++){
                    String deviceMacAddress = deviceFindedMacArray.get(i);
                    if(deviceMacAddress.equals(deviceAddress)){
                        isHadFoundSameDevice = true;
                    }
                }

                if(isHadFoundSameDevice){ return; }

                // 添加到可变数组
                deviceFindedMacArray.add(device.getAddress());

                Log.d(MainTAG, "发现设备： " + device.getName() + " 设备的mac地址: " + deviceAddress + "  当前发现设备总数： " + deviceFindedMacArray.size());



////                // 停止扫描
//                stopScanDevice();

//                // 连接设备
//                connectToDevice(device);
            }
        }
    };

    // ******************************************** 连接 / 断开

    // 连接设备
    public void connectToDevice(final String mac){
        if(mac == null){
            Log.d(MainTAG, "蓝牙 - 连接设备异常 - mac地址为空");
            return;
        }

        // 根据mac地址获取蓝牙设备实例对象，当然此处的bluetoothDevice也可以通过蓝牙扫描得到
        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(mac);

        // 如果蓝牙设备为空
        if(bluetoothDevice == null){
            Log.d(MainTAG, "蓝牙 - 连接设备异常 - bluetoothDevice为空");
            return;
        }

//                // gatt为全局变量，获取当前gatt服务下连接的设备
//                if(mBluetoothGatt != null && mBluetoothGatt.getConnectedDevices() != null){
//                    for(BluetoothDevice device : mBluetoothGatt.getConnectedDevices()){
//                        if(device.getAddress().equals(mac)){  // 如果当前遍历出的连接设备与我们需要连得设备是同一设备
//                            mBluetoothGatt.disconnect(); //先去断开之前未正常断开的连接，解决连接133的问题
//                        }
//                    }
//                    mBluetoothGatt.close(); //释放gatt服务
//                    mBluetoothGatt = null;
//                }

        // 如果系统版本大于23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            Log.d(MainTAG, "蓝牙 - 系统大于23");

            // 连接设备，autoConnect参数为true代表自动连接设备
            /*
             * 8.0的系统使用
             * bluetoothDevice.connectGatt(MainActivity.this, true, gattCallback);
             * 总是连接失败，提示status返回133，用了各种方法都不行，
             * 后台一查才发现6.0及以上系统的手机要使用
             * bluetoothDevice.connectGatt(MainActivity.this,true, gattCallback, TRANSPORT_LE)，
             * 其中TRANSPORT_LE参数是设置传输层模式。传输层模式有三种TRANSPORT_AUTO 、TRANSPORT_BREDR 和TRANSPORT_LE。
             * 如果不传默认TRANSPORT_AUTO，6.0系统及以上需要使用TRANSPORT_LE这种传输模式，具体为啥，我也不知道，
             * 我猜是因为Android6.0及以上系统重新定义了蓝牙BLE的传输模式必须使用TRANSPORT_LE这种方式吧。
             * */
            mBluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, false, deviceConnectGattCallback, BluetoothDevice.TRANSPORT_LE); // BluetoothDevice.TRANSPORT_LE
        }else{
            // 连接设备，autoConnect参数为true代表自动连接设备
            mBluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, false, deviceConnectGattCallback);
        }


        // 如果连接蓝牙设备异常
        if(mBluetoothGatt == null){
            Log.d(MainTAG, "蓝牙 - 连接设备异常 - mBluetoothGatt为空");
            return;
        }

        Log.d(MainTAG, "蓝牙 - 开始连接设备");

        // 开始连接
        mBluetoothGatt.connect();


    }

    // 断开连接
    public void releaseBlueTooth(BluetoothGatt gatt){
        // 删除所有存储的设备
        deviceFindedMacArray.clear();
        Log.d(MainTAG, "蓝牙 - 断开连接");

        if(gatt != null){
            gatt.disconnect();
            gatt.close();
        }

        if(mBluetoothGatt != null){
            refreshGattCache(mBluetoothGatt);
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            Log.d(MainTAG, "蓝牙 - 释放mBluetoothGatt");
        }
    }

    // 刷新缓存
    public static boolean refreshGattCache(BluetoothGatt gatt) {
        boolean result = false;
        try {
            if (gatt != null) {
                Method refresh = BluetoothGatt.class.getMethod("refresh");
                if (refresh != null) {
                    refresh.setAccessible(true);
                    result = (boolean) refresh.invoke(gatt, new Object[0]);
                }
            }
        } catch (Exception e) {
        }
        return result;
    }


    // ******************************************** 发送数据



    // ******************************************** 设备连接状态回调管理类

    private BluetoothGattCallback deviceConnectGattCallback = new BluetoothGattCallback() {

        @Override
        // 蓝牙设备连接状态改变回调
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(MainTAG, "BluetoothGattCallback管理类 - onConnectionStateChange");

            /**
             * newState
             * 0、STATE_DISCONNECTED    状态为已断开
             * 1、STATE_CONNECTING      状态为连接中
             * 2、STATE_CONNECTED       状态为已连接
             * 3、STATE_DISCONNECTING   状态为断开中
             * */
            Log.d(MainTAG, "新连接状态: " + newState + " 状态码： " + status);

            if (status != BluetoothGatt.GATT_SUCCESS){
                // 连接 失败
                Log.e(MainTAG,"连接失败 " + status); // Log.e 日志输出是红色
            }

            if (newState == BluetoothGatt.STATE_CONNECTED) {             // 当蓝牙设备已经连接
                Log.d(MainTAG, "onConnectionStateChange: " + "蓝牙连接成功");

                // 开始扫描服务 // discoverServices ：异步操作，服务发现完成后触发onServiceDiscovered，发现成功的话使用getService检索远程服务
                boolean isStartScanServicesSuccess = mBluetoothGatt.discoverServices();
                Log.i(MainTAG, "开始扫描服务 : " + isStartScanServicesSuccess);


            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {  // 当设备无法连接
                Log.d(MainTAG, "onConnectionStateChange: " + "蓝牙已断开");

                // 释放连接对象
                releaseBlueTooth(gatt);

            } else if (newState == BluetoothGatt.STATE_CONNECTING) {     // 当设备无法连接
                Log.d(MainTAG, "onConnectionStateChange: " + "蓝牙连接中");


            } else if (newState == BluetoothGatt.STATE_DISCONNECTING) {  // 当设备无法连接
                Log.d(MainTAG, "onConnectionStateChange: " + "蓝牙断开中");
            }

        }


        // 发现新服务的回调
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(MainTAG,"BluetoothGattCallback管理类 - onServicesDiscovered");

            // 如果发现服务成功
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(MainTAG,"服务 - 服务状态异常 " + status);
                return;

            }

            Log.d(MainTAG, "---------------- 发现服务 ----------------- ");

            // 云台指定的服务
            BluetoothGattService smart_Service = null;
            // 云台指定的通知/读取的特征
            BluetoothGattCharacteristic smart_noticeCharacteristic = null;

            // 获取所有发现的服务
            List<BluetoothGattService> serviceArray = mBluetoothGatt.getServices();
            // 遍历服务
            for (BluetoothGattService serviceItem : serviceArray){
                // 获取服务的唯一标识
                UUID serviceUuid = serviceItem.getUuid();
                Log.d(MainTAG, "服务 -------- 发现服务 -------- " + serviceUuid.toString());

                // compareTo: 小于返回-1，等于返回0，大于返回1

                // 如果服务UUID和云台的服务UUID一致
                if(serviceUuid.compareTo(UUID_SMART_SERVICE) == 0){
                    // 保存到全景变量
                    smart_Service =  serviceItem;
                }else{
                    // 如果不是，直接跳过循环末尾
                    continue;
                }

                // 获取当前服务里的所有特征
                List<BluetoothGattCharacteristic> characteristicsArray = serviceItem
                        .getCharacteristics();
                // 遍历特征
                for (BluetoothGattCharacteristic characteristicItem : characteristicsArray) {
                    // 获取特征的唯一标识
                    UUID characteristicUuid = characteristicItem.getUuid();
                    Log.d(MainTAG, "服务 - 特征 - " + characteristicUuid.toString());

                    // 如果服务UUID和云台的通知/读取UUID一致
                    if(characteristicUuid.compareTo(UUID_SMART_ChARA_NOTIFICATION) == 0){
                        // 保存到全景变量
                        smart_noticeCharacteristic =  characteristicItem;

                    }else if(characteristicUuid.compareTo(UUID_SMART_ChARA_WRITE) == 0){
                        // 保存到全景变量
                        smart_writeCharacteristic =  characteristicItem;
                    }

//                     //遍历所有描述
//                     List<BluetoothGattDescriptor> descriptorArray = characteristicItem.getDescriptors();
//                     for (BluetoothGattDescriptor descriptorItem : descriptorArray){
//                        Log.d(MainTAG, "服务 - 描述 - " + descriptorItem.getUuid().toString());
//                    }
                }

            }

            Log.d(MainTAG, "---------------- 发现服务 结束 ----------------- ");

            // 如果有服务或者特征没有找到
            if(smart_Service == null || smart_noticeCharacteristic == null || smart_writeCharacteristic == null){
                Log.d(MainTAG, "蓝牙 - 寻找云台的服务和特征 - 异常 ");
                return;
            }

            Log.d(MainTAG, "蓝牙 - 已经找到云台相关的服务和特征 ✨✨ ");

            // 订阅通知
            enableNotification(gatt,smart_noticeCharacteristic,UUID_SMART_ChARA_NOTIFICATION_DESCRIPTOR,true );

        }


        @Override
        // 特征读取
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(MainTAG,"BluetoothGattCallback管理类 - onCharacteristicRead");
        }

        @Override
        // 描述写入状态回调
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(MainTAG,"--------------------------------");
            Log.d(MainTAG,"BluetoothGattCallback管理类 - onDescriptorWrite - 描述写入状态回调");
            // 获取写入的数据
            byte[] value = descriptor.getValue();
            // 转换成字符串
            String byteString = byte2hex(value);
            Log.d(MainTAG,"描述写入数据 - " + byteString);
            Log.d(MainTAG,"--------------------------------");


            // 如果是 订阅通知特征的描述 写入成功
            if(descriptor.getUuid().compareTo(UUID_SMART_ChARA_NOTIFICATION_DESCRIPTOR) == 0 && status == BluetoothGatt.GATT_SUCCESS){
                Log.d(MainTAG,"蓝牙 - 描述写入成功");
//                // 发送第一次握手数据，这次握手会有数据返回，会回调，onCharacteristicChanged。第二次发送握手数据则不会有返回。
                sendCmdData(hand_deviceHandshakeFirstCmd());
            }else{
                Log.d(MainTAG,"蓝牙 - 描述写入失败");
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(MainTAG,"BluetoothGattCallback管理类 - onDescriptorRead");
        }

        @Override
        // 特征写入状态回调
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(MainTAG,"--------------------------------");
            Log.d(MainTAG,"BluetoothGattCallback管理类 - onCharacteristicWrite - 特征写入状态回调");
            // 获取写入的数据
            byte[] value = characteristic.getValue();
            // 转换成字符串
            String byteString = byte2hex(value);
            Log.d(MainTAG,"特征写入数据 - " + byteString);
            Log.d(MainTAG,"--------------------------------");

            // 如果是第二次握手数据写入成功
            if(byteEqual(value, hand_deviceHandshakeSecondCmd())){
                // 发送第三次握手数据
                sendCmdData(hand_deviceHandshakeThirdCmd());
            }



        }

        @Override
        // 接受数据
        public final void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) { //characteristic改变，数据接收
            byte[] value = characteristic.getValue();
            Log.d(MainTAG,"--------------------------------");
            Log.d(MainTAG,"BluetoothGattCallback管理类 - onCharacteristicChanged - 收到数据");
            // 返回的数字转换成字符串
            String byteString = byte2hex(value);
            Log.d(MainTAG,"蓝牙返回数据 - " + byteString);
            Log.d(MainTAG,"--------------------------------");


            // 如果是握手相关数据
            if(value[0] == CMD_PrefixByte_HandData && value[1] == CMD_PrefixByte_HandData){
                // 如果是发送第一次握手后的反馈
                if(value.length == 16){   // 这里应该也可以使用 ff ff 00 0c 02 30 30 30 32 62 02 24 20 e3 ed 57 判等
                    Log.d(MainTAG,"蓝牙 - 第一次握手成功");
                    // 发送第二次握手数据(第二次握手数据和第三次握手数据要一起发送，但要第二次握手数据发送成功后再发送第三次握手数据)
                    sendCmdData(hand_deviceHandshakeSecondCmd());

                }else if(value.length == 6){ // 这里应该也可以使用  ff ff 00 02 09 3c 判等
                    Log.d(MainTAG,"蓝牙 - 第三次握手成功 - 全部握手成功");
                    Log.d(MainTAG,"蓝牙 - 现在可以发送操作指令数据了");
                    // 到这里应该是完全握手成功了
                    // 现在可以发送操作命令了
                }
            }
        }

    };



    /*
    * 设置是否监听特征的内容改变. 这一步必须要有 否则收不到通知
    * gatt  当前设备蓝牙连接管理对象
    * characteristic  特征
    * descriptor      描述
    * enableNotification  是否开启监听
    * */
    private void enableNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, UUID descriptor, boolean enableNotification) {
        if (gatt == null || characteristic == null) {
            Log.d(MainTAG, "蓝牙 - 设置监听失败 - 参数异常");
            return;
        }

        // 获取特征指定的描述
        BluetoothGattDescriptor characteristicDescriptor = characteristic.getDescriptor(descriptor);

        if (characteristicDescriptor == null) {
            Log.d(MainTAG, "蓝牙 - 获取指定描述类 - 异常");
            return;
        }

        byte[] value;
        if(enableNotification){
            // 开启订阅/监听
            value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
        }else{
            // 关闭订阅/监听
            value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        }
        // 把监听参数设置到描述类里
        characteristicDescriptor.setValue(value);

        boolean isWriteSuccess =  gatt.writeDescriptor(characteristicDescriptor);
        boolean isSetNotificationSuccess = gatt.setCharacteristicNotification(characteristic, enableNotification);

        if(isWriteSuccess && isSetNotificationSuccess){
            Log.d(MainTAG, "蓝牙 - 订阅指定特征 - 成功");
        }else{
            Log.d(MainTAG, "蓝牙 - 订阅指定特征 - 失败");
        }
    }

    // ******************************************** 自定义接口发送数据





    /**
     * 发送中心跟随指令  0x23指令
     * @param xOffset x轴偏移量 [-1000,1000] 正为顺时针
     * @param yOffset y轴偏移量 [-1000,1000] 正为顺时针
     * @param speed      跟随速度 [10-100]  档位
     */
    public void sendFollowData(int xOffset, int yOffset, int speed){




        byte[] xByte = intToTwoLengthByte(xOffset);
        byte[] yByte = intToTwoLengthByte(yOffset);
        byte speedByte = (byte)speed;

        byte[] appendData = new byte[xByte.length + yByte.length + 1];

        // byte[0]是低位
        appendData[0] = speedByte;
        appendData[1] = yByte[0];
        appendData[2] = yByte[1];
        appendData[3] = xByte[0];
        appendData[4] = xByte[1];

        // 最终的数据
        byte[] data = createCmd((byte)0x02,(byte)0x0b,(byte)0x23, appendData, (byte) appendData.length);

//        String aaa = byte2hex(xByte);
//        String bbb = String.format("%x",xOffset);
//
//        Log.e(MainTAG, "aaa: " + aaa + "   bbb: " + bbb );

        // 发送数据
         sendCmdData(data);
    }

    // ******************************************** 握手处理
    /**
     * 第一次握手命令 FF FF 00 02 01 F7
     */
    private byte[] hand_deviceHandshakeFirstCmd(){
        return new byte[]{(byte)0xFF,(byte)0xFF,(byte)0x00,(byte)0x02,(byte)0x01,(byte)0xF7};
    }

    /**
     * 第二次握手命令 FF FF 00 08 03 47 57 31 30 00 30 AC
     */
    private byte[] hand_deviceHandshakeSecondCmd(){
        return new byte[]{(byte)0xFF,(byte)0xFF,(byte)0x00,(byte)0x08,(byte)0x03,(byte)0x47,(byte)0x57,(byte)0x31,(byte)0x30,(byte)0x00,(byte)0x30,(byte)0xAC};
    }
    /**
     * 第三次握手命令 FF FF 00 02 08 B1
     */
    private byte[] hand_deviceHandshakeThirdCmd(){
        return new byte[]{(byte)0xFF,(byte)0xFF,(byte)0x00,(byte)0x02,(byte)0x08,(byte)0xB1};
    }


    // ******************************************** 数据底层处理

    // 发送数据
    public void sendCmdData(byte[] data){
        if(smart_writeCharacteristic == null){
            Log.d(MainTAG, "蓝牙 - 发送数据异常 - 特征对象为空");
            return;
        }

        if(data == null){
            Log.d(MainTAG, "蓝牙 - 发送数据异常 - 发送数据为空");
            return;
        }

        // 发送数据
        smart_writeCharacteristic.setValue(data);
        mBluetoothGatt.writeCharacteristic(smart_writeCharacteristic);
    }

    /**
     * 构造完整指令数据  0x23指令
     * @param pack    目的地址
     * @param source  源地址
     * @param func    功能码
     * @param appendData  用户要添加的数据
     * @param appendDataLength 用户要添加的数据的长度
     */
    private byte[] createCmd(byte pack, byte source, byte func, byte[] appendData, byte appendDataLength ){
        // 字节总数
        int sumDataLength = 1 + 1 + 1 + 1 + 1 + 1 + appendDataLength + 1;

        Log.d(MainTAG, "数据 - 开始拼接数据 - 数据长度  " + sumDataLength);

        byte[] dataBytes = new byte[sumDataLength];


        int i = -1;
        dataBytes[++i] = 0x55;
        dataBytes[++i] = pack;
        dataBytes[++i] = source;
        dataBytes[++i] = func;
        dataBytes[++i] = appendDataLength;
        dataBytes[++i] = appendDataLength; // 执行完这轮 i是5

        Log.d(MainTAG, "数据 -  i:  " + i);


        // 用户的数据拼接上去
        for (int index = appendDataLength - 1; index >= 0; index--){
            byte value = appendData[index];
            dataBytes[++i] = value;
        }

        Log.d(MainTAG, "数据 -  i:  " + i);


        // 计算校验码 // 从目的地址到检验码之前的字节的内容的总和
        byte verifyValue = 0x00; // byte verifyValue = 0; 应该是一样的
        for (int index = 1; index <= sumDataLength - 2; index++){
            byte value = dataBytes[index];
            verifyValue += value;
        }

        Log.d(MainTAG, "数据 - " + verifyValue + "  i:  " + i);
        dataBytes[++i] = verifyValue;

        String hexString = byte2hex(dataBytes);

        Log.d(MainTAG, "数据 - 结束拼接数据 - 数据  " + hexString);

        return dataBytes;
    }



    // 整数转2个字节
    public byte[] intToTwoLengthByte(int val){
        byte[] b = new byte[2];
        b[1] = (byte)(val  & 0x000000ff);
        b[0] = (byte)((val >> 8) & 0x000000ff);

        return b;
    }


    // 字节数组判等
    public boolean byteEqual(byte[] aBytes, byte[] bBytes){
        if(aBytes == null && bBytes == null){ return true; }
        if(aBytes == null && bBytes != null){ return false; }
        if(aBytes != null && bBytes == null){ return false; }
        if(aBytes.length != bBytes.length)  { return false; }

        // 是否至少存在一个不相等
        boolean isHadNoEqual = false;
        for (int i = 0; i < aBytes.length; i++){
            if(aBytes[i] != bBytes[i]){ isHadNoEqual = true; break; }
        }
        if(isHadNoEqual){ return false; } else { return true; }
    }

    // 字节数组转换成16进制内容的字符串
    public String byte2hex(byte[] bytes){
        if(bytes == null) { return ""; }
        StringBuilder sb = new StringBuilder();
        String tmp = null;
        for(byte b: bytes){

            //将每个字节与0xFF进行与运算，然后转化为10进制，然后借助于Integer再转化为16进制
            tmp = Integer.toHexString(0xFF & b);
            if(tmp.length() == 1){
                tmp = "0" + tmp;
            }
            sb.append(tmp);
        }
        return sb.toString();
    }





    //-------------------------------------------------
    //录屏
    public void start(View view) {
        if (null == screenCaputre) {
            prepareScreen();
            if (REQUEST_CODE == 2){

            }



        } else {
            screenCaputre.start();
        }

    }


    public void stop(View view) {
        screenCaputre.stop();
        screenCaputre = null;
    }

    public void prepareScreen() {
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE);

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



    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onImageBitmap(Bitmap bitmap) {
        // Log.v(MainTAG, "123  ");
        this.imageView.setImageBitmap(bitmap);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)

    // 判断我们的应用是否在白名单中
    private boolean isIgnoringBatteryOptimizations() {

        boolean isIgnoring = false;

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if(powerManager != null) {

            isIgnoring = powerManager.isIgnoringBatteryOptimizations(getPackageName());

        }

        return isIgnoring;

    }



    @RequiresApi(api = Build.VERSION_CODES.M)
    // 如果不在白名单中，可以通过以下代码申请加入白名单
    public void requestIgnoreBatteryOptimizations() {

        try{

            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);

            intent.setData(Uri.parse("package:"+ getPackageName()));

            startActivity(intent);

        } catch(Exception e) {

            e.printStackTrace();

        }

    }

}
