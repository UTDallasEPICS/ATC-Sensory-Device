package com.example.atcsensorydevice.modules;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.atcsensorydevice.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PairDialogBackup extends AppCompatDialogFragment {
    //Context
    private Context context;

    //ReturnDevice Interface
    private ReturnDevice mCallback;

    //GUI
    private TextView text;
    ListView devicesListView;

    //Bluetooth
    private ArrayList<BluetoothDevice> foundDevices;
    private ArrayAdapter adapter;
    BluetoothManager btManager;
    BluetoothAdapter btAdapter;

    //Bluetooth LE
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private final Handler handler = new Handler();
    private ScanSettings settings;
    private final List<ScanFilter> filters = new ArrayList<>();

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 15000;

    //Permissions
    boolean courseGranted = false;
    boolean fineGranted = false;
    boolean btAdminGranted = false;
    boolean btScanGranted = false;
    boolean btConnectGranted = false;

    public PairDialogBackup() {
    }

    @SuppressLint({"MissingPermission", "SetTextI18n"})
    @RequiresApi(api = Build.VERSION_CODES.S)
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.pair_dialog, null);

        context = builder.getContext();

        text = view.findViewById(R.id.ratDialogTitle);
        text.setText("");

        devicesListView = view.findViewById(R.id.deviceList);
        foundDevices = new ArrayList<>();
        adapter = new ArrayAdapter(context, android.R.layout.simple_list_item_2, android.R.id.text1, foundDevices) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                text1.setTypeface(text1.getTypeface(), Typeface.BOLD);
                text2.setTextSize(11f);
                text1.setText(foundDevices.get(position).getName());
                text2.setText(foundDevices.get(position).getAddress());
                return view;
            }
        };

        //Start
        onResume();

        //Interact With List
        devicesListView.setOnItemClickListener((adapterView, view1, i, l) -> {
            if (scanning) {
                scanning = false;
                bluetoothLeScanner.stopScan(leScanCallback);
            }
            //Connect To Device
            Toast.makeText(context, "You Clicked " + foundDevices.get(i).getName(), Toast.LENGTH_SHORT).show();
            text.setText("Successfully paired " + foundDevices.get(i).getName());
            mCallback.onReturnedDevice(context, foundDevices.get(i));
        });

        builder.setView(view)
                .setNegativeButton("Cancel", (dialogInterface, i) -> {
                    if (scanning) {
                        scanning = false;
                        bluetoothLeScanner.stopScan(leScanCallback);
                    }
                })
                .setPositiveButton("Ok", (dialogInterface, i) -> {

                });
        return builder.create();
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onResume() {
        //Setup Bluetooth
        super.onResume();
        scanDevices();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint({"SetTextI18n", "MissingPermission"})
    private void scanDevices() {
        Context context = getContext();
        assert context != null;
        btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        bluetoothLeScanner = btAdapter.getBluetoothLeScanner();

        //Set Scan Settings
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();

        //Set Scan Filters
        UUID uuid = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");    //UART UUID
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(uuid)).build();
        filters.add(filter);

        //Enable Bluetooth
        if (btAdapter == null) {
            text.setText("Bluetooth Not Supported On This Device");
        } else if (!btAdapter.isEnabled()) {
            text.setText("Turning on Bluetooth");
            Intent btOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(btOn);
        } else {
            text.setText("Bluetooth is on");
            //Start Scanning
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                scanLeDevice();
            }
        }
    }

    //Permission Handler
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Log.d("PERMISSIONS RESULT", result.toString());
                if (result.get(Manifest.permission.ACCESS_COARSE_LOCATION) != null) {
                    courseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    Log.d("PERMISSIONS", "ACCESS_COARSE_LOCATION granted");
                }
                if (result.get(Manifest.permission.ACCESS_FINE_LOCATION) != null) {
                    courseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    Log.d("PERMISSIONS", "ACCESS_FINE_LOCATION granted");
                }
                if (result.get(Manifest.permission.BLUETOOTH_ADMIN) != null) {
                    courseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.BLUETOOTH_ADMIN));
                    Log.d("PERMISSIONS", "BLUETOOTH_ADMIN granted");
                }
                if (result.get(Manifest.permission.BLUETOOTH_SCAN) != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        courseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.BLUETOOTH_SCAN));
                        Log.d("PERMISSIONS", "BLUETOOTH_SCAN granted");
                    }
                }
                if (result.get(Manifest.permission.BLUETOOTH_CONNECT) != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        courseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.BLUETOOTH_CONNECT));
                        Log.d("PERMISSIONS", "BLUETOOTH_CONNECT granted");
                    }
                }
                //onResume();
            }
    );

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void requestPermissions() {
        courseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        btAdminGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        btScanGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        btConnectGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

        List<String> permissionRequests = new ArrayList<>();

        if (!courseGranted) {
            permissionRequests.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (!fineGranted) {
            permissionRequests.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!btAdminGranted) {
            permissionRequests.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        if (!btScanGranted) {
            permissionRequests.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (!btConnectGranted) {
            permissionRequests.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (!permissionRequests.isEmpty()) {
            requestPermissionLauncher.launch(permissionRequests.toArray(new String[0]));
        }
    }

    // Device scan callback.
    private final ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    foundDevices.add(result.getDevice());
                    devicesListView.setAdapter(adapter);
                }
            };

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void scanLeDevice() {
        text.setText(R.string.scan_start);
        if (!scanning) {

            // Stops scanning after a predefined scan period.
            handler.postDelayed(() -> {
                scanning = false;
                try {
                    text.setText(R.string.scan_stop);
                    bluetoothLeScanner.stopScan(leScanCallback);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }, SCAN_PERIOD);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions();
            }
            scanning = true;
            bluetoothLeScanner.startScan(filters, settings, leScanCallback);
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    @Override
    public void onAttach(@NonNull Activity activity){
        super.onAttach(activity);

        try{
            mCallback = (ReturnDevice) activity;
        }catch (ClassCastException e){
            Log.d("MyDialog", "Activity doesn't implement the ReturnDevice interface");
        }
    }
}
