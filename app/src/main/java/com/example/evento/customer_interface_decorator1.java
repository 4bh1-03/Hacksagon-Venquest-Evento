package com.example.evento;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;

import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import java.io.IOException;
import java.util.*;

public class customer_interface_decorator1 extends AppCompatActivity implements OnMapReadyCallback, PaymentResultListener {

    private TextView vendorNameTV, serviceNameTV, locationTV;
    private RecyclerView recyclerViewImages, recyclerViewItems;
    private MapView mapView;
    private GoogleMap googleMap;
    private EditText nameET, phoneET;
    private Button payButton;
    private RadioButton lastSelectedRadioButton = null;

    private String vendorName, serviceName = "";
    private String selectedPrice = "";

    private DatabaseReference databaseRef;
    private StorageReference storageRef;

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_interface_decorator1);

        vendorNameTV = findViewById(R.id.textView);
        serviceNameTV = findViewById(R.id.textView2);
        locationTV = findViewById(R.id.textView3);
        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        recyclerViewItems = findViewById(R.id.recyclerViewItems);
        mapView = findViewById(R.id.mapView);
        nameET = findViewById(R.id.editTextText13);
        phoneET = findViewById(R.id.editTextPhone3);
        payButton = findViewById(R.id.button15);

        vendorName = getIntent().getStringExtra("vendor_name");

        databaseRef = FirebaseDatabase.getInstance().getReference();
        storageRef = FirebaseStorage.getInstance().getReference();

        vendorNameTV.setText(vendorName);

        Checkout.preload(getApplicationContext()); // Razorpay init

        Bundle mapViewBundle = savedInstanceState != null ? savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY) : null;
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        fetchVendorDetails();
        loadDecorationItems();
        loadVendorImagesFromStorage();

        payButton.setOnClickListener(v -> {
            String name = nameET.getText().toString().trim();
            String phone = phoneET.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || selectedPrice.isEmpty()) {
                Toast.makeText(this, "Enter name, phone, and select an item", Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse price safely
            String priceStr = selectedPrice.replace("₹", "").replace(" ", "").trim();
            int amount = 0;
            try {
                amount = Integer.parseInt(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price selected", Toast.LENGTH_SHORT).show();
                return;
            }

            if (amount <= 0) {
                Toast.makeText(this, "Amount should be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            startPayment(name, phone, amount);
        });
    }

    private void startPayment(String customerName, String customerPhone, int amount) {
        Checkout checkout = new Checkout();
        checkout.setKeyID("rzp_test_g9coO6mLvqk7WR"); // Replace with your actual key

        try {
            JSONObject options = new JSONObject();
            options.put("name", vendorName);
            options.put("description", serviceName);
            options.put("currency", "INR");
            options.put("amount", amount * 100); // amount in paise

            JSONObject preFill = new JSONObject();
            preFill.put("email", "test@example.com");
            preFill.put("contact", customerPhone);

            options.put("prefill", preFill);
            checkout.open(this, options);
        } catch (Exception e) {
            Toast.makeText(this, "Payment error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("Razorpay", "Error in starting payment", e);
        }
    }

    @Override
    public void onPaymentSuccess(String paymentID) {
        String name = nameET.getText().toString().trim();
        String phone = phoneET.getText().toString().trim();
        int amount = Integer.parseInt(selectedPrice.replace("₹", "").replace(" ", "").trim());

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Bookings")
                .child(vendorName)
                .child(serviceName)
                .child(name);

        Map<String, Object> booking = new HashMap<>();
        booking.put("customer_name", name);
        booking.put("customer_phone", phone);
        booking.put("amount", amount);
        booking.put("paymentID", paymentID);

        ref.setValue(booking).addOnSuccessListener(unused ->
                Toast.makeText(this, "Booking successful!", Toast.LENGTH_LONG).show()
        ).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to save booking", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onPaymentError(int code, String response) {
        Toast.makeText(this, "Payment Failed: " + response, Toast.LENGTH_SHORT).show();
        Log.e("Razorpay", "Payment error code: " + code + ", response: " + response);
    }

    private void fetchVendorDetails() {
        databaseRef.child("Vendors").child(vendorName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                serviceName = snapshot.child("service").getValue(String.class);
                String location = snapshot.child("location").getValue(String.class);

                serviceNameTV.setText(serviceName);
                locationTV.setText(location);

                Geocoder geocoder = new Geocoder(customer_interface_decorator1.this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocationName(location, 1);
                    if (!addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        if (googleMap != null) {
                            googleMap.addMarker(new MarkerOptions().position(latLng).title(vendorName));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        }
                    }
                } catch (IOException e) {
                    Toast.makeText(customer_interface_decorator1.this, "Location error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(customer_interface_decorator1.this, "Failed to load vendor details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadVendorImagesFromStorage() {
        StorageReference vendorFolderRef = storageRef.child("vendor_images").child(vendorName);

        vendorFolderRef.listAll().addOnSuccessListener(listResult -> {
            List<String> imageUrls = new ArrayList<>();
            for (StorageReference item : listResult.getItems()) {
                item.getDownloadUrl().addOnSuccessListener(uri -> {
                    imageUrls.add(uri.toString());
                    if (imageUrls.size() == listResult.getItems().size()) {
                        recyclerViewImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                        recyclerViewImages.setAdapter(new ImageAdapter(imageUrls));
                    }
                });
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load vendor images", Toast.LENGTH_SHORT).show());
    }

    private void loadDecorationItems() {
        databaseRef.child("decorations").child(vendorName).child("items")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<DecorationItem> items = new ArrayList<>();
                        for (DataSnapshot itemSnap : snapshot.getChildren()) {
                            String imageUrl = itemSnap.child("imageUrl").getValue(String.class);
                            String price = itemSnap.child("price").getValue(String.class);
                            items.add(new DecorationItem(imageUrl, price));
                        }

                        recyclerViewItems.setLayoutManager(new LinearLayoutManager(customer_interface_decorator1.this));
                        recyclerViewItems.setAdapter(new DecorationAdapter(items));
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(customer_interface_decorator1.this, "Failed to load decorations", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override public void onMapReady(GoogleMap map) {
        this.googleMap = map;
    }

    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }

    static class DecorationItem {
        String imageUrl, price;
        DecorationItem(String imageUrl, String price) {
            this.imageUrl = imageUrl;
            this.price = price;
        }
    }

    class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
        List<String> imageUrls;
        ImageAdapter(List<String> urls) { this.imageUrls = urls; }

        class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            ImageViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView2);
            }
        }

        @NonNull
        @Override public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_recycler4, parent, false);
            return new ImageViewHolder(view);
        }

        @Override public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            Glide.with(holder.itemView.getContext()).load(imageUrls.get(position)).into(holder.imageView);
        }

        @Override public int getItemCount() { return imageUrls.size(); }
    }

    class DecorationAdapter extends RecyclerView.Adapter<DecorationAdapter.ViewHolder> {
        List<DecorationItem> itemList;
        DecorationAdapter(List<DecorationItem> list) { this.itemList = list; }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView priceText;
            RadioButton radioButton;

            ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView10);
                priceText = itemView.findViewById(R.id.textView19);
                radioButton = itemView.findViewById(R.id.radioButton);

                radioButton.setOnClickListener(v -> {
                    if (lastSelectedRadioButton != null && lastSelectedRadioButton != radioButton) {
                        lastSelectedRadioButton.setChecked(false);
                    }
                    lastSelectedRadioButton = radioButton;
                    selectedPrice = itemList.get(getAdapterPosition()).price;
                });
            }
        }

        @NonNull
        @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_recycler6, parent, false);
            return new ViewHolder(view);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DecorationItem item = itemList.get(position);
            Glide.with(holder.itemView.getContext()).load(item.imageUrl).into(holder.imageView);
            holder.priceText.setText("₹ " + item.price);
        }

        @Override public int getItemCount() { return itemList.size(); }
    }
}
