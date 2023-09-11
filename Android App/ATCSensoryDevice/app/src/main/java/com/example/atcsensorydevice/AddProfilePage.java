package com.example.atcsensorydevice;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.atcsensorydevice.dialogs.PairDialog;
import com.example.atcsensorydevice.modules.BLEController;
import com.example.atcsensorydevice.modules.LivePressureCallback;
import com.example.atcsensorydevice.modules.LivePressureGraph;
import com.example.atcsensorydevice.modules.ReturnDevice;
import com.example.atcsensorydevice.objects.Profile;
import com.example.atcsensorydevice.utilities.DataManager;
import com.google.android.material.textfield.TextInputEditText;
import com.jjoe64.graphview.GraphView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;


public class AddProfilePage extends AppCompatActivity implements ReturnDevice, LivePressureCallback {
    //Profile Database
    private final String FILENAME = "users.dat";

    //UI Elements
    private Button uploadImageButton;
    private ImageView profileImageView;
    private Button addProfileButton;
    private Button cancelButton;
    private Button pairButton;
    private Button testButton;

    private TextInputEditText name;
    private String title = "N/A";
    private String address = "N/A";

    private TextView number, deviceTitle, deviceAddress;
    private Intent intent;
    private int pos;

    //Connecting to Bluetooth Device
    private BLEController controller;
    private boolean connected = false;
    private float pressureValue;
    byte[] pressureBytes;

    //Live Pressure Graph
    private GraphView pressurePlotView;
    private LivePressureGraph livePressureGraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_profile);
        intent = getIntent();

        //pos tells if this page was redirected from a profile button
        //if so, it exists in the array, so send its index
        pos = intent.getIntExtra("position", -1);
        System.out.println("POS: " + pos);

        //Input Name Field
        name = findViewById(R.id.inputFieldName);

        /*
        //Profile Image
        //TODO: Upload and save image to Profile object (optional)
        profileImageView = findViewById(R.id.profileImage);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        profileImageView.setImageResource(R.drawable.ic_baseline_account_box_24);
        ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            Intent data = result.getData();
                            profileImageView.setImageURI(data.getData());
                        }
                    }
                });
        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent iGallery = new Intent(Intent.ACTION_PICK);
                iGallery.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                someActivityResultLauncher.launch(iGallery);
            }
        });*/

        //Pressure Slider
        SeekBar slider = findViewById(R.id.seekBar);
        slider.setProgress(0);
        number = findViewById(R.id.pressureNumber);

        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                pressureValue = 14.3f + (0.1f)*(value);
                System.out.println(pressureValue);
                number.setText(String.format("%.1f", pressureValue) + " PSI");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //Pressure Monitor
        pressurePlotView = findViewById(R.id.livePressurePlot);
        livePressureGraph = new LivePressureGraph(pressurePlotView);

        //Paired Device Name and Address
        deviceTitle = findViewById(R.id.deviceName);
        deviceAddress = findViewById(R.id.deviceAddress);

        //Pair Bluetooth
        //Create new Bluetooth device
        pairButton = findViewById(R.id.pairButton);
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
                testButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
                connected = false;
            }
        });

        //Fill in Profile info if this is an edit
        if(exists(pos)){
            name.setText(DataManager.getProfilesList().get(pos).getName());
            int sliderValue = (int)((DataManager.getProfilesList().get(pos).getPressure()-14.3)/0.1);
            slider.setProgress(sliderValue);
            address = DataManager.getProfilesList().get(pos).getAddress();
            title = DataManager.getProfilesList().get(pos).getDeviceTitle();
            deviceTitle.setText(title);
            deviceAddress.setText(address);
            pairButton.setText("Pair New Device");
        }

        testButton = findViewById(R.id.testButton);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(connected){
                    //Send Data
                    pressureBytes = controller.floatToByteArray(pressureValue);
                    controller.sendMessage(pressureBytes);
                }
            }
        });

        //Add Profile Button (Save)
        addProfileButton = findViewById(R.id.addProfileButton);
        addProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!title.equals("N/A") || !address.equals("N/A")){
                    createProfile();
                    gotoProfilesPage();
                }

            }
        });

        //Cancel Button
        cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoProfilesPage();
            }
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReturnedDevice(Context context, BluetoothDevice device) {
        //Connect to Device
        controller = new BLEController(context, this, device);
        controller.connectDevice(device);

        //Arm Test Button
        connected = true;
        testButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.accent));
        testButton.setTextColor(ContextCompat.getColorStateList(context, R.color.white));
        testButton.setClickable(true);

        //Set Screen Text
        address = device.getAddress();
        title = device.getName();
        deviceTitle.setText(title);
        deviceAddress.setText(address);

        //Change "Pair" to "Disconnect"
        pairButton.setText(R.string.disconnectDeviceText);
    }

    @Override
    public void onPressureReceive(float value){
        livePressureGraph.onPressureReceive(value);
    }

    @Override
    public void onBackPressed() {
        //Do Nothing
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

    private void gotoProfilesPage() {
        //Disconnect from Bluetooth Device if there is a connection
        if(connected){
            controller.disconnectDevice();
            connected = false;
        }
        Intent intent = new Intent(this, ProfilesPage.class);
        startActivity(intent);
    }

    private void createProfile(){
        Profile newProfile = new Profile(name.getText().toString().trim(), pressureValue, title, address);
        ArrayList<Profile> list = DataManager.getProfilesList();

        if(!exists(pos)){
            list.add(newProfile);
        }else{
            list.set(pos, newProfile);
        }


        //Debugging: Print list
        for(int the = 0; the < list.size(); the++){
            System.out.println(list.get(the).getName() + list.get(the).getPressure()+" ");
        }

        ObjectOutputStream output = null;

        try {
            output = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), FILENAME)));
            output.writeObject(list);
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            if(output != null){
                try {
                    output.close();
                    if(exists(pos)){
                        Toast.makeText(this, "Edit Successful", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(this, "Profile created", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean exists(int p){
        return p != -1;
    }
}
