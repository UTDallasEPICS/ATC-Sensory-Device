package com.example.atcsensorydevice.modules;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.atcsensorydevice.R;
import com.example.atcsensorydevice.objects.Profile;
import com.example.atcsensorydevice.utilities.DataManager;

import java.util.ArrayList;

public class ProfileListAdapter extends RecyclerView.Adapter<ProfileListAdapter.MyViewHolder> {

    Context context;
    private OnNoteListener globalNoteListener;
    private CheckedCallback checkedCallback;
    private boolean managePressed;

    public ProfileListAdapter(Context context, OnNoteListener onNoteListener, boolean managePressed){
        this.context = context;
        this.globalNoteListener = onNoteListener;
        this.managePressed = managePressed;
    }

    @NonNull
    @Override
    public ProfileListAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Inflate Layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.profile_results_template, parent, false);
        return new ProfileListAdapter.MyViewHolder(view, globalNoteListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileListAdapter.MyViewHolder holder, int position) {
        //Assign values to rows as they appear on the screen
        //This is based on the position of the recycler view

        holder.imageView.setImageResource(R.drawable.ic_baseline_account_box_24);
        holder.profileNameText.setText(DataManager.profilesList.get(position).getName());
        holder.profilePressureText.setText(String.valueOf(DataManager.profilesList.get(position).getPressure()));
        holder.profileDeviceTitle.setText(DataManager.profilesList.get(position).getDeviceTitle());
        holder.profileCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(checkedCallback != null){
                    checkedCallback.onCheckedChanged(holder.getAdapterPosition(), b);
                }
            }
        });
        if(managePressed){
            holder.profileCheckbox.setVisibility(View.VISIBLE);
        }else{
            holder.profileCheckbox.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return DataManager.profilesList.size();
    }

    public void setCheckedCallback(CheckedCallback callback){
        this.checkedCallback = callback;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView imageView;
        TextView profileNameText, profilePressureText, profileDeviceTitle;
        CheckBox profileCheckbox;
        OnNoteListener onNoteListener;

        public MyViewHolder(@NonNull View itemView, OnNoteListener onNoteListener) {
            super(itemView);
            imageView = itemView.findViewById(R.id.profileResultImage);
            profileNameText = itemView.findViewById(R.id.profileResultName);
            profilePressureText = itemView.findViewById(R.id.profileResultPressure);
            profileDeviceTitle = itemView.findViewById(R.id.profileResultDeviceTitle);
            profileCheckbox = itemView.findViewById(R.id.profileCheckbox);
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

    public interface CheckedCallback {
        void onCheckedChanged(int position, boolean isChecked);
    }
}
