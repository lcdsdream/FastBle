package com.clj.blesample;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.bluetooth.BleGattCallback;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;
import com.clj.fastble.utils.BluetoothUtil;
import com.clj.fastble.utils.HexUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ble_sample";

    private static final String UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    private static final String UUID_SERVICE_OPERATE = "0000fff0-0000-1000-8000-00805f9b34fb";
    private static final String UUID_OPERATE_WRITE = "0000fff1-0000-1000-8000-00805f9b34fb";

    private static final String SAMPLE_WRITE_DATA = "2204020b010300";

    private static final long TIME_OUT = 5000;                                   // 扫描超时时间
    private static final String DEVICE_MAC_SAMPLE = "11:22:33:44:55:66";

    private BleManager bleManager;                                                // Ble核心管理类
    private Handler handler = new Handler(Looper.getMainLooper());


    // scan
    private List<Map<String, Object>> scanListItem = new ArrayList<Map<String, Object>>();
    private List<BluetoothDevice> deviceList = new ArrayList<>(); // scan device list
    private List<String> deviceListData = new ArrayList<>(); // with device's adv data

    private String deviceMac;
    private MyListScanCallback myListScanCallback = new MyListScanCallback(TIME_OUT);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        bleManager = BleManager.getInstance();
        bleManager.init(this);
    }


    private void initView() {

        //scan adv
        findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (bleManager.isInScanning()) {
                    return;
                }

                scanListItem.clear();
                deviceList.clear();
                deviceListData.clear();

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                // get mac
                EditText etMac = (EditText) findViewById(R.id.et_mac);
                if (etMac.getText().length() == DEVICE_MAC_SAMPLE.length()) {
                    deviceMac = etMac.getText().toString();
                } else {
                    deviceMac = "";
                }
                showToast("Start scan...");
                bleManager.scanDevice(myListScanCallback);
            }
        });
    }

    private static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private void runOnMainThread(Runnable runnable) {
        if (isMainThread()) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    private void showToast(final CharSequence msg) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }


    class MyListScanCallback extends ListScanCallback {

        public MyListScanCallback(long timeoutMillis) {
            super(timeoutMillis);
        }

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            // repeat
            if (deviceList.contains(device)) {
                return;
            }

            if (deviceMac.equals("") || deviceMac.equals(device.getAddress())) {

                deviceList.add(device);
                deviceListData.add(HexUtil.encodeHexStr(scanRecord));

                super.onLeScan(device, rssi, scanRecord);

                Map<String, Object> showItem = new HashMap<String, Object>();
                showItem.put("name", device.getName());
                showItem.put("mac", device.getAddress());
                showItem.put("rssi", rssi);
                scanListItem.add(showItem);

                // show listView
                final List<Map<String, Object>> listItem = scanListItem;
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        SimpleAdapter myAdapter = new SimpleAdapter(getApplicationContext(), listItem, R.layout.list_item,
                                new String[]{"name", "mac", "rssi"}, new int[]{R.id.name, R.id.mac, R.id.rssi});
                        ListView listView = (ListView) findViewById(R.id.lv_mac);
                        listView.setAdapter(myAdapter);
                    }
                });
            }
        }

        @Override
        public void onDeviceFound(final BluetoothDevice[] devices) {
            Log.i(TAG, "共发现" + devices.length + "台设备");
            for (int i = 0; i < devices.length; i++) {
                Log.i(TAG, "name:" + devices[i].getName() + "------mac:" + devices[i].getAddress());
                Log.i(TAG, "adv : " + deviceListData.get(i));
            }
            showToast("共发现" + devices.length + "台设备");
        }

        @Override
        public void onScanTimeout() {
            super.onScanTimeout();
            Log.i(TAG, "搜索时间结束");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleManager.closeBluetoothGatt();
        bleManager.disableBluetooth();
    }
}

