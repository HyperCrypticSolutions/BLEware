package com.hypercryptic.bleware;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.hypercryptic.bleware.blefeature.BLEActions;
import com.hypercryptic.bleware.blefeature.BLEFeatures;
import com.hypercryptic.bleware.BLERequest.RequestType;
import com.hypercryptic.bleware.BLERequest.FailReason;
import com.hypercryptic.bleware.util.BroadcomUtil;
import com.hypercryptic.bleware.util.SamsungUtil;

/**
 * Created by sharukhhasan on 6/28/16.
 */
public class BLEService extends Service {
    private static final String TAG = "BLEService";

    public static final String BLE_NOT_SUPPORTED = "com.xtremeprog.sdk.ble.not_supported";
    public static final String BLE_NO_BT_ADAPTER = "com.xtremeprog.sdk.ble.no_bt_adapter";
    public static final String BLE_STATUS_ABNORMAL = "com.xtremeprog.sdk.ble.status_abnormal";

    public static final String BLE_REQUEST_FAILED = "com.xtremeprog.sdk.ble.request_failed";

    public static final String BLE_DEVICE_FOUND = "com.xtremeprog.sdk.ble.device_found";

    public static final String BLE_GATT_CONNECTED = "com.xtremeprog.sdk.ble.gatt_connected";

    public static final String BLE_GATT_DISCONNECTED = "com.xtremeprog.sdk.ble.gatt_disconnected";

    public static final String BLE_SERVICE_DISCOVERED = "com.xtremeprog.sdk.ble.service_discovered";

    public static final String BLE_CHARACTERISTIC_READ = "com.xtremeprog.sdk.ble.characteristic_read";

    public static final String BLE_CHARACTERISTIC_NOTIFICATION = "com.xtremeprog.sdk.ble.characteristic_notification";

    public static final String BLE_CHARACTERISTIC_INDICATION = "com.xtremeprog.sdk.ble.characteristic_indication";

    public static final String BLE_CHARACTERISTIC_WRITE = "com.xtremeprog.sdk.ble.characteristic_write";

    public static final String BLE_CHARACTERISTIC_CHANGED = "com.xtremeprog.sdk.ble.characteristic_changed";

    public static final String EXTRA_DEVICE = "DEVICE";
    public static final String EXTRA_RSSI = "RSSI";
    public static final String EXTRA_SCAN_RECORD = "SCAN_RECORD";
    public static final String EXTRA_SOURCE = "SOURCE";
    public static final String EXTRA_ADDR = "ADDRESS";
    public static final String EXTRA_CONNECTED = "CONNECTED";
    public static final String EXTRA_STATUS = "STATUS";
    public static final String EXTRA_UUID = "UUID";
    public static final String EXTRA_VALUE = "VALUE";
    public static final String EXTRA_REQUEST = "REQUEST";
    public static final String EXTRA_REASON = "REASON";

    public static final int DEVICE_SOURCE_SCAN = 0;
    public static final int DEVICE_SOURCE_BONDED = 1;
    public static final int DEVICE_SOURCE_CONNECTED = 2;

    public static final UUID DESC_CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public enum BLESDK {
        NOT_SUPPORTED, ANDROID, SAMSUNG, BROADCOM
    }

    private final IBinder mBinder = new LocalBinder();
    private BLESDK mBleSDK;
    private BLEFeatures mBLE;
    private Queue<BLERequest> mRequestQueue = new LinkedList<BLERequest>();
    private BLERequest mCurrentRequest = null;
    private static final int REQUEST_TIMEOUT = 10 * 10;
    private boolean mCheckTimeout = false;
    private int mElapsed = 0;
    private Thread mRequestTimeout;
    private String mNotificationAddress;

