package com.example.atcsensorydevice;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.atcsensorydevice.utilities.DataManager;
import com.example.atcsensorydevice.modules.ProfileListAdapter;
import com.example.atcsensorydevice.modules.ScanResultsAdapter;
import com.example.atcsensorydevice.objects.Profile;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class ProfilesPage extends AppCompatActivity implements ScanResultsAdapter.OnNoteListener {
    //Data Access
    private final String FILENAME = DataManager.profilesFile;

    //UI Elements
    private LinearLayout manageLayout;
    private FloatingActionButton addButton;
    private Button manageButton, deleteButton;
    private RecyclerView profilesRecyclerView;
    private ProfileListAdapter adapter;

    //State Checking
    private boolean managing = false;

    //Profile Management
    private ArrayList<Profile> selectedProfiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profiles_page);

        //Identify UI Elements
        manageLayout = findViewById(R.id.manageBar);
        addButton = findViewById(R.id.buttonAdd);
        manageButton = findViewById(R.id.manageButton);
        deleteButton = findViewById(R.id.deleteProfileButton);
        deleteButton.setWidth(manageButton.getWidth());

        //Load and Display Saved Profiles
        loadProfiles();
        if(DataManager.profilesList != null){
            displayProfiles();
        }else{
            DataManager.profilesList = new ArrayList<>();
        }

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoAddProfilePage();
            }
        });

        manageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!managing){
                    deleteButton.setVisibility(View.VISIBLE);
                    manageProfiles();
                }else{
                    endManage();
                }
            }
        });
    }

    private void gotoAddProfilePage(){
        Intent intent = new Intent(this, AddProfilePage.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void loadProfiles(){
        ObjectInputStream file = null;
        try {
            file = new ObjectInputStream(new FileInputStream(new File(getFilesDir(), FILENAME)));
            DataManager.profilesList = (ArrayList<Profile>) file.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if(file != null){
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void displayProfiles() {
        profilesRecyclerView = findViewById(R.id.profilesRecyclerView);
        adapter = new ProfileListAdapter(this, this::onNoteClick, false);
        profilesRecyclerView.setAdapter(adapter);
        profilesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void manageProfiles(){
        /*
        profilesList.clear();
        FileOutputStream file = null;
        try{
            String clear = "";
            file = openFileOutput(FILENAME, MODE_PRIVATE);
            file.write(clear.getBytes());
            System.out.println("Profile successfully cleared at " + getFilesDir() + "/" + FILENAME);
        }catch (IOException e){
            e.printStackTrace();
        }finally{
            if(file != null){
                try {
                    file.close();
                    Toast.makeText(getApplicationContext(), "Cleared all profiles", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }*/

        //Setting state and text changes
        managing = true;
        manageButton.setText("Done");

        //Checkboxes
        adapter = new ProfileListAdapter(this, this::onNoteClick, true);
        adapter.setCheckedCallback(new ProfileListAdapter.CheckedCallback() {
            @Override
            public void onCheckedChanged(int position, boolean isChecked) {
                Log.d("[CHECK]", "yo");
                //Toast.makeText(getApplicationContext(), "Checked @ " + position + ", " + isChecked, Toast.LENGTH_SHORT).show();
                selectedProfiles.add(DataManager.profilesList.get(position));
            }
        });
        profilesRecyclerView.setAdapter(adapter);
        profilesRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        //Delete Button
        deleteButton.setOnClickListener(deleteButtonListener());
    }

    private void endManage(){
        managing = false;
        deleteButton.setVisibility(View.GONE);
        manageButton.setText("Manage Profiles");
        ProfileListAdapter adapter = new ProfileListAdapter(this, this::onNoteClick, false);
        profilesRecyclerView.setAdapter(adapter);
        profilesRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    private View.OnClickListener deleteButtonListener() {
        return new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                //Delete items off RecyclerView and ProfileList
                if(selectedProfiles.size() > 0){
                    for(int i = 0; i < selectedProfiles.size(); i++){
                        for(int r = 0; r < DataManager.profilesList.size(); r++){
                            DataManager.profilesList.remove(selectedProfiles.get(i));
                            adapter.notifyItemRemoved(r);
                            adapter.notifyItemRangeChanged(r, DataManager.profilesList.size());
                            //endManage();
                        }
                    }
                    ObjectOutputStream output = null;
                    try {
                        output = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), FILENAME)));
                        output.writeObject(DataManager.profilesList);
                        output.close();
                        Toast.makeText(getApplicationContext(), "Delete Successful", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "Delete Unsuccessful", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(getApplicationContext(), "Nothing is selected", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onNoteClick(int position) {
        if(!managing){
            Intent intent = new Intent(getApplicationContext(), ControlPanel.class);
            intent.putExtra("profileName", DataManager.profilesList.get(position).getName());
            intent.putExtra("profileVal", DataManager.profilesList.get(position).getPressure());
            intent.putExtra("profileDeviceName", DataManager.profilesList.get(position).getDeviceTitle());
            intent.putExtra("profileDeviceAddress", DataManager.profilesList.get(position).getAddress());
            intent.putExtra("position", position);
            startActivity(intent);
        }

    }
}