package com.hypercryptic.bleware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import com.hypercryptic.bleware.BLERequest;
import com.hypercryptic.bleware.BLEService;
import com.hypercryptic.bleware.BLERequest.RequestType;

import com.hypercryptic.bleware.GattCharacteristic;
import com.hypercryptic.bleware.GattService;
import com.hypercryptic.bleware.blefeature.BLEActions;
import com.hypercryptic.bleware.blefeature.BLEFeatures;
import com.samsung.android.sdk.bt.gatt.BluetoothGatt;
import com.samsung.android.sdk.bt.gatt.BluetoothGattAdapter;
import com.samsung.android.sdk.bt.gatt.BluetoothGattCallback;
import com.samsung.android.sdk.bt.gatt.BluetoothGattCharacteristic;
import com.samsung.android.sdk.bt.gatt.BluetoothGattDescriptor;
import com.samsung.android.sdk.bt.gatt.BluetoothGattService;

/**
 * Created by sharukhhasan on 6/28/16.
 */
public class SamsungUtil implements BLEFeatures, BLEActions{
    protected static final String TAG = "blelib";

    private BluetoothAdapter mBtAdapter;
    private BLEService mService;
    private BluetoothGatt mBluetoothGatt;
    private boolean mScanning;

    private final BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {
        @Override
        public void onAppRegistered(int status) {
        }

        @Override
        public void onScanResult(BluetoothDevice device, int rssi,
                                 byte[] scanRecord) {
            mService.bleDeviceFound(device, rssi, scanRecord,
                    BLEService.DEVICE_SOURCE_SCAN);
        }

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status,
                                            int newState) {
            if (mBluetoothGatt == null) {
                return;
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnect(device.getAddress());
                mService.bleGattDisConnected(device.getAddress());
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mService.bleGattConnected(device);
                mService.addBleRequest(new BLERequest(
                        RequestType.DISCOVER_SERVICE, device.getAddress()));
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mService.bleGattDisConnected(device.getAddress());
                disconnect(device.getAddress());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothDevice device, int status) {
            String address = device.getAddress();
            if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnect(address);
                mService.bleGattDisConnected(address);
                mService.requestProcessed(address,
                        RequestType.DISCOVER_SERVICE, false);
                return;
            }
            mService.bleServiceDiscovered(device.getAddress());
        }

        @Override
        public void onCharacteristicRead(
                BluetoothGattCharacteristic characteristic, int status) {
            BLERequest request = mService.getCurrentRequest();
            String address = request.address;
            if (status != BluetoothGatt.GATT_SUCCESS) {
                mService.requestProcessed(address,
                        RequestType.READ_CHARACTERISTIC, false);
                return;
            }
            mService.bleCharacteristicRead(address, characteristic.getUuid()
                    .toString(), status, characteristic.getValue());
        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged");
            String address = mService.getNotificationAddress();
            mService.bleCharacteristicChanged(address, characteristic.getUuid()
                    .toString(), characteristic.getValue());
        }

        @Override
        public void onCharacteristicWrite(
                BluetoothGattCharacteristic characteristic, int status) {
            BLERequest request = mService.getCurrentRequest();
            String address = request.address;
            if (status != BluetoothGatt.GATT_SUCCESS) {
                mService.requestProcessed(address,
                        RequestType.WRITE_CHARACTERISTIC, false);
                return;
            }
            mService.bleCharacteristicWrite(address, characteristic.getUuid()
                    .toString(), status);
        };

        @Override
        public void onDescriptorRead(BluetoothGattDescriptor descriptor,
                                     int status) {
            BLERequest request = mService.getCurrentRequest();
            String address = request.address;
            byte[] value = descriptor.getValue();
            byte[] val_set = null;
            if (request.type == RequestType.CHARACTERISTIC_NOTIFICATION) {
                val_set = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
            } else if (request.type == RequestType.CHARACTERISTIC_INDICATION) {
                val_set = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
            } else {
                val_set = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            }

            if (Arrays.equals(value, val_set)) {
                if (request.type == RequestType.CHARACTERISTIC_NOTIFICATION) {
                    mService.bleCharacteristicNotification(address, descriptor
                                    .getCharacteristic().getUuid().toString(), true,
                            status);
                } else if (request.type == RequestType.CHARACTERISTIC_INDICATION) {
                    mService.bleCharacteristicIndication(address, descriptor
                            .getCharacteristic().getUuid().toString(), status);
                } else {
                    mService.bleCharacteristicNotification(address, descriptor
                                    .getCharacteristic().getUuid().toString(), false,
                            status);
                }
                return;
            }

            if (!descriptor.setValue(val_set)) {
                mService.requestProcessed(address, request.type, false);
            }

            mBluetoothGatt.writeDescriptor(descriptor);
        };

