package com.example.evento;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;
import java.util.UUID;

public class Image_upload extends AppCompatActivity {
    private ImageView imageView;
    private Button selectImageBtn, uploadImageBtn, uploadBtn;
    private Uri imageUri;
    private StorageReference storageReference;
    private ProgressDialog progressDialog;

    private TextView instructions;
    boolean val = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_upload);

        FirebaseApp.initializeApp(this);

        imageView = findViewById(R.id.upload_image);
        selectImageBtn = findViewById(R.id.selectImageBtn);
        uploadImageBtn = findViewById(R.id.uploadImageBtn);
        uploadBtn = findViewById(R.id.uploadButton);
        instructions = findViewById(R.id.instructions);


        String serviceProvided = getIntent().getStringExtra("service_provided");

        if(Objects.equals(serviceProvided, "Caterers")){
            instructions.setText("Upload each dish with its name and price. This will help clients understand your offerings and make informed decision");
        }else if(Objects.equals(serviceProvided, "Decorators")){
            instructions.setText("Kindly upload images of your recent or featured projects. These visuals will give clients a clear sense of your style and capabilities.");
        }

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Objects.equals(serviceProvided, "Caterers") && val){
                    Intent intent = new Intent(Image_upload.this, Caterer_detail_upload.class);
                    startActivity(intent);
                } else if (Objects.equals(serviceProvided, "Decorators") && val) {
                    Intent intent = new Intent(Image_upload.this, Decorator_details_upload.class);
                    startActivity(intent);
                } else if (Objects.equals(serviceProvided, "Venues") && val) {
                    Toast.makeText(Image_upload.this, "Uploaded successfully", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(Image_upload.this, "Please upload image first", Toast.LENGTH_SHORT).show();
                }
            }
        });


        storageReference = FirebaseStorage.getInstance().getReference();

        selectImageBtn.setOnClickListener(v -> {
            ImagePicker.with(Image_upload.this)
                    .galleryOnly()
                    .createIntent(intents -> {
                        imagePickerLauncher.launch(intents);
                        return null;
                    });
        });

        uploadImageBtn.setOnClickListener(v -> {
            if (imageUri != null) {
                // Show progress dialog
                progressDialog = new ProgressDialog(Image_upload.this);
                progressDialog.setMessage("Uploading image...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                uploadImageToFirebase();
            } else {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    imageView.setImageURI(imageUri);
                }
            });

    private void uploadImageToFirebase() {
        String vendorId = getIntent().getStringExtra("vendor_id");
        if (vendorId == null || vendorId.isEmpty()) {
            Toast.makeText(this, "Vendor ID missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = UUID.randomUUID().toString();
        StorageReference fileRef = storageReference.child("vendor_images/" + vendorId + "/" + fileName);

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            DatabaseReference vendorRef = FirebaseDatabase.getInstance()
                                    .getReference("Vendors")
                                    .child(vendorId)
                                    .child("imageUrl");

                            vendorRef.setValue(uri.toString())
                                    .addOnSuccessListener(aVoid -> {
                                        // Dismiss progress dialog
                                        if (progressDialog != null && progressDialog.isShowing()) {
                                            progressDialog.dismiss();
                                        }

                                        // Show success dialog
                                        new AlertDialog.Builder(Image_upload.this)
                                                .setTitle("Upload Successful")
                                                .setMessage("Your image has been uploaded successfully!")
                                                .setPositiveButton("OK", null)
                                                .show();

                                        val = true;
                                    });
                        }))
                .addOnFailureListener(e -> {
                    // Dismiss progress dialog on failure
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(Image_upload.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        // Dismiss dialog to prevent memory leaks
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDestroy();
    }

}
