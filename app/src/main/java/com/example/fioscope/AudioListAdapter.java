package com.example.fioscope;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AudioListAdapter extends RecyclerView.Adapter<AudioListAdapter.AudioViewHolder> {

    private File[] allFiles;
    private TimeAgo timeAgo;

    private onItemListClick onItemListClick;

    public AudioListAdapter(File[] allFiles, onItemListClick onItemListClick) {
        this.allFiles = allFiles;
        this.onItemListClick = onItemListClick;
    }

    @NonNull
    @Override
    public AudioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_list_item, parent, false);
        timeAgo = new TimeAgo();
        return new AudioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AudioViewHolder holder, int position) {
        holder.list_title.setText(allFiles[position].getName());
        holder.list_date.setText(timeAgo.getTimeAgo(allFiles[position].lastModified()));
    }

    @Override
    public int getItemCount() {
        return allFiles.length;
    }

    public class AudioViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener/*, PopupMenu.OnMenuItemClickListener*/ {

        private static final String TAG = "AudioViewHolder";
        private ImageView list_image;
        private TextView list_title;
        private TextView list_date;
        /*private ImageButton imageButton;*/


        public AudioViewHolder(@NonNull View itemView) {

            super(itemView);

            list_image = itemView.findViewById(R.id.list_image_view);
            list_title = itemView.findViewById(R.id.list_title);
            list_date = itemView.findViewById(R.id.list_date);
            /*imageButton = itemView.findViewById(R.id.button_share_delete);*/
            /*imageButton.setOnClickListener(this);*/
            itemView.setOnClickListener(this);



        }

        @Override
        public void onClick(View v) {
            onItemListClick.onClickListener(allFiles[getAdapterPosition()], getAdapterPosition());
            Log.d(TAG, "onClick: " + getAdapterPosition());
            /*showPopUpMenu(v);*/

        }


        /*private void showPopUpMenu(View view) {
            PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
            popupMenu.inflate(R.menu.popup_menu);
            popupMenu.setOnMenuItemClickListener(this);
            popupMenu.show();

        }*/

        /*@Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_popup_share:
                    Log.d(TAG, "onMenuItemClick: action_popup_share @position: " + getAdapterPosition());
                    return true;

                case R.id.action_popup_delete:
                   //Here is my issue. What can go here to delete file from recyclerview list
                    // and from file directory of device
                    Log.d(TAG, "onMenuItemClick: action_popup_delete @position: " + getAdapterPosition());
                    return true;
                default:
                    return false;

            }
        }*/
    }

    public interface onItemListClick {
        void onClickListener(File file,int position);
    }


}
