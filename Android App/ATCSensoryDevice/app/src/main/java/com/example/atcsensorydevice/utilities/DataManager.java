package com.example.atcsensorydevice.utilities;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.atcsensorydevice.objects.Profile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DataManager {

    //Internal Storage
    public static final String profilesFile = "users.dat";
    private static final String timeLimitFile = "timeLimit.dat";

    //Profile List
    public static ArrayList<Profile> profilesList;

    //Time Limits
    private static final int defaultTimeLimit = 120;
    private static int timeLimit;

    public static ArrayList<Profile> getProfilesList(){
        return profilesList;
    }

    public static void writeTimeLimitToFile(Context context, String timeLimitString){
        timeLimit = Integer.parseInt(timeLimitString);
        //Write time limit to file
        try{
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(context.getFilesDir(), timeLimitFile)));
            //bufferedWriter.write(timeLimitView.getText().toString().trim());
            bufferedWriter.write(timeLimitString);
            bufferedWriter.close();
        }catch (Exception e){
            Log.d("[FILEWRITER]", "Didn't work.");
            e.printStackTrace();
        }
        Toast.makeText(context, "Settings Saved!", Toast.LENGTH_SHORT).show();
    }

    public static int setTimeLimitFromFile(Context context){
        BufferedReader reader;
        String line;

        //Read from File if it exists
        //Create new file if it does not exist
        File timeLimitFileIO = new File(context.getFilesDir(), timeLimitFile);
        if(!timeLimitFileIO.exists()){
            try{
                timeLimitFileIO.createNewFile();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        try{
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(timeLimitFileIO)));
            line = reader.readLine();
            Log.d("[FILEWRITER]", line);
            reader.close();
            if(line == null){
                timeLimit = defaultTimeLimit;
                return defaultTimeLimit;
            }else{
                timeLimit = Integer.parseInt(line);
                return timeLimit;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return defaultTimeLimit;
    }

    public static int getTimeLimit(){
        return timeLimit;
    }
}
