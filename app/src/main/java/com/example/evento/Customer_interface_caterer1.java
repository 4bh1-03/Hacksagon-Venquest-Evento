package com.example.evento;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Customer_interface_caterer1 extends AppCompatActivity implements OnMapReadyCallback {

    private TextView serviceProvider, serviceName, locationText, plateRate;
    private RecyclerView imageRecycler, itemRecycler;
    private MapView mapView;
    private GoogleMap googleMap;
    private Button buttonBookNow;

    private String vendorName;
    private FirebaseDatabase database;
    private FirebaseStorage storage;

    private VendorImageAdapter imageAdapter;
    private VendorItemAdapter itemAdapter;

    private ArrayList<String> imageUrls = new ArrayList<>();
    private ArrayList<VendorItem> itemList = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_interface_caterer1);

        // 1) Hook up views
        serviceProvider = findViewById(R.id.textView);
        serviceName     = findViewById(R.id.textView2);
        locationText    = findViewById(R.id.textView3);
        plateRate       = findViewById(R.id.textView4);
        imageRecycler   = findViewById(R.id.recyclerViewImages);
        itemRecycler    = findViewById(R.id.recyclerViewItems);
        mapView         = findViewById(R.id.mapView);
        buttonBookNow          =findViewById(R.id.buttonBookNow);
        buttonBookNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Customer_interface_caterer1.this,caterer_payment.class);
                intent.putExtra("Vendor_name", (CharSequence) vendorName);
                startActivity(intent);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // 2) Intent + Firebase
        vendorName = getIntent().getStringExtra("vendor_name");
        if (vendorName == null) {
            Log.e("IntentError", "vendor_name missing");
            finish();
            return;
        }
        database = FirebaseDatabase.getInstance();
        storage  = FirebaseStorage.getInstance();

        // 3) Images RecyclerView
        imageRecycler.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new VendorImageAdapter(this, imageUrls);
        imageRecycler.setAdapter(imageAdapter);
        imageRecycler.setNestedScrollingEnabled(false);

        // 4) Items RecyclerView
        itemRecycler.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        itemAdapter = new VendorItemAdapter(this, itemList);
        itemRecycler.setAdapter(itemAdapter);
        itemRecycler.setNestedScrollingEnabled(false);

        // 5) MapView
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // 6) Load data
        loadVendorData();
        loadVendorImages();
        loadVendorItems();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        MapsInitializer.initialize(this);
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        String addr = locationText.getText().toString();
        if (!addr.isEmpty() && !"N/A".equals(addr)) {
            geocodeAndMark(addr);
        }
    }

    private void geocodeAndMark(String address) {
        try {
            List<Address> results = new Geocoder(this, Locale.getDefault())
                    .getFromLocationName(address, 1);
            if (results != null && !results.isEmpty()) {
                Address a = results.get(0);
                LatLng pos = new LatLng(a.getLatitude(), a.getLongitude());
                googleMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(vendorName));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f));
            }
        } catch (IOException e) {
            Log.e("GeocodeError", e.getMessage());
        }
    }

    private void loadVendorData() {
        DatabaseReference vRef = database.getReference("Vendors").child(vendorName);
        vRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                String svc  = snap.child("service").getValue(String.class);
                String locn = snap.child("location").getValue(String.class);
                serviceProvider.setText(vendorName);
                serviceName.setText(svc  != null ? svc  : "N/A");

                locationText.setText(locn != null ? locn : "N/A");
                if (googleMap != null && locn != null) geocodeAndMark(locn);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Log.e("FirebaseError", e.getMessage());
            }
        });

        DatabaseReference rRef = database.getReference("caterer_details").child(vendorName);
        rRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                String veg    = snap.child("veg_plate_rate").getValue(String.class);
                String nonveg = snap.child("nonveg_plate_rate").getValue(String.class);
                veg    = veg    != null ? veg    : "N/A";
                nonveg = nonveg != null ? nonveg : "N/A";
                plateRate.setText("Veg: " + veg + " | Non-Veg: " + nonveg);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Log.e("FirebaseError", e.getMessage());
            }
        });
    }

    private void loadVendorImages() {
        StorageReference ref = storage.getReference("vendor_images").child(vendorName);
        ref.listAll().addOnSuccessListener(list -> {
            for (StorageReference img : list.getItems()) {
                img.getDownloadUrl().addOnSuccessListener(uri -> {
                    imageUrls.add(uri.toString());
                    imageAdapter.notifyDataSetChanged();
                });
            }
        }).addOnFailureListener(e -> Log.e("StorageError", e.getMessage()));
    }

    /** ← UPDATED: now populating an ArrayList<VendorItem>  **/
    private void loadVendorItems() {
        DatabaseReference iRef = database
                .getReference("caterer_details")
                .child(vendorName)
                .child("items");

        iRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                itemList.clear();
                for (DataSnapshot s : snap.getChildren()) {
                    String name  = s.child("itemName").getValue(String.class);
                    String price = s.child("price").getValue(String.class);
                    if (name  == null) name  = "Unnamed";
                    if (price == null) price = "0";
                    // VendorItem(String name, String price, String description)
                    itemList.add(new VendorItem(name, price, ""));
                }
                itemAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Log.e("FirebaseError", e.getMessage());
            }
        });
    }

    // MapView lifecycle
    @Override protected void onStart()               { super.onStart();    mapView.onStart(); }
    @Override protected void onResume()              { super.onResume();   mapView.onResume(); }
    @Override protected void onPause()               { mapView.onPause();  super.onPause(); }
    @Override protected void onStop()                { mapView.onStop();   super.onStop(); }
    @Override protected void onDestroy()             { mapView.onDestroy();super.onDestroy();}
    @Override public    void onLowMemory()           { mapView.onLowMemory(); super.onLowMemory(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        mapView.onSaveInstanceState(out);
    }
}
