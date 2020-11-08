package com.cqu.ebd.ble;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.cqu.ebd.R;
import com.cqu.ebd.beans.JDY_type;
import com.cqu.ebd.utils.Log;

//import com.example.jdy_type.Get_type;


public class DeviceListAdapter extends Activity {
    private static String TAG = "DeviceListAdapter";
    int list_select_index = 0;

    //	Get_type mGet_type;
    private DeviceListAdapter1 list_cell_0;
    BluetoothAdapter apter;
    Context context;

    int scan_int = 0;
    int ip = 0;

    public String ibeacon_UUID = "";
    public String ibeacon_MAJOR = "";
    public String ibeacon_MINOR = "";

    public byte sensor_temp;//传感器温度值十六进制格式-----1byte
    public byte sensor_humid;//传感器湿度值 十六进制格式-----1byte

    public byte sensor_batt;//传感器电量十六进制格式-----1byte
    public byte[] sensor_VID;//传感器厂家识别码十六进制格式-----2byte


    public JDY_type DEV_TYPE;

    Timer timer = new Timer();
    boolean stop_timer = true;

    byte dev_VID = (byte) 0x88;

    public JDY_type dv_type(byte[] p) {
        //Log.d( "out_3=","scan_byte_len:"+ p.length);
        if (p.length != 62) return null;
        //if( p.length!=0 )return null;
        String str;

        byte m1 = (byte) ((p[18 + 2] + 1) ^ 0x11);////透传模块密码位判断
        str = String.format("%02x", m1);
        //Log.d( "out_1","="+ str);

        byte m2 = (byte) ((p[17 + 2] + 1) ^ 0x22);//透传模块密码位判断
        str = String.format("%02x", m2);
        //Log.d( "out_2","="+ str);


        int ib1_major = 0;
        int ib1_minor = 0;
        if (p[52] == (byte) 0xff) {
            if (p[53] == (byte) 0xff) ib1_major = 1;
        }
        if (p[54] == (byte) 0xff) {
            if (p[55] == (byte) 0xff) ib1_minor = 1;
        }

        if (p[5] == (byte) 0xe0 && p[6] == (byte) 0xff && p[11] == m1 && p[12] == m2 && (dev_VID == p[19 - 6]))//JDY
        {
            byte[] WriteBytes = new byte[4];
            WriteBytes[0] = p[19 - 6];
            WriteBytes[1] = p[20 - 6];
            Log.d("out_1", "TC" + list_cell_0.bytesToHexString1(WriteBytes));

            if (p[20 - 6] == (byte) 0xa0) return JDY_type.JDY;//透传
            else if (p[20 - 6] == (byte) 0xa5) return JDY_type.JDY_AMQ;//按摩器
            else if (p[20 - 6] == (byte) 0xb1) return JDY_type.JDY_LED1;// LED灯
            else if (p[20 - 6] == (byte) 0xb2) return JDY_type.JDY_LED2;// LED灯
            else if (p[20 - 6] == (byte) 0xc4) return JDY_type.JDY_KG;// 开关控制
            else if (p[20 - 6] == (byte) 0xc5) return JDY_type.JDY_KG1;// 开关控制

            //Log.d( "JDY_type.JDY=","1");
            return JDY_type.JDY;
        } else if (p[44] == (byte) 0x10 && p[45] == (byte) 0x16 && (ib1_major == 1 || ib1_minor == 1))//sensor
        {
            return JDY_type.sensor_temp;
        } else if (p[3] == (byte) 0x1a && p[4] == (byte) 0xff) {

            return JDY_type.JDY_iBeacon;
        } else {
            //Log.d( "JDY_type.UNKW=","0");
            return JDY_type.UNKW;
        }

        //return JDY_type.JDY_iBeacon;
    }


