package com.hypercryptic.bleware;

import com.hypercryptic.bleware.blefeature.BLEActions;
import com.hypercryptic.bleware.blefeature.BLEFeatures;
import com.hypercryptic.bleware.BLERequest.RequestType;
import com.hypercryptic.bleware.BLERequest.FailReason;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Created by sharukhhasan on 6/28/16.
 */
@SuppressLint("NewApi")
public class DeviceBLE implements BLEFeatures, BLEActions{
    protected static final String TAG = "DeviceBLE";

    private BLEService mService;
    private BluetoothAdapter mBtAdapter;
    private Map<String, BluetoothGatt> mBluetoothGatts;
    // private BTQuery btQuery;

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            mService.bleDeviceFound(device, rssi, scanRecord, BLEService.DEVICE_SOURCE_SCAN);
        }
    };

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            String address = gatt.getDevice().getAddress();
            Log.d(TAG, "onConnectionStateChange " + address + " status " + status + " newState " + newState);
            if(status != BluetoothGatt.GATT_SUCCESS)
            {
                disconnect(address);
                mService.bleGattDisConnected(address);
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mService.bleGattConnected(gatt.getDevice());
                mService.addBleRequest(new BLERequest(
                        RequestType.DISCOVER_SERVICE, address));
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mService.bleGattDisConnected(address);
                disconnect(address);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            String address = gatt.getDevice().getAddress();
            Log.d(TAG, "onServicesDiscovered " + address + " status " + status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                mService.requestProcessed(address,
                        RequestType.DISCOVER_SERVICE, false);
                return;
            }
            mService.bleServiceDiscovered(gatt.getDevice().getAddress());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            String address = gatt.getDevice().getAddress();
            Log.d(TAG, "onCharacteristicRead " + address + " status " + status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                mService.requestProcessed(address,
                        RequestType.READ_CHARACTERISTIC, false);
                return;
            }
            // Log.d(TAG, "data " + characteristic.getStringValue(0));
            mService.bleCharacteristicRead(gatt.getDevice().getAddress(),
                    characteristic.getUuid().toString(), status,
                    characteristic.getValue());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            String address = gatt.getDevice().getAddress();
            Log.d(TAG, "onCharacteristicChanged " + address);
            Log.d(TAG, new String(Hex.encodeHex(characteristic.getValue())));
            mService.bleCharacteristicChanged(address, characteristic.getUuid()
                    .toString(), characteristic.getValue());
        }

        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            String address = gatt.getDevice().getAddress();
            Log.d(TAG, "onCharacteristicWrite " + address + " status " + status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                mService.requestProcessed(address,
                        RequestType.WRITE_CHARACTERISTIC, false);
                return;
            }
            mService.bleCharacteristicWrite(gatt.getDevice().getAddress(),
                    characteristic.getUuid().toString(), status);
        };

        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            String address = gatt.getDevice().getAddress();
            Log.d(TAG, "onDescriptorWrite " + address + " status " + status);
            BLERequest request = mService.getCurrentRequest();
            if (request.type == RequestType.CHARACTERISTIC_NOTIFICATION
                    || request.type == RequestType.CHARACTERISTIC_INDICATION
                    || request.type == RequestType.CHARACTERISTIC_STOP_NOTIFICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    mService.requestProcessed(address,
                            RequestType.CHARACTERISTIC_NOTIFICATION, false);
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

    public DeviceBLE(BLEService service) {
        mService = service;
        // btQuery = BTQuery.getInstance();
        if (!mService.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            mService.bleNotSupported();
            return;
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) mService
                .getSystemService(Context.BLUETOOTH_SERVICE);

        mBtAdapter = bluetoothManager.getAdapter();
        if (mBtAdapter == null) {
            mService.bleNoBtAdapter();
        }
        mBluetoothGatts = new HashMap<String, BluetoothGatt>();
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
        mBtAdapter.startLeScan(mLeScanCallback);
    }

    @Override
    public void stopScan() {
        mBtAdapter.stopLeScan(mLeScanCallback);
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
        BluetoothGatt gatt = device.connectGatt(mService, false, mGattCallback);
        if (gatt == null) {
            mBluetoothGatts.remove(address);
            return false;
        } else {
            // TODO: if state is 141, it can be connected again after about 15
            // seconds
            mBluetoothGatts.put(address, gatt);
            return true;
        }
    }

    @Override
    public void disconnect(String address) {
        if (mBluetoothGatts.containsKey(address)) {
            BluetoothGatt gatt = mBluetoothGatts.remove(address);
            if (gatt != null) {
                gatt.disconnect();
                gatt.close();
            }
        }
    }

    @Override
    public ArrayList<GattService> getServices(String address) {
        BluetoothGatt gatt = mBluetoothGatts.get(address);
        if (gatt == null) {
            return null;
        }

        ArrayList<GattService> list = new ArrayList<GattService>();
        List<BluetoothGattService> services = gatt.getServices();
        for (BluetoothGattService s : services) {
            GattService service = new GattService(s);
            // service.setInfo(btQuery.getGattServiceInfo(s.getUuid()));
            list.add(service);
        }
        return list;
    }

    @Override
    public boolean requestReadCharacteristic(String address, GattCharacteristic characteristic) {
        BluetoothGatt gatt = mBluetoothGatts.get(address);
        if (gatt == null || characteristic == null) {
            return false;
        }

        mService.addBleRequest(new BLERequest(RequestType.READ_CHARACTERISTIC,
                gatt.getDevice().getAddress(), characteristic));
        return true;
    }

    public boolean readCharacteristic(String address, GattCharacteristic characteristic) {
        BluetoothGatt gatt = mBluetoothGatts.get(address);
        if (gatt == null) {
            return false;
        }

        return gatt.readCharacteristic(characteristic.getGattCharacteristicA());
    }

    @Override
    public boolean discoverServices(String address) {
        BluetoothGatt gatt = mBluetoothGatts.get(address);
        if (gatt == null) {
            return false;
        }

        boolean ret = gatt.discoverServices();
        if (!ret) {
            disconnect(address);
        }
        return ret;
    }

    @Override
    public GattService getService(String address, UUID uuid) {
        BluetoothGatt gatt = mBluetoothGatts.get(address);
        if (gatt == null) {
            return null;
        }

        BluetoothGattService service = gatt.getService(uuid);
        if (service == null) {
            return null;
        } else {
            return new GattService(service);
        }
    }

    @Override
    public boolean requestCharacteristicNotification(String address, GattCharacteristic characteristic) {
        BluetoothGatt gatt = mBluetoothGatts.get(address);
        if (gatt == null || characteristic == null) {
            return false;
        }

        mService.addBleRequest(new BLERequest(RequestType.CHARACTERISTIC_NOTIFICATION, gatt.getDevice().getAddress(), characteristic));
        return true;
    }

    @Override
    public boolean characteristicNotification(String address, GattCharacteristic characteristic) {
        BLERequest request = mService.getCurrentRequest();
        BluetoothGatt gatt = mBluetoothGatts.get(address);
        if (gatt == null || characteristic == null) {
            return false;
        }

        boolean enable = true;
        if (request.type == RequestType.CHARACTERISTIC_STOP_NOTIFICATION) {
            enable = false;
        }
        BluetoothGattCharacteristic c = characteristic.getGattCharacteristicA();
        if (!gatt.setCharacteristicNotification(c, enable)) {
            return false;
        }

        BluetoothGattDescriptor descriptor = c.getDescriptor(BLEService.DESC_CCC);
        if (descriptor == null) {
            return false;
        }

        byte[] val_set = null;
        if (request.type == RequestType.CHARACTERISTIC_NOTIFICATION) {
            val_set = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
        } else if (request.type == RequestType.CHARACTERISTIC_INDICATION) {
            val_set = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
        } else {
            val_set = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        }
        if (!descriptor.setValue(val_set)) {
            return false;
        }

        return gatt.writeDescriptor(descriptor);
    }

    @Override
    public boolean requestWriteCharacteristic(String address, GattCharacteristic characteristic, String remark) {
        BluetoothGatt gatt = mBluetoothGatts.get(address);
        if (gatt == null || characteristic == null) {
            return false;
        }

        mService.addBleRequest(new BLERequest(RequestType.WRITE_CHARACTERISTIC,
                gatt.getDevice().getAddress(), characteristic, remark));
        return true;
    }

    @Override
    public boolean writeCharacteristic(String address, GattCharacteristic characteristic) {
        BluetoothGatt gatt = mBluetoothGatts.get(address);
        if (gatt == null) {
            return false;
        }

        Log.d("blelib", new String(Hex.encodeHex(characteristic.getGattCharacteristicA().getValue())));
        return gatt.writeCharacteristic(characteristic.getGattCharacteristicA());
    }

    @Override
    public boolean requestConnect(String address) {
        BluetoothGatt gatt = mBluetoothGatts.get(address);
        if (gatt != null && gatt.getServices().size() == 0) {
            return false;
        }

        mService.addBleRequest(new BLERequest(RequestType.CONNECT_GATT, address));
        return true;
    }

    @Override
    public boolean requestIndication(String address, GattCharacteristic characteristic) {
        BluetoothGatt gatt = mBluetoothGatts.get(address);
        if (gatt == null || characteristic == null) {
            return false;
        }

        mService.addBleRequest(new BLERequest(RequestType.CHARACTERISTIC_INDICATION, gatt.getDevice().getAddress(), characteristic));
        return true;
    }

    @Override
    public boolean requestStopNotification(String address, GattCharacteristic characteristic) {
        BluetoothGatt gatt = mBluetoothGatts.get(address);
        if (gatt == null || characteristic == null) {
            return false;
        }

        mService.addBleRequest(new BLERequest(RequestType.CHARACTERISTIC_NOTIFICATION, gatt.getDevice().getAddress(), characteristic));
        return true;
    }
}
