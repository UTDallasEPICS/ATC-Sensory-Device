package com.example.atcsensorydevice;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.atcsensorydevice.utilities.DataManager;

public class MainActivity extends AppCompatActivity {
    Button profilePageButton, btSettingsButton, freeRun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initSettings();

        profilePageButton = findViewById(R.id.profilesButton);
        profilePageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoProfilesPage();
            }
        });

        freeRun = findViewById(R.id.freeRun);
        freeRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoFreeRunPage();
            }
        });

        btSettingsButton = findViewById(R.id.btButton);
        btSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoBTSettings();
            }
        });
    }

    private void gotoProfilesPage() {
        Intent intent = new Intent(this, ProfilesPage.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void gotoFreeRunPage() {
        Intent intent = new Intent(this, FreeRun.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void gotoBTSettings() {
        Intent intent = new Intent(this, SettingsPage.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    // Initialization code to set constants that will be used throughout the app
    // Namely, the global time limit
    private void initSettings(){
        DataManager.setTimeLimitFromFile(this);
    }
}