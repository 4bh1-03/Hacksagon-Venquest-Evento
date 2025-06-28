package com.example.evento;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Decorator_details_upload extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 100;
    private List<DecorationItem> itemList = new ArrayList<>();
    private RecyclerViewAdapter adapter;
    private int activePosition = -1;
    private FirebaseStorage storage;
    private ProgressDialog progressDialog;
    private EditText companyNameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decorator_details_upload);

        // Initialize Firebase Storage
        storage = FirebaseStorage.getInstance();

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading Data");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        // Initialize views
        companyNameEditText = findViewById(R.id.decoratorName);
        ImageView addButton = findViewById(R.id.addImage);
        Button submitButton = findViewById(R.id.submitBtn);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create top item and add to list
        DecorationItem topItem = new DecorationItem();
        itemList.add(topItem);

        adapter = new RecyclerViewAdapter(itemList);
        recyclerView.setAdapter(adapter);

        // Add new item
        addButton.setOnClickListener(v -> {
            DecorationItem newItem = new DecorationItem();
            itemList.add(newItem);
            adapter.notifyItemInserted(itemList.size() - 1);
        });

        // Submit data to Firebase
        submitButton.setOnClickListener(v -> {
            if (validateData()) {
                uploadToFirebase();
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openGallery(int position) {
        activePosition = position;
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (activePosition >= 0 && activePosition < itemList.size()) {
                // Update item at active position
                itemList.get(activePosition).setImageUri(imageUri);
                adapter.notifyItemChanged(activePosition);
            }
        }
    }

    private boolean validateData() {
        // Validate company name
        String vendorName = companyNameEditText.getText().toString().trim();
        if (vendorName.isEmpty()) {
            Toast.makeText(this, "Please enter company name", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check all items in the list
        for (int i = 0; i < itemList.size(); i++) {
            DecorationItem item = itemList.get(i);
            if (item.getImageUri() == null) {
                Toast.makeText(this, "Please upload image for item " + (i+1), Toast.LENGTH_SHORT).show();
                return false;
            }
            if (item.getPrice().isEmpty()) {
                Toast.makeText(this, "Please enter price for item " + (i+1), Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void uploadToFirebase() {
        progressDialog.show();

        // Get sanitized vendor name
        String vendorName = companyNameEditText.getText().toString().trim();
        String sanitizedVendorName = vendorName.replaceAll("[.#$\\[\\]]", "_");

        // Create a list of upload tasks
        List<Task<Uri>> uploadTasks = new ArrayList<>();
        List<DecorationItem> validItems = new ArrayList<>(); // Track items with images

        for (int i = 0; i < itemList.size(); i++) {
            DecorationItem item = itemList.get(i);
            Uri imageUri = item.getImageUri();
            if (imageUri != null) {
                validItems.add(item);
                // Create vendor-specific filename
                String filename = "image_" + UUID.randomUUID().toString();
                StorageReference storageRef = storage.getReference()
                        .child("decoration_images/" + sanitizedVendorName + "/" + filename);

                // Create upload task
                UploadTask uploadTask = storageRef.putFile(imageUri);
                Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return storageRef.getDownloadUrl();
                });

                uploadTasks.add(urlTask);
            }
        }

        // Wait for all image uploads to complete
        Tasks.whenAllSuccess(uploadTasks).addOnSuccessListener(uris -> {
            // Now we have download URLs for all images
            Map<String, Object> vendorData = new HashMap<>();
            Map<String, Object> itemsMap = new HashMap<>();

            // Process all valid items
            for (int i = 0; i < validItems.size(); i++) {
                DecorationItem item = validItems.get(i);
                Uri itemUri = (Uri) uris.get(i);
                itemsMap.put("item_" + i, createItemMap(item, itemUri));
            }

            // Add items and timestamp to vendor data
            vendorData.put("items", itemsMap);
            vendorData.put("timestamp", ServerValue.TIMESTAMP);
            vendorData.put("vendorName", vendorName);

            // Save to vendor-specific path in Realtime Database
            DatabaseReference vendorRef = FirebaseDatabase.getInstance()
                    .getReference("decorations")
                    .child(sanitizedVendorName);

            vendorRef.setValue(vendorData)
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        Toast.makeText(Decorator_details_upload.this,
                                "Data uploaded successfully to " + vendorName, Toast.LENGTH_SHORT).show();
                        resetForm();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(Decorator_details_upload.this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("FirebaseUpload", "Database error", e);
                    });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(Decorator_details_upload.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("FirebaseUpload", "Image upload error", e);
        });
    }

    private Map<String, Object> createItemMap(DecorationItem item, Uri downloadUri) {
        Map<String, Object> itemMap = new HashMap<>();
        itemMap.put("imageUrl", downloadUri.toString());
        itemMap.put("price", item.getPrice());
        return itemMap;
    }

    private void resetForm() {
        // Clear all items
        itemList.clear();

        // Re-add the top item
        DecorationItem topItem = new DecorationItem();
        itemList.add(topItem);

        adapter.notifyDataSetChanged();

        // Clear company name
        companyNameEditText.setText("");
    }

    // DecorationItem class
    public static class DecorationItem {
        private Uri imageUri;
        private String price = "";

        public Uri getImageUri() {
            return imageUri;
        }

        public void setImageUri(Uri imageUri) {
            this.imageUri = imageUri;
        }

        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }
    }

    // RecyclerView Adapter
    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        private final List<DecorationItem> localItems;

        public RecyclerViewAdapter(List<DecorationItem> items) {
            localItems = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.custom_recycler, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DecorationItem item = localItems.get(position);

            // Clear previous image and set new
            holder.imageView.setImageURI(null);
            if (item.getImageUri() != null) {
                holder.imageView.setImageURI(item.getImageUri());
            } else {
                holder.imageView.setImageResource(R.drawable.ic_image_placeholder);
            }

            // Update price text without triggering watcher
            holder.priceEditText.removeTextChangedListener(holder.priceWatcher);
            holder.priceEditText.setText(item.getPrice());
            holder.priceEditText.addTextChangedListener(holder.priceWatcher);

            // Upload button
            holder.uploadButton.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    openGallery(currentPosition);
                }
            });

            // Remove button - don't allow removal of top item (position 0)
            if (position == 0) {
                holder.removeButton.setVisibility(View.GONE);
            } else {
                holder.removeButton.setVisibility(View.VISIBLE);
                holder.removeButton.setOnClickListener(v -> {
                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION && currentPosition > 0) {
                        localItems.remove(currentPosition);
                        notifyItemRemoved(currentPosition);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return localItems.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            EditText priceEditText;
            Button uploadButton;
            ImageView removeButton;
            TextWatcher priceWatcher;

            public ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.decoratorImg);
                priceEditText = itemView.findViewById(R.id.price);
                uploadButton = itemView.findViewById(R.id.uploadBtn);
                removeButton = itemView.findViewById(R.id.removeBtn);

                // Create persistent TextWatcher
                priceWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        int pos = getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            localItems.get(pos).setPrice(s.toString());
                        }
                    }
                };
                priceEditText.addTextChangedListener(priceWatcher);
            }
        }
    }
}
