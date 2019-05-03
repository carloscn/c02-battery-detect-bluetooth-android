package com.mltbsn.root.c02_battery_detect_android;

/*
* https://blog.csdn.net/zw1996/article/details/75168742
*https://www.2cto.com/kf/201707/659553.html
*https://blog.csdn.net/a1054751988/article/details/51054441
*https://blog.csdn.net/qq_35414804/article/details/53352205
* https://blog.csdn.net/Small_Lee/article/details/50899743
* */

import android.Manifest;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.icu.text.SymbolTable;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.provider.SyncStateContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;




public class BluetoothBleActivity extends AppCompatActivity {

    private Toast   mToast;
    private Button mBtnSend;
    private Button mBtnDisconnect;
    private Button mBtnScan;
    private Button mBtnClear;
    private Button mBtnOpen;
    private Button mBtnClose;
    private ListView mLvDeviceList;
    private TextView mTextView;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private BluetoothDeviceAdapter mBluetoothDeviceAdapter;
    private OutputStream outStream = null;
    private InputStream inStream = null;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private BluetoothSocket socket;
    private List<BluetoothDevice> mBlueList = new ArrayList<>();
    private Context context;
    private Handler mUIHandler = new MyHandler();
    private EditText    mEditTextErrorState;
    private EditText    mEditTextErrorPosition;
    private EditText    mEditTextVoltage;
    private EditText    mEditTextCurrent;
    private EditText    mEditTextTemp;

    String recv_str;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_ENABLE=1;
    private static  final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION  = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_ble);

        context =   this;
        mBluetoothDeviceAdapter = new BluetoothDeviceAdapter(this);

        mBtnSend = findViewById(R.id.btn_send);
        mBtnDisconnect = findViewById(R.id.btn_disconnect);
        mBtnScan = findViewById(R.id.btn_scan);
        mBtnClear   =   findViewById(R.id.btn_clear);
        mBtnClose   =   findViewById(R.id.btn_close);
        mBtnOpen    =   findViewById(R.id.btn_open);
        mLvDeviceList = findViewById(R.id.lv_bluelist);
        mEditTextCurrent = findViewById(R.id.editTextCurrent);
        mEditTextErrorPosition = findViewById(R.id.editTextErrorPosition);
        mEditTextErrorState = findViewById(R.id.editTextErrorSate);
        mEditTextTemp = findViewById(R.id.editTextTemp);
        mEditTextVoltage = findViewById(R.id.editTextVoltate);
