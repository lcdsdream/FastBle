package com.clj.blesample;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.bluetooth.BleGattCallback;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lcd on 9/9/16.
 */
public class ConnectedActivity extends AppCompatActivity {

    private static final String TAG = "ble_sample";

    private static final String UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    private static final String UUID_SERVICE_OPERATE = "0000a800-0000-1000-8000-00805f9b34fb";
    private static final String UUID_OPERATE_WRITE = "0000a802-0000-1000-8000-00805f9b34fb";
    private static final String UUID_OPERATE_NOTIFY = "0000a802-0000-1000-8000-00805f9b34fb";

    private static final String SAMPLE_WRITE_DATA = "2204020b010300";

    private BleManager bleManager;                                                // Ble核心管理类
    private Handler handler = new Handler(Looper.getMainLooper());

    private BleWriteCharacterCallback myWriteGattback = new BleWriteCharacterCallback();
    private BleNtyCharacterCallback myNtyGattback = new BleNtyCharacterCallback();

    private List<Map<String, Object>> logListItem = new ArrayList<Map<String, Object>>();          // show in listView

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.connected);

        bleManager = BleManager.getInstance();
        bleManager.init(this);

        logListItem.clear();

        initListener();
    }

    private void initListener() {

        findViewById(R.id.btn_write).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String cmd = new String(SAMPLE_WRITE_DATA);
                EditText inCmd = (EditText) findViewById(R.id.edit_write);
                if (inCmd.getText().length() > 0) {
                    cmd = inCmd.getText().toString();
                }
                bleManager.writeDevice(UUID_SERVICE_OPERATE, UUID_OPERATE_WRITE, UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                        HexUtil.hexStringToBytes(cmd),
                        myWriteGattback);
            }
        });
        findViewById(R.id.btn_dis).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bleManager.isConnectingOrConnected()) {
                    bleManager.closeBluetoothGatt();
                }
                finish();
            }
        });

        bleManager.addGattCallback(new BleGattCallback() {
            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {}
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {}
            @Override
            public void onConnectFailure(BleException exception) {
                Log.i(TAG, "连接失败或连接中断：" + '\n' + exception.toString());
                showToast("连接断开");
                //bleManager.handleException(exception);
                // quit
                finish();
            }
        });
        //获得Intent对象,并且用Bundle出去里面的数据
        Intent it = getIntent();
        Bundle bd = it.getExtras();
        String mac  = bd.getString("mac");
        String name  = bd.getString("name");
        TextView txtLable = (TextView) findViewById(R.id.txt_lable);
        txtLable.setText(name + " | " + mac);

        if (bleManager.isConnected() && bleManager.isServiceDiscovered()) {
            // set listen
            bleManager.notifyDevice(UUID_SERVICE_OPERATE, UUID_OPERATE_NOTIFY, UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR, myNtyGattback);
            // write sample : test
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bleManager.writeDevice(UUID_SERVICE_OPERATE, UUID_OPERATE_WRITE, UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                            HexUtil.hexStringToBytes(SAMPLE_WRITE_DATA),
                            myWriteGattback);
                }
            }, 500);
        }
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

    private void showLog(final String log) {
         runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> showItem = new HashMap<String, Object>();
                showItem.put("log", "->" + log);
                logListItem.add(0, showItem);

                final List<Map<String, Object>> listItem = logListItem;
                SimpleAdapter myAdapter = new SimpleAdapter(getApplicationContext(), listItem, R.layout.list_item_nty,
                                new String[]{"log"}, new int[]{R.id.txt_log,});
                ListView listView = (ListView) findViewById(R.id.lv_log);
                listView.setAdapter(myAdapter);
            }
        });
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
            showLog(HexUtil.encodeHexStr(characteristic.getValue()));
        }
        @Override
        public void onFailure(BleException exception) {
            Log.e(TAG, "特征值Notification通知回调失败: " + '\n' + exception.toString());
            bleManager.handleException(exception);
        }
    }

}

