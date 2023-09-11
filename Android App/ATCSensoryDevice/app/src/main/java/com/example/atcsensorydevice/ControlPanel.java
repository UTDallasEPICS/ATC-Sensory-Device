package com.example.atcsensorydevice;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;

import com.example.atcsensorydevice.modules.BLEController;
import com.example.atcsensorydevice.modules.LivePressureCallback;
import com.example.atcsensorydevice.modules.LivePressureGraph;
import com.example.atcsensorydevice.utilities.DataManager;
import com.jjoe64.graphview.GraphView;

public class ControlPanel extends AppCompatActivity implements AdapterView.OnItemSelectedListener, LivePressureCallback {
    //Connecting To Device
    private Button connectButton;
    private Group connectingAnim;
    private BLEController controller;
    private BluetoothDevice device;
    private boolean justDisconnected = false;

    //Profile Info
    float pressureValue;

    //Start Button
    private Button startButton;

    //Normal Mode
    private Group normalGroup;

    //Manual Mode
    private Group manualGroup;

    //Timer
    private Group timerGroup;
    private TextView timeText;
    private ProgressBar timerProgressBar;
    private final int globalTimeLimit = DataManager.getTimeLimit();
    private int timeLimit;

    private LivePressureGraph livePressureGraph;

    //For Edits
    private int pos;

