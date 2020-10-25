/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cqu.ebd.common;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.cqu.ebd.R;
import com.cqu.ebd.ble.DeviceListAdapter;
import com.cqu.ebd.beans.Get_type;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ExplainReasonCallbackWithBeforeParam;
import com.permissionx.guolindev.callback.ForwardToSettingsCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.request.ExplainScope;
import com.permissionx.guolindev.request.ForwardScope;

import java.util.List;
import java.util.Timer;


/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends AppCompatActivity {
    // private LeDeviceListAdapter mLeDeviceListAdapter;
    Get_type mGet_type;
    private BluetoothAdapter mBluetoothAdapter;
    private Activity mContext ;
    private boolean mScanning;
    private Handler mHandler;
    private static String TAG = "DeviceScanActivity";
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 5000;

    private DeviceListAdapter mDevListAdapter;
    ToggleButton tb_on_off;
    TextView btn_searchDev;
    Button btn_aboutUs;
    ListView lv_bleList;
    byte dev_bid;
    Timer timer;
    String APP_VERTION = "1002";



    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
        };

        PermissionX.init((FragmentActivity) mContext)
                .permissions(permissions)
                .onExplainRequestReason(new ExplainReasonCallbackWithBeforeParam() {
                    @Override
                    public void onExplainReason(ExplainScope scope, List<String> deniedList, boolean beforeRequest) {
                        scope.showRequestReasonDialog(deniedList, "即将申请的权限是程序必须依赖的权限", "我已明白");
                    }
                })
                .onForwardToSettings(new ForwardToSettingsCallback() {
                    @Override
                    public void onForwardToSettings(ForwardScope scope, List<String> deniedList) {
                        scope.showForwardToSettingsDialog(deniedList, "您需要去应用程序设置当中手动开启权限", "我已明白");
                    }
                })
                .request(new RequestCallback() {
                    @Override
                    public void onResult(boolean allGranted, List<String> grantedList, List<String> deniedList) {
                        if (allGranted) {
                            Toast.makeText(DeviceScanActivity.this, "所有申请的权限都已通过", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(DeviceScanActivity.this, "您拒绝了如下权限：" + deniedList, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jdy_activity_main);
        mContext = this;
        this.setTitle("BLE无线控制器");
        //getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 如果本地蓝牙没有开启，则开启  
        if (!mBluetoothAdapter.isEnabled()) {
            // 我们通过startActivityForResult()方法发起的Intent将会在onActivityResult()回调方法中获取用户的选择，比如用户单击了Yes开启，  
            // 那么将会收到RESULT_OK的结果，  
            // 如果RESULT_CANCELED则代表用户不愿意开启蓝牙  
            Intent mIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(mIntent, 1);
            // 用enable()方法来开启，无需询问用户(实惠无声息的开启蓝牙设备),这时就需要用到android.permission.BLUETOOTH_ADMIN权限。  
            // mBluetoothAdapter.enable();  
            // mBluetoothAdapter.disable();//关闭蓝牙  
        }


        lv_bleList = (ListView) findViewById(R.id.lv_bleList);

        //权限申请
        requestPermissions();
        mDevListAdapter = new DeviceListAdapter(mBluetoothAdapter, DeviceScanActivity.this);
        dev_bid = (byte) 0x88;//88 是JDY厂家VID码
        mDevListAdapter.set_vid(dev_bid);//用于识别自家的VID相同的设备，只有模块的VID与APP的VID相同才会被搜索得到
        lv_bleList.setAdapter(mDevListAdapter.init_adapter());


        lv_bleList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (mDevListAdapter.get_count() > 0) {

                    Byte vid_byte = mDevListAdapter.get_vid(position);//返回136表示是JDY厂家模块
                    //String vid_str =String.format("%02x", vid_byte );
                    //Toast.makeText( DeviceScanActivity.this,"设备VID:"+vid_str, Toast.LENGTH_SHORT).show();
//				    Toast.makeText( DeviceScanActivity.this, "type:"+mDevListAdapter.get_item_type(position), Toast.LENGTH_SHORT).show(); 

                    if (vid_byte == dev_bid)//JDY厂家VID为0X88， 用户的APP不想搜索到其它厂家的JDY-08模块的话，可以设备一下 APP的VID，此时模块也需要设置，
                        //模块的VID与厂家APP的VID要一样，APP才可以搜索得到模块VID与APP一样的设备
                        switch (mDevListAdapter.get_item_type(position)) {
                            case JDY:////为标准透传模块
                            {
                                Log.i(TAG, "标准透传模块");
                                BluetoothDevice device1 = mDevListAdapter.get_item_dev(position);
                                if (device1 == null) return;
                                Intent intent1 = new Intent(DeviceScanActivity.this, MainActivity.class);
                                intent1.putExtra(EXTRAS_DEVICE_NAME, device1.getName());
                                intent1.putExtra(EXTRAS_DEVICE_ADDRESS, device1.getAddress());
                                mDevListAdapter.scan_jdy_ble(false);
                                mScanning = false;
                                startActivity(intent1);
                                break;
                            }
                            case JDY_iBeacon:////为iBeacon设备
                            {
                                Log.i(TAG, "为iBeacon设备");

                            }
                            case sensor_temp://温度传感器
                            {

                                Log.i(TAG, "为iBeacon设备");
                                break;
                            }
                            case JDY_KG://开关控制APP
                            {
                                Log.i(TAG, "开关控制APP");
                                break;
                            }
                            case JDY_KG1://开关控制APP
                            {
                                Log.i(TAG, "开关控制APP2");
                                break;
                            }
                            case JDY_AMQ://massager 按摩器APP
                            {
                                Log.i(TAG, "按摩器APP");
                                break;
                            }
                            case JDY_LED1:// LED灯 APP 测试版本
                            {

                                Log.i(TAG, "LED灯");
                                break;
                            }
                            case JDY_LED2:// LED灯 APP
                            {
                                Log.i(TAG, "LED灯2");
                                break;
                            }


                            default:
                                break;
                        }

                }
            }
        });


        Message message = new Message();
        message.what = 100;
        handler.sendMessage(message);


    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 100) {
            }


            super.handleMessage(msg);
        }

        private void setTitle(String hdf) {
            // TODO 自动生成的方法存根

        }

        ;
    };

    public static boolean turnOnBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            return bluetoothAdapter.enable();
        }
        return false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_menu, menu);

        menu.findItem(R.id.scan_menu_set).setVisible(true);
        menu.findItem(R.id.scan_menu_id).setActionView(null);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan_menu_set:
                //mDevListAdapter.clear();
                //mDevListAdapter.scan_jdy_ble( true );
                Intent intent1 = new Intent(DeviceScanActivity.this, SetActivity.class);
                ;
                startActivity(intent1);
                break;
            case R.id.scan_menu_set1: {
                mDevListAdapter.clear();
                scanLeDevice(true);
            }
            break;
        }
        return true;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mDevListAdapter.scan_jdy_ble(false);
                    //invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mDevListAdapter.scan_jdy_ble(true);
        } else {
            mScanning = false;
            mDevListAdapter.scan_jdy_ble(false);
        }
        // invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {//打开APP时扫描设备
        super.onResume();
        scanLeDevice(true);

        //mDevListAdapter.scan_jdy_ble( false );
    }

    @Override
    protected void onPause() {//停止扫描
        super.onPause();
        //scanLeDevice(false);
        mDevListAdapter.scan_jdy_ble(false);
    }

    public boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }



}