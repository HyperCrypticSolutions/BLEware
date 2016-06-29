package com.hypercryptic.bleware.blefeature;

import com.hypercryptic.bleware.GattCharacteristic;
import com.hypercryptic.bleware.GattService;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by sharukhhasan on 6/28/16.
 */
public interface BLEFeatures {

    public String getMacAddress();

    public void startScan();

    public void stopScan();

    public boolean isAdapterEnabled();

    public void disconnect(String address);

    public boolean discoverServices(String address);

    public ArrayList<GattService> getServices(String address);

    public GattService getService(String address, UUID uuid);

    public boolean requestConnect(String address);

    public boolean requestReadCharacteristic(String address, GattCharacteristic characteristic);

    public boolean requestCharacteristicNotification(String address, GattCharacteristic characteristic);

    public boolean requestStopNotification(String address, GattCharacteristic characteristic);

    public boolean requestIndication(String address, GattCharacteristic characteristic);

    public boolean requestWriteCharacteristic(String address, GattCharacteristic characteristic, String remark);
}
