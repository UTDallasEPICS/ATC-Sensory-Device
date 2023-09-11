package com.example.atcsensorydevice.modules;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.atcsensorydevice.R;

import java.util.ArrayList;

public class ScanResultsAdapter extends RecyclerView.Adapter<ScanResultsAdapter.ResultViewHolder> {
    private Context context;
    private ArrayList<BluetoothDevice> foundDevices;
    private OnNoteListener globalNoteListener;


    public ScanResultsAdapter(Context context, ArrayList<BluetoothDevice> foundDevices, OnNoteListener onNoteListener) {
        this.context = context;
        this.foundDevices = foundDevices;
        this.globalNoteListener = onNoteListener;
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Inflate Layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.scan_results_template, parent, false);
        return new ResultViewHolder(view, globalNoteListener);
    }

    @Override
    @SuppressLint("MissingPermission")
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        //Assign values to views
        holder.title.setText(foundDevices.get(position).getName());
        holder.address.setText(foundDevices.get(position).getAddress());
        holder.image.setImageResource(R.drawable.ic_baseline_devices_other_24);
    }

    @Override
    public int getItemCount() {
        //Returns number of items
        return foundDevices.size();
    }

    public static class ResultViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ImageView image;
        TextView title, address;
        OnNoteListener onNoteListener;

        public ResultViewHolder(@NonNull View itemView, OnNoteListener onNoteListener) {
            super(itemView);
            image = itemView.findViewById(R.id.deviceResultImage);
            title = itemView.findViewById(R.id.deviceResultTitle);
            address = itemView.findViewById(R.id.deviceResultAddress);
            this.onNoteListener = onNoteListener;

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onNoteListener.onNoteClick(getAdapterPosition());
        }
    }
    
    public interface OnNoteListener{
        void onNoteClick(int position);
    }
}
