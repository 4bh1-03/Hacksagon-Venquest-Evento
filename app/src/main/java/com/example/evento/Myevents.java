package com.example.evento;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Myevents extends Fragment {

    private String vendorName, date, time;

    private TextView titleTextView, vendorTextView, dateTextView, timeTextView;

    public Myevents() {
        // Required empty public constructor
    }

    // Static method to create new instance with data
    public static Myevents newInstance(String vendorName, String date, String time) {
        Myevents fragment = new Myevents();
        Bundle args = new Bundle();
        args.putString("vendor_name", vendorName);
        args.putString("date", date);
        args.putString("time", time);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout
        return inflater.inflate(R.layout.fragment_myevents, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retrieve arguments
        if (getArguments() != null) {
            vendorName = getArguments().getString("vendor_name", "");
            date = getArguments().getString("date", "");
            time = getArguments().getString("time", "");
        }

        // Bind views
        titleTextView = view.findViewById(R.id.textView18);
        vendorTextView = view.findViewById(R.id.textView30);
        dateTextView = view.findViewById(R.id.textView31);
        timeTextView = view.findViewById(R.id.textView32);

        // Update UI
        titleTextView.setText("Your Events");
        vendorTextView.setText("Vendor: " + vendorName);
        dateTextView.setText("Date: " + date);
        timeTextView.setText("Time: " + time);
    }
}