        @Override
        public void onDescriptorWrite(BluetoothGattDescriptor descriptor,
                                      int status) {
            BLERequest request = mService.getCurrentRequest();
            String address = request.address;
            if (request.type == RequestType.CHARACTERISTIC_NOTIFICATION
                    || request.type == RequestType.CHARACTERISTIC_INDICATION
                    || request.type == RequestType.CHARACTERISTIC_STOP_NOTIFICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    mService.requestProcessed(address, request.type, false);
                    return;
                }

                if (request.type == RequestType.CHARACTERISTIC_NOTIFICATION) {
                    mService.bleCharacteristicNotification(address, descriptor
                                    .getCharacteristic().getUuid().toString(), true,
                            status);
                } else if (request.type == RequestType.CHARACTERISTIC_INDICATION) {
                    mService.bleCharacteristicIndication(address, descriptor
                            .getCharacteristic().getUuid().toString(), status);
                } else {
                    mService.bleCharacteristicNotification(address, descriptor
                                    .getCharacteristic().getUuid().toString(), false,
                            status);
                }
                return;
            }

        };
    };

    private final BluetoothProfile.ServiceListener mProfileServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mBluetoothGatt = (BluetoothGatt) proxy;
            mBluetoothGatt.registerApp(mGattCallbacks);
        }

        @Override
        public void onServiceDisconnected(int profile) {
            mBluetoothGatt = null;
        }
    };

    public SamsungBLE(BLEService service) {
        mService = service;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            mService.bleNoBtAdapter();
            return;
        }
        BluetoothGattAdapter.getProfileProxy(mService, mProfileServiceListener,
                BluetoothGattAdapter.GATT);
    }

    @Override
    public String getMacAddress() {
        if (mBtAdapter != null) {
            return mBtAdapter.getAddress();
        }
        return null;
    }

    @Override
    public void startScan() {
        if (mScanning) {
            return;
        }

        if (mBluetoothGatt == null) {
            mScanning = false;
            return;
        }

        mScanning = true;
        mBluetoothGatt.startScan();
    }

    @Override
    public void stopScan() {
        if (!mScanning || mBluetoothGatt == null) {
            return;
        }

        mScanning = false;
        mBluetoothGatt.stopScan();
    }

    @Override
    public boolean isAdapterEnabled() {
        if (mBtAdapter != null) {
            return mBtAdapter.isEnabled();
        }
        return false;
    }

    @Override
    public boolean connect(String address) {
        BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
        return mBluetoothGatt.connect(device, false);
    }

    @Override
    public void disconnect(String address) {
        BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
        mBluetoothGatt.cancelConnection(device);
    }

    @Override
    public ArrayList<GattService> getServices(String address) {
        ArrayList<GattService> list = new ArrayList<GattService>();
        BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
        List<BluetoothGattService> services = mBluetoothGatt
                .getServices(device);
        for (BluetoothGattService s : services) {
            list.add(new GattService(s));
        }
        return list;
    }

    @Override
    public boolean requestReadCharacteristic(String address, GattCharacteristic characteristic) {
        mService.addBleRequest(new BLERequest(RequestType.READ_CHARACTERISTIC,
                address, characteristic));
        return true;
    }

    @Override
    public boolean discoverServices(String address) {
        return mBluetoothGatt.discoverServices(mBtAdapter
                .getRemoteDevice(address));
    }

    @Override
    public boolean readCharacteristic(String address, GattCharacteristic characteristic) {
        return mBluetoothGatt.readCharacteristic(characteristic
                .getGattCharacteristicS());
    }

    @Override
    public GattService getService(String address, UUID uuid) {
        BluetoothGattService service = mBluetoothGatt.getService(
                mBtAdapter.getRemoteDevice(address), uuid);
        if (service == null) {
            return null;
        } else {
            return new GattService(service);
        }
    }

    @Override
    public boolean requestCharacteristicNotification(String address, GattCharacteristic characteristic) {
        mService.addBleRequest(new BLERequest(
                RequestType.CHARACTERISTIC_NOTIFICATION, address,
                characteristic));
        return true;
    }

    @Override
    public boolean characteristicNotification(String address, GattCharacteristic characteristic) {
        BluetoothGattCharacteristic c = characteristic.getGattCharacteristicS();

        if (!mBluetoothGatt.setCharacteristicNotification(c, true)) {
            return false;
        }

        BluetoothGattDescriptor descriptor = c
                .getDescriptor(BLEService.DESC_CCC);
        if (descriptor == null) {
            return false;
        }

        return mBluetoothGatt.readDescriptor(descriptor);
    }

    @Override
    public boolean requestWriteCharacteristic(String address, GattCharacteristic characteristic, String remark) {
        mService.addBleRequest(new BLERequest(RequestType.WRITE_CHARACTERISTIC,
                address, characteristic));
        return true;
    }

    @Override
    public boolean writeCharacteristic(String address, GattCharacteristic characteristic) {
        return mBluetoothGatt.writeCharacteristic(characteristic
                .getGattCharacteristicS());
    }

    @Override
    public boolean requestConnect(String address) {
        mService.addBleRequest(new BLERequest(RequestType.CONNECT_GATT, address));
        return true;
    }

    @Override
    public boolean requestIndication(String address, GattCharacteristic characteristic) {
        mService.addBleRequest(new BLERequest(
                RequestType.CHARACTERISTIC_INDICATION, address, characteristic));
        return true;
    }

    @Override
    public boolean requestStopNotification(String address, GattCharacteristic characteristic) {
        mService.addBleRequest(new BLERequest(
                RequestType.CHARACTERISTIC_STOP_NOTIFICATION, address,
                characteristic));
        return true;
    }
}
