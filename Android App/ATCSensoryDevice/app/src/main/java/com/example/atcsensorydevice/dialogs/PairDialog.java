package com.example.atcsensorydevice.dialogs;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.atcsensorydevice.R;
import com.example.atcsensorydevice.modules.ReturnDevice;
import com.example.atcsensorydevice.modules.ScanResultsAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PairDialog extends DialogFragment implements ScanResultsAdapter.OnNoteListener {
    //Context
    private Context context;

    //ReturnDevice Interface
    private ReturnDevice mCallback;

    //GUI
    private TextView text;
    private ProgressBar progressBar;
    RecyclerView devicesRecyclerView;
    ScanCountdownTimer scanCountdownTimer;

    //Bluetooth
    private ArrayList<BluetoothDevice> foundDevices;
    private ScanResultsAdapter adapter;
    BluetoothManager btManager;
    BluetoothAdapter btAdapter;

    //Bluetooth LE Scanning
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private final Handler handler = new Handler();
    private final List<ScanFilter> filters = new ArrayList<>();

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    public PairDialog() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.PairDialog);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.pair_dialog, null);

        context = getContext();

        //GUI Setup
        text = view.findViewById(R.id.ratDialogTitle);
        devicesRecyclerView = view.findViewById(R.id.deviceList);
        progressBar = view.findViewById(R.id.scanProgress);
        progressBar.setProgress(0);
        scanCountdownTimer = new ScanCountdownTimer(SCAN_PERIOD, 100);
        scanCountdownTimer.start();

        //Instantiate Results Array
        foundDevices = new ArrayList<>();

        //Start Scanning
        scanDevices();

        adapter = new ScanResultsAdapter(context, foundDevices, this);
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        devicesRecyclerView.setAdapter(adapter);

        builder.setView(view)
                .setNegativeButton("Cancel", (dialogInterface, i) -> {
                    if (scanning) {
                        scanning = false;
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                            bluetoothLeScanner.stopScan(leScanCallback);
                        }
                    }
                });
        return builder.create();
    }

    @Override
    public void onResume() {
        //Setup Bluetooth
        super.onResume();
    }

    private void scanDevices() {
        //Scan Setup
        btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        bluetoothLeScanner = btAdapter.getBluetoothLeScanner();

        //Set Scan Settings
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                //.setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                .build();

        //Set Scan Filters
        UUID uuid = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");    //UART UUID
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(uuid)).build();
        filters.add(filter);
        scanning = true;

        text.setText(R.string.scan_start);
        // Stops scanning after a predefined scan period.
        handler.postDelayed(() -> {
            scanning = false;
            try {
                text.setText(R.string.scan_stop);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    bluetoothLeScanner.stopScan(leScanCallback);
                }

            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }, SCAN_PERIOD);

        //Actually Start Scanning
        scanning = true;
        bluetoothLeScanner.startScan(filters, settings, leScanCallback);
    }

    // Device scan callback.
    private final ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    if(!foundDevices.contains(result.getDevice())){
                        foundDevices.add(result.getDevice());
                        adapter.notifyItemInserted(foundDevices.size() - 1);
                    }
                }
            };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mCallback = (ReturnDevice) context;
        } catch (ClassCastException e) {
            Log.d("MyDialog", "Activity doesn't implement the ReturnDevice interface");
        }
    }

    @Override
    public void onNoteClick(int position) {
        if (scanning) {
            scanning = false;
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                bluetoothLeScanner.stopScan(leScanCallback);
            }
        }
        //Connect To Device
        Toast.makeText(context, "Paired with " + foundDevices.get(position).getName(), Toast.LENGTH_SHORT).show();
        mCallback.onReturnedDevice(context, foundDevices.get(position));
        dismiss();
    }

    public class ScanCountdownTimer extends CountDownTimer {

        public ScanCountdownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            //Log.d("Time in millis", String.valueOf(millisUntilFinished));
            int progress = (int) (100-(millisUntilFinished/100));
            progressBar.setProgress(progress);
        }

        @Override
        public void onFinish() {
            //Log.d("Time in millis", "Timer finished");
            progressBar.setProgress(100);
        }
    }
}
