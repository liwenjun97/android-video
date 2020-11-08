package com.cqu.ebd.common;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import android.os.IBinder;
import android.os.Message;

import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.cqu.ebd.R;
import com.cqu.ebd.ble.BluetoothLeService;
import com.cqu.ebd.monitor.CameraWrapper;
import com.cqu.ebd.utils.FileUtil;
import com.cqu.ebd.utils.Log;
import com.cqu.ebd.utils.Log;
import com.cqu.ebd.utils.Utils;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static String TAG = "MainActivity";
    private SurfaceView surfaceView;
    private View rlRoot;
    private Button mBtStart;

    private int mRecordTime;
    private Handler mHandler = new Handler();
    private Button mBtPause;


    //BLE
    private boolean mConnected = false;//是否连接
    private boolean connect_status_bit = false;
    int connect_count = 0;
    int send_count = 0; //发送数据数量
    int receive_count = 0;    //接收数据数量
    private BluetoothLeService mBluetoothLeService;
    private StringBuffer sbValues;
    private TextView mConnectionState;
    private TextView mDataField;
    private TextView textView5;     //暂空比
    private TextView sendDataTxt;   //发送数据
    private SeekBar seekBar1;       //暂空比
    private String mDeviceName;
    private String mDeviceAddress;
    private Button send_button;
    private Button writeToFile;
    private Button clear_button;
    private File bleLogFile = null;
    private EditText txd_txt;
    private EditText rx_data_id_1;
    private Switch pwmSwitch;
    boolean send_hex = true;//HEX格式发送数据  透传
    boolean rx_hex = false;//HEX格式接收数据  透传
    CheckBox checkBox5, checkBox1;
    ToggleButton key1, key2, key3, key4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initBleView();
    }

    private void initView() {
        surfaceView = findViewById(R.id.surface);
        rlRoot = findViewById(R.id.rl_root);

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initCamera(false);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        mBtStart = findViewById(R.id.bt_start);
        mBtPause = findViewById(R.id.bt_pause);
        rlRoot = findViewById(R.id.rl_root);
        mBtStart.setOnClickListener(this);
        findViewById(R.id.bt_stop).setOnClickListener(this);
        mBtPause.setOnClickListener(this);


    }
    private void initBleView(){
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);

        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);

        send_button = (Button) findViewById(R.id.tx_button);//send data 1002
        send_button.setOnClickListener(listener);//设置监听

        writeToFile = (Button)findViewById(R.id.tx_button_write) ;//写入文件
        writeToFile.setOnClickListener(listener);
        writeToFile.setTag(false);

        clear_button = (Button) findViewById(R.id.clear_button);//send data 1002
        clear_button.setOnClickListener(listener);//设置监听

        txd_txt = (EditText) findViewById(R.id.tx_text);//1002 data
        txd_txt.setText("0102030405060708090A0102030405060708090A0102030405060708090A0102030405060708090A");
        txd_txt.clearFocus();

        rx_data_id_1 = (EditText) findViewById(R.id.rx_data_id_1);//1002 data
        rx_data_id_1.setText("");
        rx_data_id_1.setOnTouchListener(onTouchListener);

        key1 = (ToggleButton) findViewById(R.id.toggleButton1);
        key2 = (ToggleButton) findViewById(R.id.toggleButton2);
        key3 = (ToggleButton) findViewById(R.id.toggleButton3);
        key4 = (ToggleButton) findViewById(R.id.toggleButton4);

        key1.setOnClickListener(OnClickListener_listener);//设置监听
        key2.setOnClickListener(OnClickListener_listener);//设置监听
        key3.setOnClickListener(OnClickListener_listener);//设置监听
        key4.setOnClickListener(OnClickListener_listener);//设置监听

        textView5 = (TextView) findViewById(R.id.textView5);
        sendDataTxt = (TextView) findViewById(R.id.tx);
        sbValues = new StringBuffer();
        mHandler = new Handler();
        pwmSwitch = (Switch) findViewById(R.id.switch1);
        pwmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                // TODO Auto-generated method stub
                if (isChecked) {
                    if (mConnected) {
                        mBluetoothLeService.set_PWM_frequency(250);//设置PWM频率
                        mBluetoothLeService.Delay_ms(20);//延时20MS
                        mBluetoothLeService.set_PWM_OPEN(1);//打开PWM
                    }
                } else {
                    if (mConnected)
                        mBluetoothLeService.set_PWM_OPEN(0);//关闭PWM
                }
            }
        });
        seekBar1 = (SeekBar) findViewById(R.id.seekBar1);
        seekBar1.setOnSeekBarChangeListener(this);
        seekBar1.setMax(255);

        checkBox5 = (CheckBox) findViewById(R.id.checkBox5);
        checkBox1 = (CheckBox) findViewById(R.id.checkBox1);
        checkBox5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                // TODO Auto-generated method stub
                if (isChecked) {
                    rx_hex = true;

                } else {
                    rx_hex = false;
                }
            }
        });
        checkBox1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                // TODO Auto-generated method stub
                if (isChecked) {
                    send_hex = false;
                } else {
                    send_hex = true;
                }
            }
        });

        // timer.schedule(task, 3000, 3000); // 1s后执行task,经过1s再次执行
        Message message = new Message();
        message.what = 1;
        handler.sendMessage(message);
        //注册接收
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {

            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        updateConnectionState(R.string.connecting);
        show_view(false);
        get_pass();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG,"onConfigurationChanged");
        initCamera(false);
    }

    private void initCamera(boolean isChecked) {
        CameraWrapper.getInstance().setContext(this);
        changeSurfaceView(isChecked);
        CameraWrapper.getInstance()
                .setCameraId(Camera.CameraInfo.CAMERA_FACING_BACK)
                .doOpenCamera(surfaceView);
        CameraWrapper.getInstance().pausePreview();
        CameraWrapper.getInstance().startPreview();
    }

    private void changeSurfaceView(boolean isPort) {
        Display display = getWindowManager().getDefaultDisplay();
        int screenW = rlRoot.getWidth();
        int screenH = rlRoot.getHeight();
        Log.d("TAG", "changeSurfaceView" + screenW + "*" + screenH);
        if (isPort) {
            float scaleW = (float) screenW / CameraWrapper.SRC_VIDEO_HEIGHT;
            float scaleH = (float) screenH / CameraWrapper.SRC_VIDEO_WIDTH;
            float scale = Math.min(scaleH, scaleW);
            surfaceView.getLayoutParams().width = (int) (CameraWrapper.SRC_VIDEO_HEIGHT * scale);
            surfaceView.getLayoutParams().height = (int) (CameraWrapper.SRC_VIDEO_WIDTH * scale);
            surfaceView.requestLayout();
        } else {
            float scaleW = (float) screenW / CameraWrapper.SRC_VIDEO_WIDTH;
            float scaleH = (float) screenH / CameraWrapper.SRC_VIDEO_HEIGHT;
            float scale = Math.min(scaleH, scaleW);
            surfaceView.getLayoutParams().width = (int) (CameraWrapper.SRC_VIDEO_WIDTH * scale);
            surfaceView.getLayoutParams().height = (int) (CameraWrapper.SRC_VIDEO_HEIGHT * scale);
            surfaceView.requestLayout();
        }
    }

    boolean misRecord;
    boolean isPause;

    String newVideoPath;
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_start:
                if(!mConnected){
                    Toast.makeText(this,"蓝牙未连接",Toast.LENGTH_SHORT).show();
                    return;
                }
                if (misRecord) {
                    return;
                }
                misRecord = true;

                newVideoPath= Utils.getVideoFilePath();
                CameraWrapper.getInstance().startRecording(newVideoPath);
                mBtStart.setEnabled(false);
                mRecordTime = 0;
                mBtStart.setText("00:00");
                isPause=false;
                mBtPause.setText("暂停录制");

                mHandler.removeCallbacksAndMessages(null);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(!isPause){
                            mRecordTime++;
                            mBtStart.setText(getStrRecordTime(mRecordTime));
                        }
                        mHandler.postDelayed(this, 1000);
                    }
                }, 1000);
                startBleRecord();
                break;
            case R.id.bt_pause:
                if(!isPause){
                    CameraWrapper.getInstance().pauseRecording();
                    isPause=true;
                    mBtPause.setText("继续录制");
                }else{
                    isPause=false;
                    CameraWrapper.getInstance().resumeRecording();
                    mBtPause.setText("暂停录制");
                }
                break;
            case R.id.bt_stop:
                Toast.makeText(this,"视频文件保存至："+newVideoPath,Toast.LENGTH_SHORT).show();
                CameraWrapper.getInstance().stopRecording();
                mHandler.removeCallbacksAndMessages(null);
                misRecord=false;
                mBtStart.setEnabled(true);
                mBtStart.setText("开始录制");
                stopBleRecord();
                break;

            default:

                break;
        }
    }

    private String getStrRecordTime(int mRecordTime) {
        int minute = mRecordTime / 60;
        int second = mRecordTime % 60;
        return String.format(Locale.CHINA, "%02d:%02d", minute, second);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add("生成数字YUV数据").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(new Intent(MainActivity.this, CreateNumberAct.class));
                return true;
            }
        });
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null)
            mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
    }


    /*
    * *******************************BLE************************************
    * */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE1);
        return intentFilter;
    }
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                connect_status_bit = true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;

                updateConnectionState(R.string.disconnected);
                connect_status_bit = false;
                show_view(false);
                invalidateOptionsMenu();
                clearUI();

                if (connect_count == 0) {
                    connect_count = 1;
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                }

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) //接收FFE1串口透传数据通道数据
            {
                displayData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE1.equals(action)) //接收FFE2功能配置返回的数据
            {
                displayData1(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA1));
            }
            //Log.d("", msg)
        }
    };
    private void displayGattServices(List<BluetoothGattService> gattServices) {


        if (gattServices == null) return;

        if (gattServices.size() > 0 && mBluetoothLeService.get_connected_status(gattServices) == 2)//表示为JDY-06、JDY-08系列蓝牙模块
        {
            connect_count = 0;
            if (connect_status_bit) {
                mConnected = true;
                show_view(true);
                mBluetoothLeService.Delay_ms(100);
                mBluetoothLeService.enable_JDY_ble(0);
                mBluetoothLeService.Delay_ms(100);
                mBluetoothLeService.enable_JDY_ble(1);
                mBluetoothLeService.Delay_ms(100);

                byte[] WriteBytes = new byte[2];
                WriteBytes[0] = (byte) 0xE7;
                WriteBytes[1] = (byte) 0xf6;
                mBluetoothLeService.function_data(WriteBytes);// 发送读取所有IO状态


                updateConnectionState(R.string.connected);

                enable_pass();
            } else {
                Toast toast = Toast.makeText(MainActivity.this, "设备没有连接！", Toast.LENGTH_SHORT);
                toast.show();
            }
        } else if (gattServices.size() > 0 && mBluetoothLeService.get_connected_status(gattServices) == 1)//表示为JDY-09、JDY-10系列蓝牙模块
        {
            connect_count = 0;
            if (connect_status_bit) {
                mConnected = true;
                show_view(true);

                mBluetoothLeService.Delay_ms(100);
                mBluetoothLeService.enable_JDY_ble(0);

                updateConnectionState(R.string.connected);
            } else {
                Toast toast = Toast.makeText(MainActivity.this, "设备没有连接！", Toast.LENGTH_SHORT);
                toast.show();
            }
        } else {
            Toast toast = Toast.makeText(MainActivity.this, "提示！此设备不为JDY系列BLE模块", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                //tvShow.setText(Integer.toString(i++));
                //scanLeDevice(true);
                if (mBluetoothLeService != null) {
                    if (mConnected == false) {
                        updateConnectionState(R.string.connecting);
                        final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                        Log.d(TAG, "Connect request result=" + result);
                    }
                }
            }
            if (msg.what == 2) {
                try {
                    Thread.currentThread();
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBluetoothLeService.enable_JDY_ble(0);
                try {
                    Thread.currentThread();
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBluetoothLeService.enable_JDY_ble(0);
                try {
                    Thread.currentThread();
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBluetoothLeService.enable_JDY_ble(1);
                try {
                    Thread.currentThread();
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                byte[] WriteBytes = new byte[2];
                WriteBytes[0] = (byte) 0xE7;
                WriteBytes[1] = (byte) 0xf6;
                mBluetoothLeService.function_data(WriteBytes);// 发送读取所有IO状态
            }
            super.handleMessage(msg);
        }
    };

    //暂空比进度条
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (mConnected) {
            mBluetoothLeService.set_PWM_ALL_pulse(seekBar.getProgress(), seekBar.getProgress(), seekBar.getProgress(), seekBar.getProgress());
            textView5.setText("暂空比：" + seekBar.getProgress());
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
    //更新连接状态
    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }
    //控制区
    void show_view(boolean p) {
        if (p) {
            send_button.setEnabled(true);
            key1.setEnabled(true);
            key2.setEnabled(true);
            key3.setEnabled(true);
            key4.setEnabled(true);
            pwmSwitch.setEnabled(true);
            seekBar1.setEnabled(true);
        } else {
            send_button.setEnabled(false);
            key1.setEnabled(false);
            key2.setEnabled(false);
            key3.setEnabled(false);
            key4.setEnabled(false);
            pwmSwitch.setEnabled(false);
            seekBar1.setEnabled(false);
        }
    }
    public void enable_pass() {
        mBluetoothLeService.Delay_ms(100);
        mBluetoothLeService.set_APP_PASSWORD(password_value);
    }

    String password_value = "123456";

    public void get_pass() {
        password_value = getSharedPreference("DEV_PASSWORD_LEY_1000");
        if (password_value != null || password_value != "") {
            if (password_value.length() == 6) {

            } else password_value = "123456";
        } else password_value = "123456";

    }
    public String getSharedPreference(String key) {
        SharedPreferences sharedPreferences = getSharedPreferences("test",
                Activity.MODE_PRIVATE);
        String name = sharedPreferences.getString(key, "");
        return name;
    }

    String da = "";

    //按钮监听事件
    Button.OnClickListener listener = new Button.OnClickListener() {//创建监听对象

        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tx_button://uuid1002 数传通道发送数据
                    if (connect_status_bit) {
                        if (mConnected) {
                            String tx_string = txd_txt.getText().toString().trim();
                            send_count += mBluetoothLeService.txxx(tx_string, send_hex);//发送字符串数据
                            sendDataTxt.setText("发送数据：" + send_count);
                        }
                    } else {
                        //Toast.makeText(this, "Deleted Successfully!", Toast.LENGTH_LONG).show();
                        Toast toast = Toast.makeText(MainActivity.this, "设备没有连接！", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    break;
                case R.id.clear_button: {
                    sbValues.delete(0, sbValues.length());
                    receive_count = 0;
                    da = "";
                    rx_data_id_1.setText(da);
                    mDataField.setText("" + receive_count);
                    send_count = 0;
                    sendDataTxt.setText("发送数据：" + send_count);
                }
                break;
                case R.id.tx_button_write: {
                    if(!(Boolean) writeToFile.getTag()){
                        String filepath = FileUtil.getBleFilePath();
                        bleLogFile = new File(filepath);
                        writeToFile.setText("停止写入");
                        writeToFile.setTag(true);
                    }else {
                        bleLogFile = null;
                        writeToFile.setText("写入文件");
                        writeToFile.setTag(false);
                    }
                }
                default:
                    break;
            }
        }
    };
    //功能设置区按钮事件
    ToggleButton.OnClickListener OnClickListener_listener = new ToggleButton.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mConnected) {
                // TODO 自动生成的方法存根
                byte bit = (byte) 0x00;
                if (v.getId() == R.id.toggleButton1) {
                    bit = (byte) 0xf1;
                } else if (v.getId() == R.id.toggleButton2) {
                    bit = (byte) 0xf2;
                } else if (v.getId() == R.id.toggleButton3) {
                    bit = (byte) 0xf3;
                } else if (v.getId() == R.id.toggleButton4) {
                    bit = (byte) 0xf4;

                    //				 byte[] WriteBytes = new byte[2];
                    //				 WriteBytes[0] = (byte) 0xE7;
                    //				 WriteBytes[1] = (byte) 0xf6;
                    //				 //WriteBytes[2] = (byte)0x01;
                    //				 mBluetoothLeService.function_data( WriteBytes );

                }
                if (bit != (byte) 0x00) {
                    boolean on = ((ToggleButton) v).isChecked();
                    if (on) {
                        // Enable here
                        //Toast.makeText(jdy_Activity.this, "Enable here", Toast.LENGTH_SHORT).show();
                        // E7F101
                        byte[] WriteBytes = new byte[3];
                        WriteBytes[0] = (byte) 0xE7;
                        WriteBytes[1] = bit;
                        WriteBytes[2] = (byte) 0x01;
                        mBluetoothLeService.function_data(WriteBytes);
                    } else {
                        // Disable here
                        //Toast.makeText(jdy_Activity.this, "Disable here", Toast.LENGTH_SHORT).show();
                        byte[] WriteBytes = new byte[3];
                        WriteBytes[0] = (byte) 0xE7;
                        WriteBytes[1] = bit;
                        WriteBytes[2] = (byte) 0x00;
                        mBluetoothLeService.function_data(WriteBytes);
                    }
                }
            }
        }

    };

    //显示数据与写入文件
    private void displayData(byte[] data1) //接收FFE1串口透传数据通道数据
    {

        if (data1 != null && data1.length > 0) {

            //写入文件
            if((Boolean) writeToFile.getTag() && bleLogFile != null){

                String data = new String(data1);

                if(data.contains("$")){
                    //只需要输出的前四位数据
                    Pattern pattern = Pattern.compile(",");
                    Matcher findMatcher = pattern.matcher(data);
                    int number = 0;
                    while(findMatcher.find()) {
                        number++;
                        if(number == 4){//当“a”第二次出现时停止
                            break;
                        }
                    }
                    int start = findMatcher.start();

                    String substring = data.substring(1, start);
                    Log.d(TAG,"截取后的输出:"+substring);
                    FileUtil.writeTextToFile(substring,bleLogFile);
                }

            }
            if (rx_hex) {
                final StringBuilder stringBuilder = new StringBuilder(sbValues.length());
                byte[] WriteBytes = mBluetoothLeService.hex2byte(stringBuilder.toString().getBytes());

                for (byte byteChar : data1)
                    stringBuilder.append(String.format(" %02X", byteChar));

                String da = stringBuilder.toString();

                sbValues.append(da);
                rx_data_id_1.setText(sbValues.toString());
            } else {
                String res = new String(data1);
                if(res.contains("$")){
                    //只需要输出的前四位数据
                    Log.d(TAG,"需要写入文件");
                    Pattern pattern = Pattern.compile(",");
                    Matcher findMatcher = pattern.matcher(res);
                    int number = 0;
                    while(findMatcher.find()) {
                        number++;
                        if(number == 4){//当“a”第二次出现时停止
                            break;
                        }
                    }
                    int start = findMatcher.start();

                    String substring = res.substring(1, start);
                    sbValues.append(substring+"\n");
                    rx_data_id_1.setText(sbValues.toString());
                }
            }

            receive_count += data1.length;

            if (sbValues.length() <= rx_data_id_1.getText().length())
                rx_data_id_1.setSelection(sbValues.length());

            if (sbValues.length() >= 5000) sbValues.delete(0, sbValues.length());
            mDataField.setText("" + receive_count);
        }

    }

    private void displayData1(byte[] data1) //接收FFE2功能配置返回的数据
    {
        //String str = mBluetoothLeService.bytesToHexString1( data1 );//将接收的十六进制数据转换成十六进制字符串
        if (data1.length == 5 && data1[0] == (byte) 0xf6)//判断是否是读取IO状态位
        {
            if (data1[1] == (byte) 0x01) {
                key1.setChecked(true);
            } else {
                key1.setChecked(false);
            }
            if (data1[2] == (byte) 0x01) {
                key2.setChecked(true);
            } else {
                key2.setChecked(false);
            }
            if (data1[3] == (byte) 0x01) {
                key3.setChecked(true);
            } else {
                key3.setChecked(false);
            }
            if (data1[4] == (byte) 0x01) {
                key4.setChecked(true);
            } else {
                key4.setChecked(false);
            }
        } else if (data1.length == 2 && data1[0] == (byte) 0x55)//判断APP的连接密码是否成功
        {
            if (data1[1] == (byte) 0x01) {
//    			Toast.makeText(jdy_Activity.this, "提示！APP密码连接成功", Toast.LENGTH_SHORT).show();
            } else {

            }
        }

    }
    //解决Scrollview嵌套textView的滑动问题
    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                //父节点不拦截子节点
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                //父节点不拦截子节点
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                //父节点拦截子节点
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        }
    };
    private void startBleRecord(){
        String filepath = FileUtil.getBleFilePath();
        bleLogFile = new File(filepath);
        writeToFile.setText("停止写入");
        writeToFile.setTag(true);
    }

    private void stopBleRecord(){
        bleLogFile = null;
        writeToFile.setText("写入文件");
        writeToFile.setTag(false);
    }
}