    public DeviceListAdapter(BluetoothAdapter adapter, Context context1) {
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        apter = adapter;
        context = context1;
        list_cell_0 = new DeviceListAdapter1();
//        mGet_type = new Get_type();
        timer.schedule(task, 1000, 1000); // 1s后执行task,经过1s再次执行  
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1 && stop_timer) {
                //tvShow.setText(Integer.toString(i++));
                loop_list();
//	            	Log.d( "out_1","time run" );
            }
            super.handleMessage(msg);
        }

        ;
    };

    TimerTask task = new TimerTask() {

        @Override
        public void run() {
            // 需要做的事:发送消息
            Message message = new Message();
            message.what = 1;
            handler.sendMessage(message);
        }
    };


    public DeviceListAdapter1 init_adapter() {

        return list_cell_0;
    }

    public BluetoothDevice get_item_dev(int pos) {
        return list_cell_0.dev_ble.get(pos);
    }

    public JDY_type get_item_type(int pos) {
        return list_cell_0.dev_type.get(pos);
    }

    public int get_count() {
        return list_cell_0.getCount();
    }



    public byte get_vid(int pos) {
        return (byte) list_cell_0.get_vid(pos);
    }

    public void set_vid(byte vid) {
        dev_VID = vid;
    }


    public void loop_list() {
        list_cell_0.loop();
    }


    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            scan_int++;
            if (scan_int > 1) {
                scan_int = 0;
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    JDY_type m_tyep = dv_type(scanRecord);
                    if (m_tyep != JDY_type.UNKW && m_tyep != null) {
                        list_cell_0.addDevice(device, scanRecord, rssi, m_tyep);
                        //mDevListAdapter.notifyDataSetChanged();
                        list_cell_0.notifyDataSetChanged();
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JDY_type m_tyep = dv_type(scanRecord);
                            if (m_tyep != JDY_type.UNKW && m_tyep != null) {
                                list_cell_0.addDevice(device, scanRecord, rssi, m_tyep);
                                //mDevListAdapter.notifyDataSetChanged();
                                list_cell_0.notifyDataSetChanged();
                            }
                        }
                    });
                }
            }
        }
    };// public void addDevice(BluetoothDevice device,byte[] scanRecord,Integer  RSSI,JDY_type type ) 


    public void stop_flash() {
        stop_timer = false;
    }

    public void start_flash() {
        stop_timer = true;
    }

    public void clear() {
        list_cell_0.clear();
    }

    public void scan_jdy_ble(Boolean p)//扫描BLE蓝牙
    {
        if (p) {

            list_cell_0.notifyDataSetChanged();
            apter.startLeScan(mLeScanCallback);
            start_flash();
        } else {
            apter.stopLeScan(mLeScanCallback);
            stop_flash();
        }
    }


    class DeviceListAdapter1 extends BaseAdapter {
        private List<BluetoothDevice> dev_ble;
        private List<JDY_type> dev_type;
        private List<byte[]> dev_scan_data;
        private List<Integer> dev_rssi;
        private List<Integer> remove;

        private ViewHolder viewHolder;
        int count = 0;
        int ip = 0;

        public DeviceListAdapter1() {
            dev_ble = new ArrayList<BluetoothDevice>();
            dev_scan_data = new ArrayList<byte[]>();
            dev_rssi = new ArrayList<Integer>();
            dev_type = new ArrayList<JDY_type>();
            remove = new ArrayList<Integer>();
        }

        public void loop() {
            if (remove != null && remove.size() > 0 && ip == 0) {

                if (count >= remove.size()) {
                    count = 0;
                }
                Integer it = remove.get(count);
                if (it >= 3) {
                    dev_ble.remove(count);
                    dev_scan_data.remove(count);
                    dev_rssi.remove(count);
                    dev_type.remove(count);
                    remove.remove(count);
                    notifyDataSetChanged();
                } else {
                    it++;
                    remove.add(count + 1, it);
                    remove.remove(count);
                }
                count++;

            }
        }

        public void addDevice(BluetoothDevice device, byte[] scanRecord, Integer RSSI, JDY_type type) {
            ip = 1;
            if (!dev_ble.contains(device)) {
                dev_ble.add(device);
                dev_scan_data.add(scanRecord);
                dev_rssi.add(RSSI);
                dev_type.add(type);
                Integer it = 0;
                remove.add(it);
            } else {
                for (int i = 0; i < dev_ble.size(); i++) {
                    String btAddress = dev_ble.get(i).getAddress();
                    if (btAddress.equals(device.getAddress())) {
                        //if( dev_type.get( i )==JDY_type.JDY_iBeacon||dev_type.get( i )==JDY_type.JDY_sensor )
                        {
                            dev_ble.add(i + 1, device);
                            dev_ble.remove(i);

                            dev_scan_data.add(i + 1, scanRecord);
                            dev_scan_data.remove(i);

                            dev_rssi.add(i + 1, RSSI);
                            dev_rssi.remove(i);

                            dev_type.add(i + 1, type);
                            dev_type.remove(i);

                            Integer it = 0;// remove.get( i );
                            remove.add(i + 1, it);
                            remove.remove(i);


                        }
                    }
                }
            }
            notifyDataSetChanged();
            ip = 0;
        }

        public void clear() {
            dev_ble.clear();
            dev_scan_data.clear();
            dev_rssi.clear();
            dev_type.clear();
            remove.clear();
        }

        @Override
        public int getCount() {
            return dev_ble.size();
        }

        @Override
        public BluetoothDevice getItem(int position) {
            return dev_ble.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                //Log.d( "convertView == null","0");
            }
            if (position <= dev_ble.size()) {
                JDY_type type_0 = dev_type.get(position);
                if (type_0 == JDY_type.JDY)//为标准透传模块
                {
                    Log.d(TAG, "JDY_type.JDY=标准透传模块");

                    convertView = LayoutInflater.from(context).inflate(R.layout.listitem_device, null);
                    viewHolder = new ViewHolder();
                    viewHolder.tv_devName = (TextView) convertView.findViewById(R.id.device_name);
                    viewHolder.tv_devAddress = (TextView) convertView.findViewById(R.id.device_address);
                    viewHolder.device_rssi = (TextView) convertView.findViewById(R.id.device_rssi);
                    viewHolder.scan_data = (TextView) convertView.findViewById(R.id.scan_data);
                    viewHolder.type0 = (TextView) convertView.findViewById(R.id.type0);
                    convertView.setTag(viewHolder);
                    list_select_index = 1;


                    // add-Parameters
                    BluetoothDevice device = dev_ble.get(position);
                    String devName = device.getName();
                    devName = "Name:" + devName;
                    if (viewHolder.tv_devName != null)
                        viewHolder.tv_devName.setText(devName);

                    String mac = device.getAddress();
                    mac = "MAC:" + mac;
                    if (viewHolder.tv_devAddress != null)
                        viewHolder.tv_devAddress.setText(mac);

                    String rssi_00 = "" + dev_rssi.get(position);
                    rssi_00 = "RSSI:-" + rssi_00;
                    if (viewHolder.device_rssi != null)
                        viewHolder.device_rssi.setText(rssi_00);

                    String tp = null;
                    tp = "Type:" + "标准模式";
                    if (viewHolder.type0 != null)
                        viewHolder.type0.setText(tp);

                    if (viewHolder.scan_data != null)
                        viewHolder.scan_data.setText("scanRecord:" + bytesToHexString1(dev_scan_data.get(position)));

                } else if (type_0 == JDY_type.JDY_iBeacon)//为iBeacon设备
                {
                    Log.d(TAG, "JDY_type.JDY_iBeacon=" + "iBeacon设备");
                } else if (type_0 == JDY_type.sensor_temp)//为传感器设备
                {

                    Log.d(TAG, "JDY_type.JDY_iBeacon=" + "传感器设备");

                } else if (type_0 == JDY_type.JDY_LED1)//为RGB LED灯条设备
                {
                    Log.d(TAG, "JDY_type.JDY_LED1=" + "LED灯条设备");
                } else if (type_0 == JDY_type.JDY_LED2)//为RGB LED灯条设备
                {
                    Log.d(TAG, "JDY_type.JDY_LEDw=" + "LED灯条设备");
                } else if (type_0 == JDY_type.JDY_AMQ)//为按摩器设备
                {
                    Log.d(TAG, "JDY_type.JDY_AMQ=" + "按摩器设备");

                } else if (type_0 == JDY_type.JDY_KG)//为继电器控制、IO控制等设备
                {
                    Log.d(TAG, "JDY_type.JDY_KG=" + "继电器控制、IO控制等");
                } else if (type_0 == JDY_type.JDY_KG1)//为继电器控制、IO控制等设备
                {
                    Log.d(TAG, "JDY_type.JDY_KG1=" + "继电器控制、IO控制等");
                } else if (type_0 == JDY_type.JDY_WMQ)//为纹眉器设备
                {
                    Log.d(TAG, "JDY_type.JDY_WMQ=" + "纹眉器设备");
                } else if (type_0 == JDY_type.JDY_LOCK)//为蓝牙电子锁设备
                {
                    Log.d(TAG, "JDY_type.JDY_LOCK=" + "蓝牙电子锁设备");

                } else {

                    //list_select_index=0;
                }

                return convertView;
            }
            return null;

        }


        public int get_vid(int pos) {
            String vid = null;
            byte[] byte1000 = (byte[]) dev_scan_data.get(pos);
            byte[] result = new byte[4];
            result[0] = 0X00;
            result[1] = 0X00;
            result[2] = 0X00;
            JDY_type tp = dev_type.get(pos);
            if (tp == JDY_type.JDY || tp == JDY_type.JDY_LED1 || tp == JDY_type.JDY_LED2 || tp == JDY_type.JDY_AMQ || tp == JDY_type.JDY_KG || tp == JDY_type.JDY_KG1 || tp == JDY_type.JDY_WMQ || tp == JDY_type.JDY_LOCK) {
                result[3] = byte1000[19 - 6];
            } else {
                result[3] = byte1000[56];
            }

            int ii100 = byteArrayToInt1(result);
            //vid=String.valueOf(ii100);
            return ii100;
        }


        public int byteArrayToInt1(byte[] bytes) {
            int value = 0;
            //由高位到低位
            for (int i = 0; i < 4; i++) {
                int shift = (4 - 1 - i) * 8;
                value += (bytes[i] & 0x000000FF) << shift;//往高位游
            }
            return value;
        }

        private String bytesToHexString(byte[] src) {
            StringBuilder stringBuilder = new StringBuilder(src.length);
            for (byte byteChar : src)
                stringBuilder.append(String.format("%02X", byteChar));
            return stringBuilder.toString();
        }

        private String bytesToHexString1(byte[] src) {
            StringBuilder stringBuilder = new StringBuilder(src.length);
            for (byte byteChar : src)
                stringBuilder.append(String.format(" %02X", byteChar));
            return stringBuilder.toString();
        }


    }

    class ViewHolder {
        TextView tv_devName, tv_devAddress, device_rssi, type0, scan_data;//透传
    }
}

