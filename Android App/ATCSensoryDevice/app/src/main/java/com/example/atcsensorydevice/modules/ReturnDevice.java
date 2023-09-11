package com.example.atcsensorydevice.modules;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;

public interface ReturnDevice {
    void onReturnedDevice(Context context, BluetoothDevice device);
}