//        mTextView   =   findViewById(R.id.tv_recv);
//        mEditText   =   findViewById(R.id.et_send);
        setListener();

        /*
        * GPS COARSE LOCATION permission checked.
        *
        * */
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                Toast.makeText(this, "shouldShowRequestPermissionRationale", Toast.LENGTH_SHORT).show();
            }
        }

        /*
        *  Check bluetooth state.
        * */
        mBluetoothAdapter =   BluetoothAdapter.getDefaultAdapter();
        if( mBluetoothAdapter == null ) {
            Toast.makeText(this, "Bluetooth is not available.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if( !mBluetoothAdapter.isEnabled() ) {
            //Toast.makeText(this, "Please enable your Bluetooth and re-run this program.", Toast.LENGTH_LONG).show();
            mBtnOpen.setEnabled(true);
            mBtnClose.setEnabled(false);
            System.out.println("is enable");

        }else{
            mBtnOpen.setEnabled(false);
            mBtnClose.setEnabled(true);
            System.out.println("is not enable");
        }

        /*
        *  Find nearby bluetooth, get it address.
        * */
        BluetoothReceiver   mBlueToothReceiver = new BluetoothReceiver();
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        mIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mIntentFilter.setPriority(Integer.MAX_VALUE);
        registerReceiver(mBlueToothReceiver,mIntentFilter);
        System.out.println("the bluetooth device is register receiver...");


        /*
        * Add viewList click event. click to connect the remote bluetooth.
        * */
        mLvDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {


            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                System.out.println("Click: "+position);

                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
                try {
                    BluetoothDevice device = mBluetoothDeviceAdapter.getDevice(position);
                    Boolean returnValue = false;
                    Method createBondMethod;
                    if(device.getBondState() == BluetoothDevice.BOND_NONE) {
                        // 反射方法调用；
                        System.out.println("choose a band none device.");
                        createBondMethod = BluetoothDevice.class .getMethod("createBond");
                        System.out.println("开始配对");
                        returnValue = (Boolean) createBondMethod.invoke(device);
                        //mLeDeviceListAdapter_isConnect.notifyDataSetChanged();
                        Toast.makeText(BluetoothBleActivity.this, "点击设备是"+ device.getName() + "   " + device.getAddress(), Toast.LENGTH_LONG).show() ;
                    }else if(device.getBondState() == BluetoothDevice.BOND_BONDED){
                        System.out.println("choose a band device!!");
                        connectThread = new ConnectThread(device, mBluetoothAdapter, mUIHandler);
                        connectThread.start();
                    }

                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

        });



        /*
        *  Register broadcast.
        * */
        /*
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.setPriority(Integer.MAX_VALUE);


        */

    }


    private void setListener( ) {
        OnClick onClick = new OnClick();

        mBtnOpen.setOnClickListener(onClick);
        mBtnSend.setOnClickListener(onClick);
        mBtnScan.setOnClickListener(onClick);
        mBtnClear.setOnClickListener(onClick);
        mBtnClose.setOnClickListener(onClick);
        mBtnDisconnect.setOnClickListener(onClick);

    }
    /*
    *  add
    * */
    private class OnClick implements View.OnClickListener{
        @Override
        public void onClick( View v ) {
            Intent intent = null;
            switch( v.getId() ) {
                case R.id.btn_open:
                    mBluetoothAdapter.enable();
                    mBtnOpen.setEnabled(false);
                    mBtnClose.setEnabled(true);
                    break;
                case R.id.btn_close:
                    mBluetoothAdapter.disable();
                    mBtnOpen.setEnabled(true);
                    mBtnClose.setEnabled(false);
                    break;
                case R.id.btn_disconnect:
                    connectThread.cancel();
                    showToast("Bluetooth connection has been closed!");
                    mLvDeviceList.setBackgroundColor(0);
                    mLvDeviceList.setEnabled(true);
                    break;
                case R.id.btn_scan:
                    /*
                    * Clear the list items.
                    * */
                    mBluetoothDeviceAdapter.clear();
                    mBluetoothDeviceAdapter.notifyDataSetChanged();
                    /*
                    * If discovering so cancel discovery.
                    * */
                    if(mBluetoothAdapter.isDiscovering()){
                        mBluetoothAdapter.cancelDiscovery();
                    }

                    /*
                    * find other bluetooth device.
                    * */
                    mBluetoothAdapter.startDiscovery();
                    break;
                case R.id.btn_send:
                    //String text = mEditText.getText().toString();
                    ///connectThread.sendData( text.getBytes() );
                    break;
                case R.id.btn_clear:
                    mTextView.setText("");
                    break;

            }

        }

    }

    public class BluetoothReceiver extends BroadcastReceiver {


        private String pair_info;
        private String unpair_info;
        private String state_info;


        @Override
        public void onReceive(Context context, Intent intent ) {

            String action = intent.getAction();
            System.out.println ( "SYSTEM: action triggered: " + action  );

            if(BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
                mLvDeviceList.setAdapter(mBluetoothDeviceAdapter);
                System.out.println ( "SYSTEM: Find a device : " + device.getName() + " : " + device.getAddress()  );
                // Scanned a device add to List
                mBluetoothDeviceAdapter.addDevice(device);
                // 数据改变并更新列表
                mBluetoothDeviceAdapter.notifyDataSetChanged();
                if( device.getBondState() == BluetoothDevice.BOND_BONDED ) {
                    pair_info = device.getAddress();
                }else {
                    unpair_info = device.getAddress();
                }

            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                System.out.println ( "SYSTEM: Discovery finished..."  );
            }
        }

        public void set_pairInfo( String val ) {
            pair_info = val;
        }
        public String get_pairInfo() {
            return pair_info;
        }
        public void set_unpairInfo( String val ) {
            unpair_info = val;
        }
        public String get_unpairInfo() {
            return unpair_info;
        }
        public void set_stateInfo( String val ) {
            state_info = val;
        }
        public String get_stateInfo() {
            return state_info;
        }
    }
    private void showToast(String text) {

        if( mToast == null) {
            mToast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        }
        else {
            mToast.setText(text);
        }
        mToast.show();
    }
    /**
     * 处理消息
     */
    String a_current, b_current, c_current, all_current;
    String a_voltage, b_voltage, c_voltage, all_voltage;
    String a_temp, b_temp, c_temp, all_temp;
    String all_state;

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constant.MSG_START_LISTENING:
                    setProgressBarIndeterminateVisibility(true);
                    System.out.println("Start to listener...");
                    break;
                case Constant.MSG_FINISH_LISTENING:
                    setProgressBarIndeterminateVisibility(false);
                    System.out.println("stop listenner");
                    break;
                case Constant.MSG_GOT_DATA:
                    recv_str += String.valueOf(msg.obj);
                    mEditTextErrorState.setText(recv_str);
                    recv_str = "";
                    /*
                    if (recv_str.length() >= 55) {
                        if (recv_str.charAt(0) == '!' && recv_str.contains("@")) {
                            System.out.println("recv :" + recv_str);
                            String[] disp_list = recv_str.split("#");
                            all_state =" ";

                            // 处理第一个数据
                            if (disp_list[1].equals("0")) {
                                all_state += "过流: " + "正常状态" + "  ";
                            } else if (disp_list[1].equals("1")) {
                                all_state += "过流: " + "1号电池过流" + "  ";
                            } else if (disp_list[1].equals("2")) {
                                all_state += "过流: " + "2号电池过流" + "  ";
                            } else if (disp_list[1].equals("3")) {
                                all_state += "过流: " + "1,2号电池过流" + "  ";
                            } else if (disp_list[1].equals("4")) {
                                all_state += "过流: " + "3号电池过流" + "  ";
                            } else if (disp_list[1].equals("5")) {
                                all_state += "过流: " + "1,3号电池过流" + "  ";
                            } else if (disp_list[1].equals("6")) {
                                all_state += "过流: " + "2,3号电池过流" + "  ";
                            } else if (disp_list[1].equals("7")) {
                                all_state += "过流: " + "1,2,3号电池过流" + "  ";
                            }

                            // 处理第2,3,4数据
                            a_current = disp_list[2];
                            b_current = disp_list[3];
                            c_current = disp_list[4];

                            // 处理第5 数据
                            if (disp_list[5].equals("00")) {
                                all_state += "过放: " + "正常状态" + "  ";
                            } else if (disp_list[5].equals("01")) {
                                all_state += "过放: " + "1号电池过放" + "  ";
                            } else if (disp_list[5].equals("02")) {
                                all_state += "过放: " + "2号电池过放" + "  ";
                            } else if (disp_list[5].equals("03")) {
                                all_state += "过放: " + "3号电池过放" + "  ";
                            } else if (disp_list[5].equals("04")) {
                                all_state += "过放: " + "4号电池过放" + "  ";
                            } else if (disp_list[5].equals("05")) {
                                all_state += "过放: " + "1，3号电池过放" + "  ";
                            } else if (disp_list[5].equals("06")) {
                                all_state += "过放: " + "2,3号电池过放" + "  ";
                            } else if (disp_list[5].equals("07")) {
                                all_state += "过放: " + "1,2,3号电池过放" + "  ";
                            } else if (disp_list[5].equals("08")) {
                                all_state += "过冲: " + "1号电池过冲" + "  ";
                            } else if (disp_list[5].equals("16")) {
                                all_state += "过冲: " + "2号电池过冲" + "  ";
                            } else if (disp_list[5].equals("24")) {
                                all_state += "过冲: " + "1,2号电池过冲" + "  ";
                            } else if (disp_list[5].equals("32")) {
                                all_state += "过冲: " + "3号电池过冲" + "  ";
                            } else if (disp_list[5].equals("40")) {
                                all_state += "过冲: " + "1,3号电池过冲" + "  ";
                            } else if (disp_list[5].equals("48")) {
                                all_state += "过冲: " + "2,3号电池过冲" + "  ";
                            } else if (disp_list[5].equals("56")) {
                                all_state += "过冲: " + "1,2,3号电池过冲" + "  ";
                            }

                            // 6 7 8
                            a_voltage = disp_list[6];
                            b_voltage = disp_list[7];
                            c_voltage = disp_list[8];

                            // 处理第9个数据
                            if (disp_list[9].equals("0")) {
                                all_state += "过温: " + "状态正常" + "  ";
                            } else if (disp_list[9].equals("1")) {
                                all_state += "过温: " + "1号电池过温" + "  ";
                            } else if (disp_list[9].equals("2")) {
                                all_state += "过温: " + "2号电池过温" + "  ";
                            } else if (disp_list[9].equals("3")) {
                                all_state += "过温: " + "1,2号电池过温" + "  ";
                            } else if (disp_list[9].equals("4")) {
                                all_state += "过温: " + "1号电池过温" + "  ";
                            } else if (disp_list[9].equals("6")) {
                                all_state += "过温: " + "2,3号电池过温" + "  ";
                            } else if (disp_list[9].equals("7")) {
                                all_state += "过温: " + "1,2,3号电池过温" + "  ";
                            }
                            // 处理第10 11 12
                            a_temp = disp_list[10];
                            b_temp = disp_list[11];
                            c_temp = disp_list[12];


                            all_current = a_current + " mA   |  " + b_current + " mA   |   " + c_current + " mA";
                            all_voltage = a_voltage + " V  |   " + b_voltage + " V   |   " + c_voltage + " V";
                            all_temp = a_temp  + "℃   |   " + b_temp + "℃   |   " + c_temp + " ℃";


                            mEditTextErrorState.setText(all_state);
                            mEditTextVoltage.setText(all_voltage);
                            mEditTextCurrent.setText(all_current);
                            mEditTextTemp.setText(all_temp);
                            recv_str = "";
                            all_state = "";
                        }else{
                            System.out.println("数据没有对齐");
                            recv_str = "";
                        }
                    }else{
                        return;
                    }*/
//                    mTextView.append(String.valueOf(msg.obj));
//                    System.out.println("data: "+String.valueOf(msg.obj));
                break;
                case Constant.MSG_ERROR:
                    System.out.println("error: "+String.valueOf(msg.obj));
                    break;
                case Constant.MSG_CONNECTED_TO_SERVER:
                    System.out.println("Connected to Server");
                    mLvDeviceList.setEnabled(false);
                    mLvDeviceList.setBackgroundColor(Color.rgb(119,136,153));
                    showToast("Bluetooth connection has been set up!");
                    mEditTextErrorPosition.setText("设备运行中..");
                    break;
                case Constant.MSG_GOT_A_CLINET:
                    System.out.println("Got a Client");
                    break;
            }
        }
    }

}
