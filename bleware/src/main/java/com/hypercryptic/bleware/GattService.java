package com.hypercryptic.bleware;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;

import com.hypercryptic.bleware.BLEService.BLESDK;

/**
 * Created by sharukhhasan on 6/28/16.
 */
@SuppressLint("NewApi")
public class GattService {
    private BLESDK mBleSDK;
    private com.samsung.android.sdk.bt.gatt.BluetoothGattService mGattServiceS;
    private com.broadcom.bt.gatt.BluetoothGattService mGattServiceB;
    private android.bluetooth.BluetoothGattService mGattServiceA;
    private String mName;

    public GattService(com.samsung.android.sdk.bt.gatt.BluetoothGattService s) {
        mBleSDK = BLESDK.SAMSUNG;
        mGattServiceS = s;
        initInfo();
    }

    public GattService(com.broadcom.bt.gatt.BluetoothGattService s) {
        mBleSDK = BLESDK.BROADCOM;
        mGattServiceB = s;
        initInfo();
    }

    public GattService(android.bluetooth.BluetoothGattService s) {
        mBleSDK = BLESDK.ANDROID;
        mGattServiceA = s;
        initInfo();
    }

    private void initInfo() {
        mName = "Unknown Service";
    }

    public UUID getUuid() {
        if (mBleSDK == BLESDK.BROADCOM) {
            return mGattServiceB.getUuid();
        } else if (mBleSDK == BLESDK.SAMSUNG) {
            return mGattServiceS.getUuid();
        } else if (mBleSDK == BLESDK.ANDROID) {
            return mGattServiceA.getUuid();
        }

        return null;
    }

    public List<GattCharacteristic> getCharacteristics() {
        ArrayList<GattCharacteristic> list = new ArrayList<GattCharacteristic>();
        if (mBleSDK == BLESDK.BROADCOM) {
            for (com.broadcom.bt.gatt.BluetoothGattCharacteristic c : mGattServiceB
                    .getCharacteristics()) {
                list.add(new GattCharacteristic(c));
            }
        } else if (mBleSDK == BLESDK.SAMSUNG) {
            for (Object o : mGattServiceS.getCharacteristics()) {
                com.samsung.android.sdk.bt.gatt.BluetoothGattCharacteristic c = (com.samsung.android.sdk.bt.gatt.BluetoothGattCharacteristic) o;
                list.add(new GattCharacteristic(c));
            }
        } else if (mBleSDK == BLESDK.ANDROID) {
            for (android.bluetooth.BluetoothGattCharacteristic c : mGattServiceA
                    .getCharacteristics()) {
                list.add(new GattCharacteristic(c));
            }
        }

        return list;
    }

    public GattCharacteristic getCharacteristic(UUID uuid) {
        if (mBleSDK == BLESDK.ANDROID) {
            BluetoothGattCharacteristic c = mGattServiceA
                    .getCharacteristic(uuid);
            if (c != null) {
                return new GattCharacteristic(c);
            }
        } else if (mBleSDK == BLESDK.SAMSUNG) {
            com.samsung.android.sdk.bt.gatt.BluetoothGattCharacteristic c = mGattServiceS
                    .getCharacteristic(uuid);
            if (c != null) {
                return new GattCharacteristic(c);
            }
        } else if (mBleSDK == BLESDK.BROADCOM) {
            com.broadcom.bt.gatt.BluetoothGattCharacteristic c = mGattServiceB
                    .getCharacteristic(uuid);
            if (c != null) {
                return new GattCharacteristic(c);
            }
        }

        return null;
    }

    public void setInfo(JSONObject info) {
        if (info == null) {
            return;
        }

        try {
            setName(info.getString("name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }
}
