package com.example.mobile_midterm.fragments;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_midterm.R;
import com.example.mobile_midterm.adapters.FirebaseAdapter;
import com.example.mobile_midterm.models.FirebaseDataClass;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Comparator;

public class ViewFragment extends Fragment {
    public static final String ARG_OBJECT = "object";
    private final ArrayList<FirebaseDataClass> imageList = new ArrayList<>();
    private FirebaseAdapter adapter;
    View view;
    Button refreshBtn;
    RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_view, container, false);
        refreshBtn = view.findViewById(R.id.refresh_btn);
        recyclerView = view.findViewById(R.id.recyclerView);



        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.fragment_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        GetImageFromFirebase();

        refreshBtn.setOnClickListener(v -> {
            imageList.clear();
            GetImageFromFirebase();
        });

        return view;
    }

    private void GetImageFromFirebase() {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        adapter = new FirebaseAdapter(view.getContext(), imageList);
        recyclerView.setAdapter(adapter);

        FirebaseStorage.getInstance().getReference().child("Images").listAll().addOnCompleteListener(new OnCompleteListener<ListResult>() {
            @Override
            public void onComplete(@NonNull Task<ListResult> task) {
                for (StorageReference item : task.getResult().getItems()) {
                    String imageName = item.getName();
                    item.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void onSuccess(Uri uri) {
                            imageList.add(new FirebaseDataClass(uri.toString(), imageName));
                            // Sort imageList by imageName
                            imageList.sort(new Comparator<FirebaseDataClass>() {
                                @Override
                                public int compare(FirebaseDataClass o1, FirebaseDataClass o2) {
                                    return o1.getImageName().compareTo(o2.getImageName());
                                }
                            });
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
    }

    public void Refresh() {
        adapter.notifyDataSetChanged();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


}
