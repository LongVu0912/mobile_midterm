package com.example.mobile_midterm.adapters;

import static android.content.Context.CLIPBOARD_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.mobile_midterm.R;
import com.example.mobile_midterm.models.FirebaseDataClass;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class FirebaseAdapter extends RecyclerView.Adapter<FirebaseAdapter.MyViewHolder> {
    private final Context context;
    private final ArrayList<FirebaseDataClass> imageList;
    private long startTime;
    private int imagesLoaded = 0;

    public FirebaseAdapter(Context context, ArrayList<FirebaseDataClass> imageList) {
        this.context = context;
        this.imageList = imageList;
        this.startTime = System.currentTimeMillis();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Glide.with(context).load(imageList.get(position).getImageUrl())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        imagesLoaded++;
                        if (imagesLoaded == imageList.size()) {
                            long endTime = System.currentTimeMillis();
                            long duration = (endTime - startTime) / 1000; // convert to seconds
                            Toast.makeText(context, "All images loaded in " + duration + " seconds.", Toast.LENGTH_LONG).show();
                        }
                        return false;
                    }
                })
                .into(holder.imageView);
        holder.textView.setText(imageList.get(position).getImageName());

        holder.deleteBtn.setOnClickListener(v -> {
            FirebaseDataClass firebaseDataClass = imageList.get(position);
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("Images");

            StorageReference desertRef = storageRef.child(firebaseDataClass.getImageName());

            desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(context.getApplicationContext(), "Delete image from Firebase successfully", Toast.LENGTH_SHORT).show();
                    imageList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(0, imageList.size());
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(context.getApplicationContext(), "Uh-oh, an error occurred!", Toast.LENGTH_SHORT).show();
                }
            });
        });

        holder.copyBtn.setOnClickListener(v -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("imageUrl", imageList.get(position).getImageUrl());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context.getApplicationContext(), "Copy url successfully", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context.getApplicationContext(), "Uh-oh, an error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    // Modified to preload all images when the adapter is created
    @Override
    public void onViewRecycled(@NonNull MyViewHolder holder) {
        super.onViewRecycled(holder);
        Glide.with(context).clear(holder.imageView); // Clear the image to prevent wrong images shown while scrolling
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;
        Button deleteBtn, copyBtn;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.recyclerImage);
            textView = itemView.findViewById(R.id.recyclerCaption);
            deleteBtn = itemView.findViewById(R.id.delete_btn);
            copyBtn = itemView.findViewById(R.id.copy_btn);
        }
    }
}