    //Broadcast Receiver for Bluetooth Connection State
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            boolean connected = false;
            if (action.equals("BT_CONNECTION_SUCCESS")) {
                //Toast.makeText(getApplicationContext(), "Connection Successful!", Toast.LENGTH_SHORT).show();
                if(!justDisconnected){
                    connected = true;
                    justDisconnected = false;
                    connectingAnim.setVisibility(View.GONE);
                    connectButton.setText("Disconnect");
                    connectButton.setOnClickListener(disconnectButton());
                    startButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.accent));
                    startButton.setTextColor(ContextCompat.getColorStateList(context, R.color.white));
                    startButton.setClickable(true);
                }
            } else if (action.equals("BT_CONNECTION_FAIL")) {
                connected = false;
                connectingAnim.setVisibility(View.GONE);
                //Toast.makeText(getApplicationContext(), "Connection Unsuccessful!", Toast.LENGTH_SHORT).show();
                startButton.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), android.R.color.darker_gray));
                startButton.setTextColor(ContextCompat.getColorStateList(context, R.color.black));
                startButton.setClickable(false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_panel);

        Intent intent = getIntent();

        //Display Profile Information
        TextView title = findViewById(R.id.controlTitle);
        title.setText(intent.getStringExtra("profileName"));

        TextView subtitle = findViewById(R.id.controlSubtitle);
        pressureValue = intent.getFloatExtra("profileVal", 0.0f);
        subtitle.setText(Float.toString(pressureValue));

        TextView profileDeviceName = findViewById(R.id.deviceName);
        profileDeviceName.setText(intent.getStringExtra("profileDeviceName"));

        TextView profileDeviceAddress = findViewById(R.id.deviceAddress);
        String address = intent.getStringExtra("profileDeviceAddress");
        profileDeviceAddress.setText(address);

        //UI Elements
        normalGroup = findViewById(R.id.normalModeGroup);
        manualGroup = findViewById(R.id.manualGroup);
        timerGroup = findViewById(R.id.timerGroup);
        timeText = findViewById(R.id.timeText);
        timerProgressBar = findViewById(R.id.timerProgressBar);
        timerProgressBar.setProgress(0);

        connectingAnim = findViewById(R.id.connectingAnimGroup);

        //Pass to BLEController to receive pressure information
        LivePressureCallback pressureCallback = this;

        //Bluetooth Setup
        BluetoothManager btManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager.getAdapter();
        device = btAdapter.getRemoteDevice(address);
        controller = new BLEController(getApplicationContext(), pressureCallback, device);

        //Create Broadcast Receiver to Read the Connection State
        IntentFilter filter = new IntentFilter();
        filter.addAction("BT_CONNECTION_SUCCESS");
        filter.addAction("BT_CONNECTION_FAIL");
        this.registerReceiver(mReceiver, filter);

        //Connect Button
        connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(connectButton());

        //Start Button
        startButton = findViewById(R.id.startButton);
        startButton.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), android.R.color.darker_gray));
        startButton.setTextColor(ContextCompat.getColorStateList(this, R.color.black));
        startButton.setClickable(false);

        //Mode Menu
        Spinner spinner = findViewById(R.id.modeMenu);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.modes_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        //Timer Seek Bar
        SeekBar timerBar = findViewById(R.id.timerSeekBar);
        timerBar.setMin(1);
        timerBar.setMax(globalTimeLimit);
        timerBar.setProgress(0);
        timerBar.setOnSeekBarChangeListener(getOnChanged());

        //Live Pressure Graph
        //Live Pressure Plot
        GraphView pressurePlotView = (GraphView) findViewById(R.id.livePressurePlot);
        livePressureGraph = new LivePressureGraph(pressurePlotView);

        //Edit Button
        Button editButton = findViewById(R.id.editButton);
        pos = intent.getIntExtra("position", -1);
        editButton.setOnClickListener(view -> {
            Intent gotoEditPage = new Intent(getApplicationContext(), AddProfilePage.class);
            gotoEditPage.putExtra("position", pos);
            startActivity(gotoEditPage);
        });
    }

    @Override
    public void onBackPressed() {
        try{
            controller.disconnectDevice();
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        Intent intent = new Intent(getApplicationContext(), ProfilesPage.class);
        startActivity(intent);
    }

    @SuppressLint("ResourceType")
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        //Set motor and valve off
        controller.sendMessage(controller.floatToByteArray(-1));
        if(adapterView.getItemAtPosition(i).toString().equals("Timer")){
            //Timer
            normalGroup.setVisibility(View.GONE);
            manualGroup.setVisibility(View.GONE);
            timerGroup.setVisibility(View.VISIBLE);

            startButton.setOnClickListener(getTimerClick());
        }else if(adapterView.getItemAtPosition(i).toString().equals("Manual")){
            //Manual
            normalGroup.setVisibility(View.GONE);
            manualGroup.setVisibility(View.VISIBLE);
            timerGroup.setVisibility(View.GONE);

            startButton.setOnTouchListener(getManualTouch());
        }else{
            //Normal
            normalGroup.setVisibility(View.VISIBLE);
            manualGroup.setVisibility(View.GONE);
            timerGroup.setVisibility(View.GONE);

            startButton.setOnClickListener(getNormalClick());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        startButton.setOnClickListener(getNothingClick());
    }

    @Override
    public void onPressureReceive(float value) {
        Log.d("CONTROL PANEL", String.valueOf(value));
        livePressureGraph.onPressureReceive(value);
    }

    private View.OnClickListener connectButton() {
        return view -> {
            controller.connectDevice(device);
            connectingAnim.setVisibility(View.VISIBLE);
        };
    }

    private View.OnClickListener disconnectButton() {
        return view -> {
            controller.disconnectDevice();
            justDisconnected = true;
            connectButton.setText("Connect");
            connectButton.setOnClickListener(connectButton());
        };
    }

    private View.OnClickListener getNothingClick() {
        return view -> {
            //Toast.makeText(getApplicationContext(), "Nothing", Toast.LENGTH_SHORT).show();
        };
    }

    private View.OnClickListener getNormalClick(){
        return view -> controller.sendMessage(controller.floatToByteArray(pressureValue));
    }

    private View.OnTouchListener getManualTouch() {
        return (view, motionEvent) -> {
            if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                // Toast.makeText(getApplicationContext(), "Manual", Toast.LENGTH_SHORT).show();
                controller.sendMessage(controller.floatToByteArray(pressureValue));
                return true;
            }else if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                controller.sendMessage(controller.floatToByteArray(0));
                return false;
            }
            return false;
        };
    }

    private View.OnClickListener getTimerClick() {
        return view -> {
            int timeLimitInMillis = timeLimit * 1000;
            Toast.makeText(getApplicationContext(), Integer.toString(timeLimit), Toast.LENGTH_SHORT).show();

            TimerBarTimer timerBarTimer = new TimerBarTimer(timeLimitInMillis, 100);
            timerBarTimer.start();

            controller.sendMessage(controller.floatToByteArray(pressureValue));

            new Handler(Looper.getMainLooper()).postDelayed(() -> controller.sendMessage(controller.floatToByteArray(0)), timeLimitInMillis); //Time is in milliseconds
        };
    }

    private SeekBar.OnSeekBarChangeListener getOnChanged() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                timeLimit = value;
                timeText.setText(value + " seconds");
                System.out.println(value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
    }

    public class TimerBarTimer extends CountDownTimer {

        private final long totalMillis;

        public TimerBarTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
            totalMillis = millisInFuture;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            Log.d("Time in millis", String.valueOf(millisUntilFinished));
            Log.d("Total millis", String.valueOf(totalMillis));
            int progress = (int) (100-(100*millisUntilFinished/totalMillis));
            Log.d("PROGRESS", String.valueOf(progress));
            timerProgressBar.setProgress(progress);
        }

        @Override
        public void onFinish() {
            //Log.d("Time in millis", "Timer finished");
            timerProgressBar.setProgress(100);
        }
    }
}