/*
        findViewById(R.id.btn_01).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (bluetoothDevices == null || bluetoothDevices.length < 1)
                    return;
                BluetoothDevice sampleDevice = bluetoothDevices[0];


                bleManager.connectDevice(sampleDevice, new BleGattCallback() {
                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "连接成功！");
                        gatt.discoverServices();                // 连接上设备后搜索服务
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "服务被发现！");
                        BluetoothUtil.printServices(gatt);            // 打印该设备所有服务、特征值
                        bleManager.getBluetoothState();               // 打印与该设备的当前状态
                    }

                    @Override
                    public void onConnectFailure(BleException exception) {
                        Log.i(TAG, "连接失败或连接中断：" + '\n' + exception.toString());
                        bleManager.handleException(exception);
                    }
                });
            }
        });

        findViewById(R.id.btn_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.connectDevice(
                        DEVICE_NAME,
                        TIME_OUT,
                        new BleGattCallback() {
                            @Override
                            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                                Log.i(TAG, "连接成功！");
                                gatt.discoverServices();                // 连接上设备后搜索服务
                            }

                            @Override
                            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                                Log.i(TAG, "服务被发现！");
                                BluetoothUtil.printServices(gatt);            // 打印该设备所有服务、特征值
                                bleManager.getBluetoothState();               // 打印与该设备的当前状态
                            }

                            @Override
                            public void onConnectFailure(BleException exception) {
                                Log.i(TAG, "连接失败或连接中断：" + '\n' + exception.toString());
                                bleManager.handleException(exception);
                            }
                        });
            }
        });

        findViewById(R.id.btn_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.notifyDevice(
                        UUID_SERVICE_LISTEN,
                        UUID_LISTEN_NOTIFY,
                        UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                        new BleCharacterCallback() {
                            @Override
                            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                                Log.d(TAG, "特征值Notification通知数据回调： "
                                        + '\n' + Arrays.toString(characteristic.getValue())
                                        + '\n' + HexUtil.encodeHexStr(characteristic.getValue()));
                            }

                            @Override
                            public void onFailure(BleException exception) {
                                Log.e(TAG, "特征值Notification通知回调失败: " + '\n' + exception.toString());
                                bleManager.handleException(exception);
                            }
                        });
            }
        });

        findViewById(R.id.btn_3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.indicateDevice(
                        UUID_SERVICE_LISTEN,
                        UUID_LISTEN_INDICATE,
                        UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                        new BleCharacterCallback() {
                            @Override
                            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                                Log.d(TAG, "特征值Indication通知数据回调： "
                                        + '\n' + Arrays.toString(characteristic.getValue())
                                        + '\n' + HexUtil.encodeHexStr(characteristic.getValue()));
                            }

                            @Override
                            public void onFailure(BleException exception) {
                                Log.e(TAG, "特征值Indication通知回调失败: " + '\n' + exception.toString());
                                bleManager.handleException(exception);
                            }
                        });
            }
        });

        findViewById(R.id.btn_4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.writeDevice(
                        UUID_SERVICE_OPERATE,
                        UUID_OPERATE_WRITE,
                        UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                        HexUtil.hexStringToBytes(SAMPLE_WRITE_DATA),
                        new BleCharacterCallback() {
                            @Override
                            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                                Log.d(TAG, "写特征值成功: "
                                        + '\n' + Arrays.toString(characteristic.getValue())
                                        + '\n' + HexUtil.encodeHexStr(characteristic.getValue()));
                            }

                            @Override
                            public void onFailure(BleException exception) {
                                Log.e(TAG, "写读特征值失败: " + '\n' + exception.toString());
                                bleManager.handleException(exception);
                            }
                        });
            }
        });


        findViewById(R.id.btn_6).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.refreshDeviceCache();
            }
        });

        findViewById(R.id.btn_7).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.closeBluetoothGatt();
            }
        });
    }

    BleCharacterCallback bleCharacterCallback = new BleCharacterCallback() {
        @Override
        public void onSuccess(BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "特征值Notification通知数据回调： "
                    + '\n' + Arrays.toString(characteristic.getValue())
                    + '\n' + HexUtil.encodeHexStr(characteristic.getValue()));
        }

        @Override
        public void onFailure(BleException exception) {
            Log.e(TAG, "特征值Notification通知回调失败: " + '\n' + exception.toString());
            bleManager.handleException(exception);
        }
    };

    private void addAndRemove() {

        bleManager.notifyDevice(
                UUID_SERVICE_OPERATE,
                UUID_OPERATE_NOTIFY,
                UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                bleCharacterCallback);

        bleManager.removeBleCharacterCallback(bleCharacterCallback);
    }
*/

