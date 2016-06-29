package com.hypercryptic.bleware;

import java.util.UUID;

import android.annotation.SuppressLint;

import com.hypercryptic.bleware.BLEService.BLESDK;
/**
 * Created by sharukhhasan on 6/28/16.
 */
@SuppressLint("NewApi")
public class GattCharacteristic {
    public static final int PROPERTY_READ = 2;
    public static final int PROPERTY_WRITE = 8;
    public static final int PROPERTY_NOTIFY = 16;
    public static final int PROPERTY_INDICATE = 32;

    public static final int FORMAT_UINT8 = 0x11;
    public static final int FORMAT_UINT16 = 0x12;
    public static final int FORMAT_UINT24 = 0x13;
    public static final int FORMAT_UINT32 = 0x14;
    public static final int FORMAT_SINT8 = 0x21;
    public static final int FORMAT_SINT16 = 0x22;
    public static final int FORMAT_SINT32 = 0x24;
    public static final int FORMAT_SFLOAT = 0x32;
    public static final int FORMAT_FLOAT = 0x34;

    private android.bluetooth.BluetoothGattCharacteristic mGattCharacteristicA;
    private com.broadcom.bt.gatt.BluetoothGattCharacteristic mGattCharacteristicB;
    private com.samsung.android.sdk.bt.gatt.BluetoothGattCharacteristic mGattCharacteristicS;
    private BLESDK mBleSDK;
    private String name;

    public GattCharacteristic(android.bluetooth.BluetoothGattCharacteristic c)
    {
        mBleSDK = BLESDK.ANDROID;
        setGattCharacteristicA(c);
        initInfo();
    }

    public GattCharacteristic(com.broadcom.bt.gatt.BluetoothGattCharacteristic c)
    {
        mBleSDK = BLESDK.BROADCOM;
        setGattCharacteristicB(c);
    }

    public GattCharacteristic(com.samsung.android.sdk.bt.gatt.BluetoothGattCharacteristic c)
    {
        mBleSDK = BLESDK.SAMSUNG;
        setGattCharacteristicS(c);
    }

    private void initInfo()
    {
        name = "Unknown characteristic";
    }

    public UUID getUuid()
    {
        if(mBleSDK == BLESDK.ANDROID)
        {
            return getGattCharacteristicA().getUuid();
        }
        else if(mBleSDK == BLESDK.BROADCOM)
        {
            return getGattCharacteristicB().getUuid();
        }
        else if(mBleSDK == BLESDK.SAMSUNG)
        {
            return getGattCharacteristicS().getUuid();
        }

        return null;
    }

    protected android.bluetooth.BluetoothGattCharacteristic getGattCharacteristicA()
    {
        return mGattCharacteristicA;
    }

    public int getProperties()
    {
        if(mBleSDK == BLESDK.ANDROID)
        {
            return getGattCharacteristicA().getProperties();
        }
        else if(mBleSDK == BLESDK.BROADCOM)
        {
            return getGattCharacteristicB().getProperties();
        }
        else if(mBleSDK == BLESDK.SAMSUNG)
        {
            return getGattCharacteristicS().getProperties();
        }

        return 0;
    }

    public com.broadcom.bt.gatt.BluetoothGattCharacteristic getGattCharacteristicB()
    {
        return mGattCharacteristicB;
    }

