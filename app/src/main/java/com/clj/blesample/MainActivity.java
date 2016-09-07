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
import android.widget.AdapterView;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ble_sample";

    private static final String UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    private static final String UUID_SERVICE_OPERATE = "0000a800-0000-1000-8000-00805f9b34fb";
    private static final String UUID_OPERATE_WRITE = "0000a802-0000-1000-8000-00805f9b34fb";
    private static final String UUID_OPERATE_NOTIFY = "0000a802-0000-1000-8000-00805f9b34fb";

    private static final String SAMPLE_WRITE_DATA = "2204020b010300";

    private static final long TIME_OUT = 5000;                                   // 扫描超时时间

    private BleManager bleManager;                                                // Ble核心管理类
    private Handler handler = new Handler(Looper.getMainLooper());

    // scan
    private List<Map<String, Object>> scanListItem = new ArrayList<Map<String, Object>>(); // show in listView
    private List<BluetoothDevice> deviceList = new ArrayList<>(); // scan device list
    private List<String> deviceListData = new ArrayList<>(); // with device's adv data

    private String deviceMac; // target device address
    private MyListScanCallback myListScanCallback = new MyListScanCallback(TIME_OUT);
    private BleConnGattCallback myConnGattCallback = new BleConnGattCallback();
    private BleWriteCharacterCallback myWriteGattback = new BleWriteCharacterCallback();
    private BleNtyCharacterCallback myNtyGattback = new BleNtyCharacterCallback();

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

                bleManager.enableBluetooth();

                if (bleManager.isInScanning()) {
                    return;
                }

                if (bleManager.isConnectingOrConnected()) {
                    bleManager.closeBluetoothGatt();
                }

                scanListItem.clear();
                deviceList.clear();
                deviceListData.clear();

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                // get mac
                EditText etMac = (EditText) findViewById(R.id.et_mac);
                deviceMac = etMac.getText().toString();
                showToast("Start scan...");
                bleManager.scanDevice(myListScanCallback);
            }
        });

        ListView listView= (ListView) this.findViewById(R.id.lv_mac);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (bleManager.isInScanning()) {
                    showToast("Scanning!!");
                    return;
                }

                if (bleManager.isConnectingOrConnected()) {
                    if (bleManager.isConnected()) {
                        showToast("isConnected");
                    } else {
                        showToast("isConnecting");
                    }
                    return;
                }

                ListView listView = (ListView)parent;
                HashMap<String, Object> map = (HashMap<String, Object>) listView.getItemAtPosition(position);
                String name = (String) map.get("name");
                String mac = (String) map.get("mac");
                showToast("Connect with\n" + name + " " + mac);

                // connect device
                for(final BluetoothDevice device : deviceList) {
                    if (device.getAddress().equalsIgnoreCase(mac)) {
                        bleManager.connectDevice(device, myConnGattCallback);
                    }
                }
            }
        });
    }

/***********************************************************************************************************************/
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

            if ( device.getAddress().toLowerCase().matches("^" + deviceMac.toLowerCase() + "(.*)") ) {
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

    class BleWriteCharacterCallback extends BleCharacterCallback {
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
    }

    class  BleNtyCharacterCallback extends BleCharacterCallback {
        @Override
        public void onSuccess(BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "特征值Notification通知数据回调： "
                    + '\n' + HexUtil.encodeHexStr(characteristic.getValue())
                    + '\n' + Arrays.toString(characteristic.getValue())
                    + "\nuuid :" + characteristic.getUuid());
            showToast(HexUtil.encodeHexStr(characteristic.getValue()));
        }
        @Override
        public void onFailure(BleException exception) {
            Log.e(TAG, "特征值Notification通知回调失败: " + '\n' + exception.toString());
            bleManager.handleException(exception);
        }
    }

    class BleConnGattCallback extends BleGattCallback
    {
        @Override
        public void onConnectSuccess(BluetoothGatt gatt, int status) {
            Log.i(TAG, "连接成功！");
            gatt.discoverServices();                // 连接上设备后搜索服务
            showToast("连接成功 : " + gatt.getDevice().getAddress());
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "服务被发现！");
            BluetoothUtil.printServices(gatt);            // 打印该设备所有服务、特征值
            bleManager.getBluetoothState();               // 打印与该设备的当前状态
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    // set listen
                    bleManager.notifyDevice(UUID_SERVICE_OPERATE, UUID_OPERATE_NOTIFY, UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR, myNtyGattback);

                }
            });
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bleManager.writeDevice(UUID_SERVICE_OPERATE, UUID_OPERATE_WRITE, UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                            HexUtil.hexStringToBytes(SAMPLE_WRITE_DATA),
                            myWriteGattback);
                }
            }, 500);
        }
        @Override
        public void onConnectFailure(BleException exception) {
            Log.i(TAG, "连接失败或连接中断：" + '\n' + exception.toString());
            showToast("连接断开");
            bleManager.handleException(exception);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleManager.closeBluetoothGatt();
        bleManager.disableBluetooth();
    }
}