    private Runnable mTimeoutRunnable = new Runnable() {
        @Override
        public void run()
        {
            Log.d(TAG, "monitoring thread start");
            mElapsed = 0;
            try {
                while(mCheckTimeout)
                {
                    Thread.sleep(100);
                    mElapsed++;

                    if(mElapsed > REQUEST_TIMEOUT && mCurrentRequest != null)
                    {
                        Log.d(TAG, "-processrequest type " + mCurrentRequest.type + " address " + mCurrentRequest.address + " [timeout]");
                        bleRequestFailed(mCurrentRequest.address, mCurrentRequest.type, FailReason.TIMEOUT);
                        bleStatusAbnormal("-processrequest type " + mCurrentRequest.type + " address " + mCurrentRequest.address + " [timeout]");
                        if(mBLE != null)
                        {
                            mBLE.disconnect(mCurrentRequest.address);
                        }

                        new Thread(new Runnable() {
                            @Override
                            public void run()
                            {
                                mCurrentRequest = null;
                                processNextRequest();
                            }
                        }, "th-ble").start();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.d(TAG, "monitoring thread exception");
            }
            Log.d(TAG, "monitoring thread stop");
        }
    };

    public static IntentFilter getIntentFilter()
    {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLE_NOT_SUPPORTED);
        intentFilter.addAction(BLE_NO_BT_ADAPTER);
        intentFilter.addAction(BLE_STATUS_ABNORMAL);
        intentFilter.addAction(BLE_REQUEST_FAILED);
        intentFilter.addAction(BLE_DEVICE_FOUND);
        intentFilter.addAction(BLE_GATT_CONNECTED);
        intentFilter.addAction(BLE_GATT_DISCONNECTED);
        intentFilter.addAction(BLE_SERVICE_DISCOVERED);
        intentFilter.addAction(BLE_CHARACTERISTIC_READ);
        intentFilter.addAction(BLE_CHARACTERISTIC_NOTIFICATION);
        intentFilter.addAction(BLE_CHARACTERISTIC_WRITE);
        intentFilter.addAction(BLE_CHARACTERISTIC_CHANGED);
        return intentFilter;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    public class LocalBinder extends Binder
    {
        public BLEService getService()
        {
            return BLEService.this;
        }
    }

    @Override
    public void onCreate()
    {
        mBleSDK = getBleSDK();
        if(mBleSDK == BLESDK.NOT_SUPPORTED)
        {
            return;
        }

        Log.d(TAG, " " + mBleSDK);
        if(mBleSDK == BLESDK.BROADCOM)
        {
            mBLE = new BroadcomUtil(this);
        }
        else if(mBleSDK == BLESDK.ANDROID)
        {
            mBLE = new DeviceBLE(this);
        }
        else if(mBleSDK == BLESDK.SAMSUNG)
        {
            mBLE = new SamsungUtil(this);
        }
    }

    protected void bleNotSupported()
    {
        Intent intent = new Intent(BLEService.BLE_NOT_SUPPORTED);
        sendBroadcast(intent);
    }

    protected void bleNoBtAdapter()
    {
        Intent intent = new Intent(BLEService.BLE_NO_BT_ADAPTER);
        sendBroadcast(intent);
    }

    private BLESDK getBleSDK()
    {
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            return BLESDK.ANDROID;
        }

        ArrayList<String> libraries = new ArrayList<String>();
        for(String i : getPackageManager().getSystemSharedLibraryNames())
        {
            libraries.add(i);
        }

        if(android.os.Build.VERSION.SDK_INT >= 17)
        {
            if(libraries.contains("com.samsung.android.sdk.bt"))
            {
                return BLESDK.SAMSUNG;
            }
            else if(libraries.contains("com.broadcom.bt"))
            {
                return BLESDK.BROADCOM;
            }
        }

        bleNotSupported();
        return BLESDK.NOT_SUPPORTED;
    }

    public BLEFeatures getBle()
    {
        return mBLE;
    }

    protected void bleDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord, int source)
    {
        Log.d("blelib", "[" + new Date().toLocaleString() + "] device found " + device.getAddress());
        Intent intent = new Intent(BLEService.BLE_DEVICE_FOUND);
        intent.putExtra(BLEService.EXTRA_DEVICE, device);
        intent.putExtra(BLEService.EXTRA_RSSI, rssi);
        intent.putExtra(BLEService.EXTRA_SCAN_RECORD, scanRecord);
        intent.putExtra(BLEService.EXTRA_SOURCE, source);
        sendBroadcast(intent);
    }

