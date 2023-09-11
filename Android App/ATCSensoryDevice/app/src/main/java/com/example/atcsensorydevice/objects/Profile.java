package com.example.atcsensorydevice.objects;

import android.text.Editable;

import java.io.Serializable;

public class Profile implements Serializable {
    private String name;
    private float pressure;
    private String deviceTitle;
    private String address;

    public Profile(String name, float pressure, String deviceTitle, String address){
        this.name = name;
        this.pressure = pressure;
        this.deviceTitle = deviceTitle;
        this.address = address;
    }

    public String getName(){
        return name;
    }

    public float getPressure(){
        return pressure;
    }

    public String getDeviceTitle(){ return deviceTitle; }

    public String getAddress() {
        return address;
    }
}
