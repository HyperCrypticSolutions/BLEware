package com.hypercryptic.bleware;

/**
 * Created by sharukhhasan on 6/28/16.
 */
public class BLERequest {

    public enum RequestType {
        CONNECT_GATT, DISCOVER_SERVICE, CHARACTERISTIC_NOTIFICATION, CHARACTERISTIC_INDICATION, READ_CHARACTERISTIC, READ_DESCRIPTOR, READ_RSSI, WRITE_CHARACTERISTIC, WRITE_DESCRIPTOR, CHARACTERISTIC_STOP_NOTIFICATION
    }

    public enum FailReason {
        START_FAILED, TIMEOUT, RESULT_FAILED
    }

    public RequestType type;
    public String address;
    public GattCharacteristic characteristic;
    public String remark;

    public BLERequest(RequestType type, String address) {
        this.type = type;
        this.address = address;
    }

    public BLERequest(RequestType type, String address, GattCharacteristic characteristic) {
        this.type = type;
        this.address = address;
        this.characteristic = characteristic;
    }

    public BLERequest(RequestType type, String address, GattCharacteristic characteristic, String remark) {
        this.type = type;
        this.address = address;
        this.characteristic = characteristic;
        this.remark = remark;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BLERequest)) {
            return false;
        }

        BLERequest br = (BLERequest) o;
        return (this.type == br.type && this.address.equals(br.address));
    }
}