    protected void setGattCharacteristicB(com.broadcom.bt.gatt.BluetoothGattCharacteristic mBCGattCharacteristic)
    {
        this.mGattCharacteristicB = mBCGattCharacteristic;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean setValue(byte[] val)
    {
        if(mBleSDK == BLESDK.ANDROID)
        {
            return getGattCharacteristicA().setValue(val);
        }
        else if(mBleSDK == BLESDK.SAMSUNG)
        {
            return mGattCharacteristicS.setValue(val);
        }
        else if(mBleSDK == BLESDK.BROADCOM)
        {
            return mGattCharacteristicB.setValue(val);
        }

        return false;
    }

    public byte[] getValue()
    {
        if(mBleSDK == BLESDK.ANDROID)
        {
            return getGattCharacteristicA().getValue();
        }
        else if(mBleSDK == BLESDK.SAMSUNG)
        {
            return mGattCharacteristicS.getValue();
        }
        else if(mBleSDK == BLESDK.BROADCOM)
        {
            return mGattCharacteristicB.getValue();
        }

        return null;
    }

    public boolean setValue(int value, int formatType, int offset)
    {
        if(mBleSDK == BLESDK.ANDROID)
        {
            return getGattCharacteristicA().setValue(value, formatType, offset);
        }
        else if(mBleSDK == BLESDK.SAMSUNG)
        {
            return mGattCharacteristicS.setValue(value, formatType, offset);
        }
        else if(mBleSDK == BLESDK.BROADCOM)
        {
            return mGattCharacteristicB.setValue(value, formatType, offset);
        }

        return false;
    }

    public boolean setValue(int mantissa, int exponent, int formatType, int offset)
    {
        if(mBleSDK == BLESDK.ANDROID)
        {
            return getGattCharacteristicA().setValue(mantissa, exponent, formatType, offset);
        }
        else if(mBleSDK == BLESDK.SAMSUNG)
        {
            return mGattCharacteristicS.setValue(mantissa, exponent, formatType, offset);
        }
        else if(mBleSDK == BLESDK.BROADCOM)
        {
            return mGattCharacteristicB.setValue(mantissa, exponent, formatType, offset);
        }

        return false;
    }

    public boolean setValue(String value)
    {
        return setValue(value.getBytes());
    }

    public String getStringValue(int offset)
    {
        if(mBleSDK == BLESDK.ANDROID)
        {
            return getGattCharacteristicA().getStringValue(offset);
        }
        else if(mBleSDK == BLESDK.SAMSUNG)
        {
            return mGattCharacteristicS.getStringValue(offset);
        }
        else if(mBleSDK == BLESDK.BROADCOM)
        {
            return mGattCharacteristicB.getStringValue(offset);
        }

        return null;
    }

    public Float getFloatValue(int formatType, int offset)
    {
        if(mBleSDK == BLESDK.ANDROID)
        {
            return getGattCharacteristicA().getFloatValue(formatType, offset);
        }
        else if(mBleSDK == BLESDK.SAMSUNG)
        {
            return mGattCharacteristicS.getFloatValue(formatType, offset);
        }
        else if(mBleSDK == BLESDK.BROADCOM)
        {
            return mGattCharacteristicB.getFloatValue(formatType, offset);
        }

        return null;
    }

    public Integer getIntValue(int formatType, int offset)
    {
        if(mBleSDK == BLESDK.ANDROID)
        {
            if(formatType == FORMAT_UINT24)
            {
                byte[] value = getGattCharacteristicA().getValue();
                return byte2uint24(offset, value);
            }
            else
            {
                return getGattCharacteristicA().getIntValue(formatType, offset);
            }
        }
        else if(mBleSDK == BLESDK.SAMSUNG)
        {
            if(formatType == FORMAT_UINT24)
            {
                byte[] value = mGattCharacteristicS.getValue();
                return byte2uint24(offset, value);
            }
            else
            {
                return mGattCharacteristicS.getIntValue(formatType, offset);
            }
        }
        else if(mBleSDK == BLESDK.BROADCOM)
        {
            if(formatType == FORMAT_UINT24)
            {
                byte[] value = mGattCharacteristicB.getValue();
                return byte2uint24(offset, value);
            }
            else
            {
                return mGattCharacteristicB.getIntValue(formatType, offset);
            }
        }

        return null;
    }

    private Integer byte2uint24(int offset, byte[] value)
    {
        if((offset + 3) > value.length)
        {
            return null;
        }

        return Integer.valueOf((value[offset] & 0xFF) | (value[offset + 1] & 0xFF) << 8 | (value[offset + 2] & 0xFF) << 16);
    }

    public com.samsung.android.sdk.bt.gatt.BluetoothGattCharacteristic getGattCharacteristicS()
    {
        return mGattCharacteristicS;
    }

    protected void setGattCharacteristicS(com.samsung.android.sdk.bt.gatt.BluetoothGattCharacteristic mSSGattCharacteristic)
    {
        this.mGattCharacteristicS = mSSGattCharacteristic;
    }

    protected void setGattCharacteristicA(android.bluetooth.BluetoothGattCharacteristic mGattCharacteristicA)
    {
        this.mGattCharacteristicA = mGattCharacteristicA;
    }
}
