package com.example.evento;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Vendor_details_upload extends AppCompatActivity {
    EditText companyName, userName, phoneNumber, location, venueCapacity, venueRent;
    Spinner serviceSpinner;
    String selectedService = "";
    Button submitBtn;
    DatabaseReference databaseRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            androidx.activity.EdgeToEdge.enable(this);
        }

        setContentView(R.layout.activity_vendor_details_upload);

        databaseRef = FirebaseDatabase.getInstance().getReference("Vendors");

        companyName = findViewById(R.id.company_name);
        userName = findViewById(R.id.your_name);
        serviceSpinner = findViewById(R.id.service_spinner);
        phoneNumber = findViewById(R.id.phone_number);
        location = findViewById(R.id.location);
        venueCapacity = findViewById(R.id.venue_capacity);
        venueRent = findViewById(R.id.venue_rent);
        submitBtn = findViewById(R.id.submitBtn);

        // Spinner setup
        String[] services = {"Select your service","Venues", "Caterers", "Decorators"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, services);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serviceSpinner.setAdapter(adapter);

        // Handle Spinner selection
        serviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedService = parent.getItemAtPosition(position).toString();

                if (position == 0) {
                    // Prompt selected, treat as nothing selected
                    venueCapacity.setVisibility(View.GONE);
                    venueRent.setVisibility(View.GONE);
                    selectedService = ""; // Prevent saving invalid value
                } else if (selectedService.equals("Venues")) {
                    venueCapacity.setVisibility(View.VISIBLE);
                    venueRent.setVisibility(View.VISIBLE);
                } else {
                    venueCapacity.setVisibility(View.GONE);
                    venueRent.setVisibility(View.GONE);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedService = "";
            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        submitBtn.setOnClickListener(v -> saveVendorDetails());
    }

    private void saveVendorDetails() {
        String company = companyName.getText().toString().trim();
        String name = userName.getText().toString().trim();
        String serviceText = selectedService;
        String phone = phoneNumber.getText().toString().trim();
        String loc = location.getText().toString().trim();
        String capacity = venueCapacity.getText().toString().trim();
        String rent = venueRent.getText().toString().trim();

        if (company.isEmpty() || name.isEmpty() || phone.isEmpty() || loc.isEmpty() || selectedService.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

//        String vendorId = databaseRef.push().getKey();
        String vendorId = companyName.getText().toString().trim();
        VendorModel vendor = new VendorModel(company, name, phone, loc, capacity, rent, serviceText);

        databaseRef.child(vendorId).setValue(vendor)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Vendor details submitted!", Toast.LENGTH_SHORT).show();
                    clearFields();
                    Intent intent = new Intent(Vendor_details_upload.this, Image_upload.class);
                    intent.putExtra("vendor_id", vendorId);
                    intent.putExtra("service_provided",serviceText);
                    startActivity(intent);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to submit", Toast.LENGTH_SHORT).show());
    }

    private void clearFields() {
        companyName.setText("");
        userName.setText("");
        serviceSpinner.setSelection(0);
        phoneNumber.setText("");
        location.setText("");
        venueCapacity.setText("");
        venueRent.setText("");
    }

    public static class VendorModel {
        public String companyName, service, userName, phone, location, capacity, rent;

        public VendorModel() {}

        public VendorModel(String companyName, String userName, String phone, String location, String capacity, String rent, String service) {
            this.companyName = companyName;
            this.userName = userName;
            this.phone = phone;
            this.location = location;
            this.capacity = capacity;
            this.rent = rent;
            this.service = service;
        }
    }
}
