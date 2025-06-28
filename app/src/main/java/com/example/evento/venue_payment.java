package com.example.evento;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

public class venue_payment extends AppCompatActivity implements PaymentResultListener {

    private static final String TAG = venue_payment.class.getSimpleName();
    private Button payButton;
    private int finalAmount;
    private String vendorName, time, date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venue_payment);

        // Preload Razorpay Checkout
        Checkout.preload(getApplicationContext());

        // Initialize button
        payButton = findViewById(R.id.payButton); // Ensure this ID exists in your layout

        // Get intent extras
        vendorName = getIntent().getStringExtra("vendor_name");
        time = getIntent().getStringExtra("time");
        date = getIntent().getStringExtra("date");
        finalAmount = getIntent().getIntExtra("finalAmount", 0);

        // Set button text
        payButton.setText("Pay ₹" + finalAmount);

        // Click to initiate payment
        payButton.setOnClickListener(v -> startPayment());
    }

    private void startPayment() {
        Checkout checkout = new Checkout();
        checkout.setKeyID("rzp_test_g9coO6mLvqk7WR"); // Replace with your Razorpay test/live key

        try {
            JSONObject options = new JSONObject();
            options.put("name", vendorName);
            options.put("description", "Venue Booking Payment");
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png");
            options.put("currency", "INR");
            options.put("amount", finalAmount * 100); // Amount in paise

            JSONObject prefill = new JSONObject();
            prefill.put("email", "test@example.com");
            prefill.put("contact", "9876543210");
            options.put("prefill", prefill);

            checkout.open(this, options);

        } catch (Exception e) {
            Log.e(TAG, "Error in starting Razorpay Checkout", e);
            Toast.makeText(this, "Payment Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        Toast.makeText(this, "Payment Success: " + razorpayPaymentID, Toast.LENGTH_LONG).show();

//        Myevents myeventsFragment = Myevents.newInstance(vendorName, date, time);
//
//        getSupportFragmentManager()
//                .beginTransaction()
//                .replace(android.R.id.content, myeventsFragment) // Replace with your container ID if needed
//                .addToBackStack(null)
//                .commit();
    }


    @Override
    public void onPaymentError(int code, String response) {
        Toast.makeText(this, "Payment failed: " + response, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Payment error: Code=" + code + ", Response=" + response);
    }
}
