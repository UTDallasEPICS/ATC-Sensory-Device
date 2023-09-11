package com.example.atcsensorydevice.modules;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.atcsensorydevice.utilities.DataManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BLEController {
    //Bluetooth Objects
    private final BluetoothDevice device;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic rxCharacteristic = null;    //Data to be sent
    private BluetoothGattCharacteristic txCharacteristic = null;    //Data to be received

    //Callback Interface to transfer TX data to other activities
    private static LivePressureCallback pressureCallback;

    //Context
    private final Context context;
    private final byte[] zero = {0, 0, 0, 0};
    //private Handler handler;

    public BLEController(Context c, LivePressureCallback pressureCallback, BluetoothDevice d) {
        this.pressureCallback = pressureCallback;
        context = c;
        device = d;
    }

    //Connect to GATT server
    //Pass he callback object so it can act when something pertaining to the gatt server happens
    @SuppressLint("MissingPermission")
    public void connectDevice(BluetoothDevice device) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    @SuppressLint("MissingPermission")
    public void disconnectDevice() {
        bluetoothGatt.disconnect();
    }

    //Will send an integer number that was selected from the slider.
    //This is done by writing the value to the GATT service's characteristic
    @SuppressLint("MissingPermission")
    public void sendMessage(byte[] message){
        //byte[] value = message.getBytes(StandardCharsets.UTF_8);
        if(rxCharacteristic != null){
            rxCharacteristic.setValue(message);
            rxCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            bluetoothGatt.writeCharacteristic(rxCharacteristic);
            if (Arrays.equals(message, zero) ){
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        rxCharacteristic.setValue(0, 0, 0, 0);
                        rxCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                        bluetoothGatt.writeCharacteristic(rxCharacteristic);
                    }
                }, DataManager.getTimeLimit());
            }
        }
    }

    //This is called when an event occurs during the connection to the GATT server
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        //Executed if there is a connection change (like if the client disconnected or reconnected)
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothProfile.STATE_CONNECTED){
                //Start Service Discovery
                Log.i("[BLE]", "CONNECTED. Start service discovery " + bluetoothGatt.discoverServices());
                Intent intent = new Intent();
                intent.setAction("BT_CONNECTION_SUCCESS");
                context.sendBroadcast(intent);
                sendMessage(floatToByteArray(-1));
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                rxCharacteristic = null;
                Log.i("[BLE]", "DISCONNECTED with status " + status);
                Intent intent = new Intent();
                intent.setAction("BT_CONNECTION_FAIL");
                context.sendBroadcast(intent);
            }else{
                Log.i("[BLE]", "unknown state " + newState + " and status " + status);
            }
        }

        //Is called when a service on the GATT server is discovered
        //Services contain characteristics
        //Both the service and characteristics associated with them have a UUID
        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //We need to be sure we are connecting to the correct service
            if(txCharacteristic == null || rxCharacteristic == null){
                for (BluetoothGattService service : gatt.getServices()) {
                    List<BluetoothGattCharacteristic> gattCharacteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic bgc : gattCharacteristics) {
                        int charProp;
                        //Search for UART RX Characteristic
                        if(rxCharacteristic == null){
                            if (bgc.getUuid().toString().toUpperCase().startsWith("6E400002")) {
                                charProp = bgc.getProperties();
                                if (((charProp & BluetoothGattCharacteristic.PROPERTY_WRITE) | (charProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
                                    rxCharacteristic = bgc;
                                    Log.i("[BLE]", "Found RX Characteristic. Ready to Send.");
                                }
                            }
                        }
                        //Search for UART TX Characteristic
                        if(txCharacteristic == null){
                            if (bgc.getUuid().toString().toUpperCase().startsWith("6E400003")) {
                                charProp = bgc.getProperties();
                                if ((charProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                    txCharacteristic = bgc;
                                    //Subscribe to notifications sent by device when this characteristic changes
                                    gatt.setCharacteristicNotification(txCharacteristic, true);
                                    //Client Characteristic Configuration UUID
                                    UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                                    BluetoothGattDescriptor descriptor = txCharacteristic.getDescriptor(uuid);
                                    //System.out.println(descriptor.toString());
                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    gatt.writeDescriptor(descriptor);
                                    Log.i("[BLE]", "Found TX Characteristic. Ready to Receive.");
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            //We care only about reading the pressure level: i.e., the txCharacteristic
            if(characteristic.getUuid().toString().toUpperCase().startsWith("6E400003")){
                byte[] txValue = characteristic.getValue();
                float value = ByteBuffer.wrap(txValue).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                if(pressureCallback != null){
                    pressureCallback.onPressureReceive(value);
                }
                //Log.d("[BLE]", Float.toString(value));
            }
        }
    };

    //Convert Flat to Byte Array (btGattChar.setValue() only supports sending byte arrays)
    //Floating point representation will be in Big Endian
    public byte[] floatToByteArray(float value) {
        byte[] byteArray = new byte[4];
        Integer intBits = Float.floatToIntBits(value);
        Log.v("[DATA]", String.valueOf(intBits));
        for (int i = 0; i < byteArray.length; i++) {
            byteArray[byteArray.length-1-i] = intBits.byteValue();
            intBits = intBits >> 8;
        }
        //This converts a floating point number into a byte array following the IEEE-754 Standard
        return byteArray;
    }
}
