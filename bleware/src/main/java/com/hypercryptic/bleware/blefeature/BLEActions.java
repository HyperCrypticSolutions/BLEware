package com.hypercryptic.bleware.blefeature;

import com.hypercryptic.bleware.GattCharacteristic;

/**
 * Created by sharukhhasan on 6/28/16.
 */
public interface BLEActions {

    public boolean connect(String address);

    public boolean readCharacteristic(String address, GattCharacteristic characteristic);

    public boolean characteristicNotification(String address, GattCharacteristic characteristic);

    public boolean writeCharacteristic(String address, GattCharacteristic characteristic);
}
