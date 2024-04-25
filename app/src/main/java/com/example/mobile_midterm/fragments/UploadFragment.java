package com.example.mobile_midterm.fragments;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.example.mobile_midterm.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Objects;

public class UploadFragment extends Fragment {

    private ImageView uploadImage;
    EditText uploadName;
    ProgressBar progressBar;
    private Uri imageUri;
    final private StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Images");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload, container, false);

        FloatingActionButton uploadButton = view.findViewById(R.id.uploadButton);
        FloatingActionButton uploadMultipleButton = view.findViewById(R.id.uploadMultipleButton);
        uploadName = view.findViewById(R.id.uploadName);
        uploadImage = view.findViewById(R.id.uploadImage);
        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                assert data != null;
                if (data.getClipData() != null) { // Handling multiple images
                    int count = data.getClipData().getItemCount();
                    if (count > 1) {
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            uploadToFirebase(imageUri); // Call the upload function for each image
                        }
                    } else {
                        imageUri = data.getClipData().getItemAt(0).getUri();
                        uploadImage.setImageURI(imageUri);
                    }
                } else if (data.getData() != null) { // Handling single image selection
                    imageUri = data.getData();
                    uploadImage.setImageURI(imageUri);
                }
            } else {
                Toast.makeText(getContext(), "No Image Selected", Toast.LENGTH_SHORT).show();
            }
        });

        uploadImage.setOnClickListener(view1 -> {
            Intent photoPicker = new Intent();
            photoPicker.setAction(Intent.ACTION_GET_CONTENT);
            photoPicker.setType("image/*");
            activityResultLauncher.launch(photoPicker);
        });

        uploadButton.setOnClickListener(view2 -> {
            if (imageUri != null) {
                uploadToFirebase(imageUri);
            } else {
                Toast.makeText(getContext(), "Please select image", Toast.LENGTH_SHORT).show();
            }
        });

        uploadMultipleButton.setOnClickListener(view1 -> {
            Intent photoPicker = new Intent();
            photoPicker.setAction(Intent.ACTION_GET_CONTENT);
            photoPicker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            photoPicker.setType("image/*");
            activityResultLauncher.launch(photoPicker);
        });

        return view;
    }

    private void uploadToFirebase(Uri uri) {
        String upload_name = uploadName.getText().toString();
        if (upload_name.isEmpty()) {
            upload_name = getFileName(uri);
        }
        else {
            upload_name = upload_name + "." + getFileExtension(uri);
        }
        final StorageReference imageReference = storageReference.child(upload_name);
        imageReference.putFile(uri).addOnSuccessListener(taskSnapshot -> imageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri1) {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(getContext(), "Uploaded to Firebase successfully", Toast.LENGTH_SHORT).show();

                TabLayout tabs = requireActivity().findViewById(R.id.tab_layout);
                Objects.requireNonNull(tabs.getTabAt(1)).select();
            }
        })).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(getContext(), "Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getFileExtension(Uri fileUri) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(fileUri));
    }
}
