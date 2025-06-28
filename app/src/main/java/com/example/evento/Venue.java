package com.example.evento;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Venue extends Fragment {

    private RecyclerView recycler;
    private ServiceAdapter adapter;
    private List<ServiceModel> serviceList = new ArrayList<>();
    private List<ServiceModel> fullList = new ArrayList<>();

    private DatabaseReference vendorsRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_homefragment, container, false);

        // Recycler setup
        recycler = root.findViewById(R.id.recyclerView3);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        // Set adapter with click listener
        adapter = new ServiceAdapter(getContext(), serviceList, new ServiceAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ServiceModel service) {
                Intent intent = new Intent(getContext(), customer_view_venue1.class);

                intent.putExtra("vendor_name", service.getCompanyName());
                startActivity(intent);
            }
        });

        recycler.setAdapter(adapter);

        // Firebase reference
        vendorsRef = FirebaseDatabase.getInstance().getReference("Vendors");

        // Load venues only
        fetchVenues();

        // SearchView setup
        SearchView sv = root.findViewById(R.id.searchView);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });

        return root;
    }

    private void fetchVenues() {
        vendorsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                serviceList.clear();
                List<ServiceModel> tempList = new ArrayList<>();

                for (DataSnapshot vSnap : snap.getChildren()) {
                    String company = vSnap.child("companyName").getValue(String.class);
                    String location = vSnap.child("location").getValue(String.class);
                    String imgUrl = vSnap.child("imageUrl").getValue(String.class);
                    String capacity = vSnap.child("capacity").getValue(String.class);
                    String rent = vSnap.child("rent").getValue(String.class);

                    if (capacity != null && !capacity.isEmpty()
                            && rent != null && !rent.isEmpty()) {
                        String details = "Capacity: " + capacity + ", Price: " + rent;
                        tempList.add(new ServiceModel(company, "Venue", details, location, imgUrl));
                    }
                }

                finalizeList(tempList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void finalizeList(List<ServiceModel> all) {
        serviceList.clear();
        serviceList.addAll(all);
        fullList.clear();
        fullList.addAll(all);
        adapter.notifyDataSetChanged();
    }

    private void filter(String text) {
        String query = text == null ? "" : text.toLowerCase().trim();
        serviceList.clear();

        if (query.isEmpty()) {
            serviceList.addAll(fullList);
        } else {
            for (ServiceModel m : fullList) {
                if (m.getCompanyName().toLowerCase().contains(query)
                        || m.getServiceType().toLowerCase().contains(query)
                        || m.getDetailText().toLowerCase().contains(query)
                        || m.getLocation().toLowerCase().contains(query)) {
                    serviceList.add(m);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}
