package com.example.evento;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class caterer_payment extends AppCompatActivity implements PaymentResultListener {

    private static final String TAG = caterer_payment.class.getSimpleName();
    private RecyclerView recyclerView1, recyclerView2;
    private TextView perPlateCostText;
    private EditText guestCountEdit, nameEdit, phoneEdit, dateEdit, timeEdit;
    private Button payButton;

    private ArrayList<CaterItem> availableItems = new ArrayList<>();
    private ArrayList<CaterItem> selectedItems = new ArrayList<>();

    private AvailableAdapter availableAdapter;
    private SelectedAdapter selectedAdapter;

    private int perPlateCost = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caterer_payment);

        // Initialize Razorpay SDK
        Checkout.preload(getApplicationContext());

        recyclerView1 = findViewById(R.id.recyclerView1);
        recyclerView2 = findViewById(R.id.recyclerView2);
        perPlateCostText = findViewById(R.id.textView29);
        guestCountEdit = findViewById(R.id.editTextNumberSigned);
        nameEdit = findViewById(R.id.editTextText14);
        phoneEdit = findViewById(R.id.editTextPhone);
        dateEdit = findViewById(R.id.editTextDate2);
        timeEdit = findViewById(R.id.editTextTime);
        payButton = findViewById(R.id.button17);
