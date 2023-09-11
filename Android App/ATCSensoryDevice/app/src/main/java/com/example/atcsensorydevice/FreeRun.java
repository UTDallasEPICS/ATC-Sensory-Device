package com.example.atcsensorydevice;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.atcsensorydevice.dialogs.PairDialog;
import com.example.atcsensorydevice.modules.BLEController;
import com.example.atcsensorydevice.modules.LivePressureCallback;
import com.example.atcsensorydevice.modules.LivePressureGraph;
import com.example.atcsensorydevice.modules.ReturnDevice;
import com.example.atcsensorydevice.objects.XYPoint;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

public class FreeRun extends AppCompatActivity implements ReturnDevice, LivePressureCallback {
    //Connecting To Bluetooth
    private BLEController controller;
    private boolean connected = false;
    float pressureValue;
    byte[] pressureBytes;

    //Layout Elements
    private Button pairButton, activateButton;
    private TextView number;
    private TextView deviceTitle;
    private TextView deviceAddress;

    //Live Pressure Plot
    private GraphView pressurePlotView;
    private LivePressureGraph livePressureGraph;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.free_run);

        activateButton = findViewById(R.id.activateButton);
        deviceTitle = findViewById(R.id.deviceName);
        deviceAddress = findViewById(R.id.deviceAddress);

        SeekBar pressureSlider = findViewById(R.id.frseekBar);
        number = findViewById(R.id.frpressureNumber);

        pressurePlotView = findViewById(R.id.livePressurePlot);
        livePressureGraph = new LivePressureGraph(pressurePlotView);

        pressureSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                pressureValue = 14.3f + (0.1f)*(value);
                System.out.println(pressureValue);
                number.setText(String.format("%.1f", pressureValue) + " PSI");
                //System.out.println(value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        pairButton = findViewById(R.id.frpairButton);
        pairButton.setOnClickListener(view -> {
            if(!connected){
                openDialog();
            }else{
                //Disconnect Device
                controller.disconnectDevice();
                Toast.makeText(this, "Disconnected " + deviceTitle.getText(), Toast.LENGTH_SHORT).show();

                //Reset Button Text and Display
                pairButton.setText(R.string.pair_device);
                deviceTitle.setText("");
                deviceAddress.setText("");
                activateButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
                connected = false;
            }
        });

        activateButton.setOnClickListener(view -> {
            if(connected){
                //controller.sendMessage(Integer.toString(pressureValue));
                pressureBytes = controller.floatToByteArray(pressureValue);
                controller.sendMessage(pressureBytes);
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }

    private void openDialog() {
        PairDialog pairDialog = new PairDialog();
        pairDialog.show(getSupportFragmentManager(), "pairDialog");
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReturnedDevice(Context context, BluetoothDevice device) {
        //Connect to Device
        controller = new BLEController(context, this, device);
        controller.connectDevice(device);
        connected = true;

        //Activate Test Button
        activateButton.setClickable(true);
        activateButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.accent));
        activateButton.setTextColor(ContextCompat.getColorStateList(context, R.color.white));

        //Set Screen Text
        deviceTitle.setText(device.getName());
        deviceAddress.setText(device.getAddress());

        //Change "Pair" to "Disconnect"
        pairButton.setText(R.string.disconnectDeviceText);
    }

    @Override
    public void onPressureReceive(float value){
        livePressureGraph.onPressureReceive(value);
    }
}