    protected void bleGattConnected(BluetoothDevice device)
    {
        Intent intent = new Intent(BLE_GATT_CONNECTED);
        intent.putExtra(EXTRA_DEVICE, device);
        intent.putExtra(EXTRA_ADDR, device.getAddress());
        sendBroadcast(intent);
        requestProcessed(device.getAddress(), RequestType.CONNECT_GATT, true);
    }

    protected void bleGattDisConnected(String address)
    {
        Intent intent = new Intent(BLE_GATT_DISCONNECTED);
        intent.putExtra(EXTRA_ADDR, address);
        sendBroadcast(intent);
        requestProcessed(address, RequestType.CONNECT_GATT, false);
    }

    protected void bleServiceDiscovered(String address)
    {
        Intent intent = new Intent(BLE_SERVICE_DISCOVERED);
        intent.putExtra(EXTRA_ADDR, address);
        sendBroadcast(intent);
        requestProcessed(address, RequestType.DISCOVER_SERVICE, true);
    }

    protected void requestProcessed(String address, RequestType requestType, boolean success)
    {
        if(mCurrentRequest != null && mCurrentRequest.type == requestType)
        {
            clearTimeoutThread();
            Log.d(TAG, "-processrequest type " + requestType + " address " + address + " [success: " + success + "]");
            if(!success)
            {
                bleRequestFailed(mCurrentRequest.address, mCurrentRequest.type, FailReason.RESULT_FAILED);
            }

            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    mCurrentRequest = null;
                    processNextRequest();
                }
            }, "th-ble").start();
        }
    }

    private void clearTimeoutThread()
    {
        if(mRequestTimeout.isAlive())
        {
            try {
                mCheckTimeout = false;
                mRequestTimeout.join();
                mRequestTimeout = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void bleCharacteristicRead(String address, String uuid, int status, byte[] value)
    {
        Intent intent = new Intent(BLE_CHARACTERISTIC_READ);
        intent.putExtra(EXTRA_ADDR, address);
        intent.putExtra(EXTRA_UUID, uuid);
        intent.putExtra(EXTRA_STATUS, status);
        intent.putExtra(EXTRA_VALUE, value);
        sendBroadcast(intent);
        requestProcessed(address, RequestType.READ_CHARACTERISTIC, true);
    }

    protected void addBleRequest(BLERequest request)
    {
        synchronized(mRequestQueue)
        {
            mRequestQueue.add(request);
            processNextRequest();
        }
    }

    private synchronized void processNextRequest()
    {
        if(mCurrentRequest != null)
        {
            return;
        }

        if(mRequestQueue.isEmpty())
        {
            return;
        }
        mCurrentRequest = mRequestQueue.remove();
        Log.d(TAG, "+processrequest type " + mCurrentRequest.type + " address " + mCurrentRequest.address + " remark " + mCurrentRequest.remark);

        startTimeoutThread();
        boolean ret = false;
        switch(mCurrentRequest.type) {
            case CONNECT_GATT:
                ret = ((BLEActions) mBLE).connect(mCurrentRequest.address);
                break;
            case DISCOVER_SERVICE:
                ret = mBLE.discoverServices(mCurrentRequest.address);
                break;
            case CHARACTERISTIC_NOTIFICATION:
            case CHARACTERISTIC_INDICATION:
            case CHARACTERISTIC_STOP_NOTIFICATION:
                ret = ((BLEActions) mBLE).characteristicNotification(
                        mCurrentRequest.address, mCurrentRequest.characteristic);
                break;
            case READ_CHARACTERISTIC:
                ret = ((BLEActions) mBLE).readCharacteristic(
                        mCurrentRequest.address, mCurrentRequest.characteristic);
                break;
            case WRITE_CHARACTERISTIC:
                ret = ((BLEActions) mBLE).writeCharacteristic(
                        mCurrentRequest.address, mCurrentRequest.characteristic);
                break;
            case READ_DESCRIPTOR:
                break;
            default:
                break;
        }

        if(!ret)
        {
            clearTimeoutThread();
            Log.d(TAG, "-processrequest type " + mCurrentRequest.type + " address " + mCurrentRequest.address + " [fail start]");
            bleRequestFailed(mCurrentRequest.address, mCurrentRequest.type, FailReason.START_FAILED);
            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    mCurrentRequest = null;
                    processNextRequest();
                }
            }, "th-ble").start();
        }
    }

    private void startTimeoutThread()
    {
        mCheckTimeout = true;
        mRequestTimeout = new Thread(mTimeoutRunnable);
        mRequestTimeout.start();
    }

    protected BLERequest getCurrentRequest()
    {
        return mCurrentRequest;
    }

    protected void setCurrentRequest(BLERequest mCurrentRequest)
    {
        this.mCurrentRequest = mCurrentRequest;
    }

    protected void bleCharacteristicNotification(String address, String uuid, boolean isEnabled, int status)
    {
        Intent intent = new Intent(BLE_CHARACTERISTIC_NOTIFICATION);
        intent.putExtra(EXTRA_ADDR, address);
        intent.putExtra(EXTRA_UUID, uuid);
        intent.putExtra(EXTRA_VALUE, isEnabled);
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
        if(isEnabled)
        {
            requestProcessed(address, RequestType.CHARACTERISTIC_NOTIFICATION, true);
        }
        else
        {
            requestProcessed(address, RequestType.CHARACTERISTIC_STOP_NOTIFICATION, true);
        }
        setNotificationAddress(address);
    }

    protected void bleCharacteristicIndication(String address, String uuid, int status)
    {
        Intent intent = new Intent(BLE_CHARACTERISTIC_INDICATION);
        intent.putExtra(EXTRA_ADDR, address);
        intent.putExtra(EXTRA_UUID, uuid);
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
        requestProcessed(address, RequestType.CHARACTERISTIC_INDICATION, true);
        setNotificationAddress(address);
    }

    protected void bleCharacteristicWrite(String address, String uuid, int status)
    {
        Intent intent = new Intent(BLE_CHARACTERISTIC_WRITE);
        intent.putExtra(EXTRA_ADDR, address);
        intent.putExtra(EXTRA_UUID, uuid);
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
        requestProcessed(address, RequestType.WRITE_CHARACTERISTIC, true);
    }

    protected void bleCharacteristicChanged(String address, String uuid, byte[] value)
    {
        Intent intent = new Intent(BLE_CHARACTERISTIC_CHANGED);
        intent.putExtra(EXTRA_ADDR, address);
        intent.putExtra(EXTRA_UUID, uuid);
        intent.putExtra(EXTRA_VALUE, value);
        sendBroadcast(intent);
    }

    protected void bleStatusAbnormal(String reason)
    {
        Intent intent = new Intent(BLE_STATUS_ABNORMAL);
        intent.putExtra(EXTRA_VALUE, reason);
        sendBroadcast(intent);
    }

    protected void bleRequestFailed(String address, RequestType type, FailReason reason)
    {
        Intent intent = new Intent(BLE_REQUEST_FAILED);
        intent.putExtra(EXTRA_ADDR, address);
        intent.putExtra(EXTRA_REQUEST, type);
        intent.putExtra(EXTRA_REASON, reason.ordinal());
        sendBroadcast(intent);
    }

    protected String getNotificationAddress()
    {
        return mNotificationAddress;
    }

    protected void setNotificationAddress(String mNotificationAddress)
    {
        this.mNotificationAddress = mNotificationAddress;
    }
}