//        String Vendor_name=getIntent().getStringExtra("Vendor_name");
        recyclerView1.setLayoutManager(new LinearLayoutManager(this));
        recyclerView2.setLayoutManager(new LinearLayoutManager(this));

        availableAdapter = new AvailableAdapter();
        selectedAdapter = new SelectedAdapter();

        recyclerView1.setAdapter(availableAdapter);
        recyclerView2.setAdapter(selectedAdapter);

        fetchItemsFromFirebase();

        payButton.setOnClickListener(v -> startPayment());
    }

    private void fetchItemsFromFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("caterer_details/Behrouz Biryani Catering/items");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    String itemName = itemSnapshot.child("itemName").getValue(String.class);
                    String priceStr = itemSnapshot.child("price").getValue(String.class);
                    int price = Integer.parseInt(priceStr);
                    availableItems.add(new CaterItem(itemName, price));
                }
                availableAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(caterer_payment.this, "Failed to load items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startPayment() {
        String name = nameEdit.getText().toString().trim();
        String phone = phoneEdit.getText().toString().trim();
        String guestsStr = guestCountEdit.getText().toString().trim();
        String date = dateEdit.getText().toString().trim();
        String time = timeEdit.getText().toString().trim();

        // Validate inputs
        if (name.isEmpty() || phone.isEmpty() || guestsStr.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Please select at least one item", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int guests = Integer.parseInt(guestsStr);
            if (guests <= 0) {
                Toast.makeText(this, "Please enter valid guest count", Toast.LENGTH_SHORT).show();
                return;
            }

            int totalAmount = perPlateCost * guests;
            payButton.setText("Pay: ₹" + totalAmount);

            // Start Razorpay payment
            Checkout checkout = new Checkout();
            checkout.setKeyID("rzp_test_g9coO6mLvqk7WR"); // Use same key as venue_payment

            JSONObject options = new JSONObject();
            options.put("name", "Punjabi Catering Services");
            options.put("description", "Food Order Payment");
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png"); // Use same image
            options.put("currency", "INR");
            options.put("amount", totalAmount * 100); // Convert to paise

            // Pre-fill customer details
            JSONObject prefill = new JSONObject();
            prefill.put("email", "customer@example.com"); // Add email field
            prefill.put("contact", phone);
            prefill.put("name", name);
            options.put("prefill", prefill);

            checkout.open(this, options);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid guest count", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Number format error", e);
        } catch (JSONException e) {
            Toast.makeText(this, "Payment error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Payment JSON error", e);
        } catch (Exception e) {
            Toast.makeText(this, "Payment error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Payment failed", e);
        }
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        Log.d(TAG, "Payment successful: " + razorpayPaymentID);
        saveBookingToFirebase();
        Toast.makeText(this, "Payment Successful! ID: " + razorpayPaymentID, Toast.LENGTH_SHORT).show();
        clearForm();
    }

    @Override
    public void onPaymentError(int code, String response) {
        String errorMsg;
        switch (code) {
            case Checkout.NETWORK_ERROR:
                errorMsg = "Network error, please check your connection";
                break;
            case Checkout.INVALID_OPTIONS:
                errorMsg = "Payment configuration error";
                break;
            case Checkout.PAYMENT_CANCELED:
                errorMsg = "Payment canceled by user";
                break;
            case Checkout.TLS_ERROR:
                errorMsg = "Security error occurred";
                break;
            default:
                errorMsg = "Payment failed: " + response;
        }
        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Payment error: Code=" + code + ", Response=" + response);
    }

    private void saveBookingToFirebase() {
        try {
            String name = nameEdit.getText().toString().trim();
            String phone = phoneEdit.getText().toString().trim();
            String guestsStr = guestCountEdit.getText().toString().trim();
            String date = dateEdit.getText().toString().trim();
            String time = timeEdit.getText().toString().trim();

            int guests = Integer.parseInt(guestsStr);
            int totalAmount = perPlateCost * guests;

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings/Punjabi Catering Services").child(name);
            Map<String, Object> booking = new HashMap<>();
            booking.put("customer_name", name);
            booking.put("customer_phone", phone);
            booking.put("amount", totalAmount);
            booking.put("date", date);
            booking.put("time", time);
            booking.put("status", "Paid");
            booking.put("items", getSelectedItemsNames());

            ref.setValue(booking)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(caterer_payment.this, "Booking saved", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Booking saved to Firebase");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(caterer_payment.this, "Failed to save booking", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Firebase save error", e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error saving booking", e);
        }
    }

    private String getSelectedItemsNames() {
        StringBuilder items = new StringBuilder();
        for (CaterItem item : selectedItems) {
            items.append(item.name).append(", ");
        }
        if (items.length() > 0) {
            items.setLength(items.length() - 2); // Remove last comma
        }
        return items.toString();
    }

    private void clearForm() {
        nameEdit.setText("");
        phoneEdit.setText("");
        guestCountEdit.setText("");
        dateEdit.setText("");
        timeEdit.setText("");

        // Reset items
        availableItems.addAll(selectedItems);
        selectedItems.clear();
        perPlateCost = 0;
        perPlateCostText.setText("Per plate cost: 0");

        availableAdapter.notifyDataSetChanged();
        selectedAdapter.notifyDataSetChanged();
    }

    class CaterItem {
        String name;
        int price;

        public CaterItem(String name, int price) {
            this.name = name;
            this.price = price;
        }
    }

    class AvailableAdapter extends RecyclerView.Adapter<AvailableAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_recycler7, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CaterItem item = availableItems.get(position);
            holder.name.setText(item.name);
            holder.price.setText("₹" + item.price);
            holder.add.setOnClickListener(v -> {
                selectedItems.add(item);
                availableItems.remove(position);
                perPlateCost += item.price;
                perPlateCostText.setText("Per plate cost: ₹" + perPlateCost);
                notifyDataSetChanged();
                selectedAdapter.notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return availableItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, price;
            ImageView add;

            ViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.textView25);
                price = itemView.findViewById(R.id.textView26);
                add = itemView.findViewById(R.id.imageView12);
            }
        }
    }

    class SelectedAdapter extends RecyclerView.Adapter<SelectedAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_recycler8, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CaterItem item = selectedItems.get(position);
            holder.name.setText(item.name);
            holder.price.setText("₹" + item.price);
            holder.remove.setOnClickListener(v -> {
                availableItems.add(item);
                selectedItems.remove(position);
                perPlateCost -= item.price;
                perPlateCostText.setText("Per plate cost: ₹" + perPlateCost);
                notifyDataSetChanged();
                availableAdapter.notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return selectedItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, price;
            ImageView remove;

            ViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.textView27);
                price = itemView.findViewById(R.id.textView28);
                remove = itemView.findViewById(R.id.imageView16);
            }
        }
    }
}
