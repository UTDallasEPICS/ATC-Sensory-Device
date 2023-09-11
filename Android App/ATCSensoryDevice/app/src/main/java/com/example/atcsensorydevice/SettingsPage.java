package com.example.atcsensorydevice;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.atcsensorydevice.dialogs.RationaleDialog;
import com.example.atcsensorydevice.utilities.DataManager;

import java.util.Timer;
import java.util.TimerTask;

public class SettingsPage extends AppCompatActivity {
    //Bluetooth
    private BluetoothAdapter btAdapter;

    //Permissions
    private String[] PERMISSIONS;
    private Button requestPermButton;

    //UI Elements
    private TextView btStatusText, permStatusText, timeLimitTitle;
    private EditText timeLimitView;
    private Button saveSettingsButton;

    //Data Access
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_settings);
        initUI();
        setupTouchListeners(findViewById(R.id.settingsContainer));

        //-----Check If Bluetooth Is Enabled-----
        btStatusText = findViewById(R.id.btStatus);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if(!btAdapter.isEnabled()){
            btStatusText.setText("Not Enabled");
            btStatusText.setTextColor(Color.RED);
        }else{
            btStatusText.setText("Enabled");
            btStatusText.setTextColor(Color.GREEN);
        }
        //----                              -------


        //-----Check if the required permissions are already granted----
        requestPermButton = findViewById(R.id.requestPermissionsButton);
        permStatusText = findViewById(R.id.permissionStatus);
        PERMISSIONS = new String[] {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
        };
        setPermStatusText();
        requestPermButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!hasPermissions(SettingsPage.this, PERMISSIONS)){
                    ActivityCompat.requestPermissions(SettingsPage.this, PERMISSIONS, 1);
                }
                displayHelpMessage();
            }
        });
        //------                                                    ------
    }

    //=========================== Bluetooth Settings ===========================

    private void displayHelpMessage() {
        Timer timer = new Timer("HelpMessage", true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setPermStatusText();
                    }
                });
                timer.cancel();
            }
        };
        timer.schedule(task,3000);
        permStatusText.setText("If the dialog doesn't open, check the settings for this app");
    }

    private boolean hasPermissions(Context context, String... PERMISSIONS){
        if(context != null && PERMISSIONS != null){
            for(String permission: PERMISSIONS){
                if(ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED){
                    return false;
                }
            }
        }
        return true;
    }

    private void setPermStatusText(){
        if(!hasPermissions(SettingsPage.this, PERMISSIONS)){
            permStatusText.setText("Not Granted");
            permStatusText.setTextColor(Color.RED);
        }else{
            permStatusText.setText("Granted");
            permStatusText.setTextColor(Color.GREEN);
        }
    }

    private void showRationale() {
        RationaleDialog rationaleDialog = new RationaleDialog(this);
        rationaleDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //Show Rationales for Each Permission
        if (requestCode == 1) {

            for(int i = 0; i < grantResults.length; i++){
                if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                    if(ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSIONS[i])) {
                        showRationale();
                        break;
                    }
                }
            }

            //ACCESS COURSE LOCATION Denied
            //ACCESS FINE LOCATION Denied
            //BLUETOOTH ADMIN Denied
            //BLUETOOTH SCAN Denied
            //BLUETOOTH CONNECT Denied

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setPermStatusText();
    }

    //=========================== Other Settings ===========================

    private void initUI() {
        timeLimitView = findViewById(R.id.editTextNumber);
        saveSettingsButton = findViewById(R.id.saveSettingsButton);
        timeLimitTitle = findViewById(R.id.timeLimitTitle);

        //========================= TIME LIMIT =========================

        int timeLimit = dataManager.setTimeLimitFromFile(getApplicationContext());
        timeLimitView.setText(String.valueOf(timeLimit));

        timeLimitView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                timeLimitView.setCursorVisible(true);
                return false;
            }
        });

        timeLimitView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                timeLimitTitle.setTypeface(timeLimitTitle.getTypeface(), Typeface.BOLD);
            }
        });

        //========================= SAVE SETTINGS =========================
        saveSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String timeLimitString = timeLimitView.getText().toString().trim();
                dataManager.writeTimeLimitToFile(getApplicationContext(), timeLimitString);
                timeLimitTitle.setTypeface(Typeface.create(timeLimitTitle.getTypeface(), Typeface.NORMAL));
            }
        });
    }

    public void setupTouchListeners(View view) {

        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(SettingsPage.this);
                    timeLimitView.setCursorVisible(false);
                    return false;
                }
            });
        }

        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupTouchListeners(innerView);
            }
        }
    }

    private void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if(inputMethodManager.isAcceptingText()){
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }
}
