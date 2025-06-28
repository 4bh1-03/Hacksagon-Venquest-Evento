package com.example.evento;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class customer_view_venue1 extends AppCompatActivity implements OnMapReadyCallback {

    private RecyclerView recyclerViewImages;
    private ImageAdapter imageAdapter;
    private List<String> imageUrls = new ArrayList<>();

    private TextView providerNameTV, serviceNameTV, venueCapacityTV, venueRentTV, locationTV, availabilityTV;
    private EditText dateET, timeET, customerNameET, customerPhoneET;
    private Button checkAvailabilityBtn, bookNowBtn;

    private DatabaseReference databaseReference;
    private String vendorName;
    private double latitude = 0.0, longitude = 0.0;

    private MapView mapView;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_view_venue1);

        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        providerNameTV = findViewById(R.id.textView);
        serviceNameTV = findViewById(R.id.textView2);
        venueCapacityTV = findViewById(R.id.textView21);
        venueRentTV = findViewById(R.id.textView23);
        locationTV = findViewById(R.id.textView3);
        availabilityTV = findViewById(R.id.textView24);
        dateET = findViewById(R.id.editTextDate);
        timeET = findViewById(R.id.editTextText5);
        customerNameET = findViewById(R.id.editTextText12);
        customerPhoneET = findViewById(R.id.editTextNumber2);
        checkAvailabilityBtn = findViewById(R.id.button16);
        bookNowBtn = findViewById(R.id.button15);

        vendorName = getIntent().getStringExtra("vendor_name");
        if (vendorName == null || vendorName.isEmpty()) {
            Toast.makeText(this, "Vendor name is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("Vendors").child(vendorName);

        recyclerViewImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImageAdapter(imageUrls, this);
        recyclerViewImages.setAdapter(imageAdapter);

        loadVendorDetails();

        dateET.setOnClickListener(v -> showDatePicker());

        checkAvailabilityBtn.setOnClickListener(view -> checkAvailability());

        bookNowBtn.setOnClickListener(v -> {
            String customerName = customerNameET.getText().toString().trim();
            String customerPhone = customerPhoneET.getText().toString().trim();
            String date = dateET.getText().toString().trim();
            String timeRange = timeET.getText().toString().trim();
            String rentStr = venueRentTV.getText().toString().replaceAll("[^\\d]", "");

            if (customerName.isEmpty() || customerPhone.isEmpty() || date.isEmpty() || timeRange.isEmpty() || rentStr.isEmpty()) {
                Toast.makeText(this, "Please fill all booking fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isDateWithinRange(date)) {
                Toast.makeText(this, "Date must be from today up to one year ahead", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidTimeRange(timeRange)) {
                Toast.makeText(this, "Enter time in correct 24-hour format (e.g., 14:00-16:00)", Toast.LENGTH_SHORT).show();
                return;
            }

            int rentPerHour = Integer.parseInt(rentStr);
            int hours = calculateHours(timeRange);
            int finalAmount = rentPerHour * hours;

            DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings");
            DatabaseReference customerBookingRef = bookingsRef.child(vendorName).child(customerName);

            Map<String, Object> bookingData = new HashMap<>();
            bookingData.put("date", date);
            bookingData.put("time", timeRange);
            bookingData.put("amount", finalAmount);
            bookingData.put("customer_name", customerName);
            bookingData.put("customer_phone", customerPhone);

            customerBookingRef.setValue(bookingData).addOnSuccessListener(task -> {
                Intent intent = new Intent(this, venue_payment.class);
                intent.putExtra("vendor_name", vendorName);
                intent.putExtra("finalAmount", finalAmount);
                intent.putExtra("customer_name", customerName);
                intent.putExtra("customer_phone", customerPhone);
                intent.putExtra("date", date);
                intent.putExtra("time", timeRange);
                startActivity(intent);
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to book venue", Toast.LENGTH_SHORT).show();
            });
        });

        mapView = findViewById(R.id.mapView);
        Bundle mapViewBundle = savedInstanceState != null ? savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY) : null;
        mapView.onCreate(mapViewBundle);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            dateET.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        Calendar minDate = Calendar.getInstance();
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, 1);
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
        datePickerDialog.show();
    }

    private boolean isDateWithinRange(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setLenient(false);
            Date inputDate = sdf.parse(dateStr);

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar oneYearLater = Calendar.getInstance();
            oneYearLater.add(Calendar.YEAR, 1);

            return inputDate != null && !inputDate.before(today.getTime()) && !inputDate.after(oneYearLater.getTime());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidTimeRange(String timeRange) {
        return timeRange.matches("^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$");
    }

    private void loadVendorDetails() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    providerNameTV.setText(getValueOrNA(snapshot, "companyName"));
                    serviceNameTV.setText(getValueOrNA(snapshot, "service"));

                    String capacity = getValueOrNA(snapshot, "capacity");
                    String rent = getValueOrNA(snapshot, "rent");
                    String location = getValueOrNA(snapshot, "location");

                    venueCapacityTV.setText("Capacity: " + capacity);
                    venueRentTV.setText("Rent/hr: " + rent);
                    locationTV.setText(location);

                    geocodeLocation(location);
                    loadImagesFromStorage(vendorName);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(customer_view_venue1.this, "Failed to load vendor data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getValueOrNA(DataSnapshot snapshot, String key) {
        if (snapshot.hasChild(key) && snapshot.child(key).getValue() != null) {
            return snapshot.child(key).getValue(String.class);
        } else {
            return "N/A";
        }
    }

    private void loadImagesFromStorage(String vendorName) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference listRef = storage.getReference().child("vendor_images").child(vendorName);

        listRef.listAll().addOnSuccessListener(listResult -> {
            imageUrls.clear();
            for (StorageReference item : listResult.getItems()) {
                item.getDownloadUrl().addOnSuccessListener(uri -> {
                    imageUrls.add(uri.toString());
                    imageAdapter.notifyDataSetChanged();
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading image URL", Toast.LENGTH_SHORT).show());
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to list images from storage.", Toast.LENGTH_SHORT).show());
    }

    private void geocodeLocation(String locationAddress) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationAddress, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                latitude = address.getLatitude();
                longitude = address.getLongitude();

                if (mapView != null) {
                    mapView.getMapAsync(this);
                }
            } else {
                Toast.makeText(this, "Could not find location on map", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Geocoding failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private int calculateHours(String timeRange) {
        try {
            String[] parts = timeRange.split("-");
            String[] start = parts[0].split(":");
            String[] end = parts[1].split(":");
            int startHour = Integer.parseInt(start[0]);
            int startMin = Integer.parseInt(start[1]);
            int endHour = Integer.parseInt(end[0]);
            int endMin = Integer.parseInt(end[1]);

            int totalStartMinutes = startHour * 60 + startMin;
            int totalEndMinutes = endHour * 60 + endMin;
            int durationMinutes = totalEndMinutes - totalStartMinutes;

            return Math.max(1, durationMinutes / 60);
        } catch (Exception e) {
            return 1;
        }
    }

    private void checkAvailability() {
        String date = dateET.getText().toString().trim();
        String timeRange = timeET.getText().toString().trim();

        if (date.isEmpty() || timeRange.isEmpty()) {
            Toast.makeText(this, "Please enter both date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isDateWithinRange(date)) {
            Toast.makeText(this, "Date must be from today up to one year ahead", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidTimeRange(timeRange)) {
            Toast.makeText(this, "Enter time in correct 24-hour format (e.g., 14:00-16:00)", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings");

        bookingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean available = true;

                if (snapshot.hasChild(vendorName)) {
                    DataSnapshot vendorBookings = snapshot.child(vendorName);
                    for (DataSnapshot bookingDetail : vendorBookings.getChildren()) {
                        String bookedDate = bookingDetail.child("date").getValue(String.class);
                        String bookedTime = bookingDetail.child("time").getValue(String.class);
                        if (date.equals(bookedDate) && timeRange.equals(bookedTime)) {
                            available = false;
                            break;
                        }
                    }
                }

                if (available) {
                    availabilityTV.setText("Available");
                    availabilityTV.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    availabilityTV.setText("Not Available");
                    availabilityTV.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(customer_view_venue1.this, "Error checking availability", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override public void onMapReady(@NonNull GoogleMap googleMap) {
        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(this, "Invalid or missing location data", Toast.LENGTH_SHORT).show();
            return;
        }
        LatLng vendorLocation = new LatLng(latitude, longitude);
        googleMap.addMarker(new MarkerOptions().position(vendorLocation).title("Vendor Location"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vendorLocation, 15));
    }

    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onPause() { mapView.onPause(); super.onPause(); }
    @Override protected void onStop() { mapView.onStop(); super.onStop(); }
    @Override protected void onDestroy() { mapView.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }

    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }
        mapView.onSaveInstanceState(mapViewBundle);
    }
}
