package com.example.atcsensorydevice.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.example.atcsensorydevice.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.util.Scanner;

// This class is a dialog that lets the user know why certain permissions need to be granted
// in order for the app to be used as intended.
public class RationaleDialog extends AlertDialog {
    private final int NUM_PERMISSIONS = 4;
    private TextView[] rationaleTextViews = new TextView[NUM_PERMISSIONS];
    private final String[] FILENAMES = new String[]{
            "locationRationale.txt",
            "btAdminRationaleText.txt",
            "btConnectRationaleText.txt",
            "btScanRationaleText.txt"
    };
    public RationaleDialog(@NonNull Context context) {
        super(context);

        //Load rationale_dialog.xml
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.rationale_dialog, null);

        //Load Rationale Messages
        rationaleTextViews[0] = view.findViewById(R.id.locRatText);
        rationaleTextViews[1] = view.findViewById(R.id.adminRatText);
        rationaleTextViews[2] = view.findViewById(R.id.scanRatText);
        rationaleTextViews[3] = view.findViewById(R.id.connRatText);
        BufferedReader reader = null;
        AssetManager assetManager = context.getAssets();
        for(int i = 0; i < rationaleTextViews.length; i++){
            try{
                reader = new BufferedReader(new InputStreamReader(assetManager.open("rationaleTexts/" + FILENAMES[i])));
                String text = reader.readLine();
                rationaleTextViews[i].setText(text);
                reader.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
            }
        });
        setView(view);
    }

    @Override
    public void show() {
        super.show();
    }
}
