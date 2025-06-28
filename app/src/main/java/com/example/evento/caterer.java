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

public class caterer extends Fragment {
    private RecyclerView recycler;
    private ServiceAdapter adapter;
    private List<ServiceModel> serviceList = new ArrayList<>();
    private List<ServiceModel> fullList    = new ArrayList<>();

    private DatabaseReference vendorsRef;
    private DatabaseReference catererRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_homefragment, container, false);

        recycler = root.findViewById(R.id.recyclerView3);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter with click listener
        adapter = new ServiceAdapter(
                getContext(),
                serviceList,
                service -> {
                    // only react to Catering cards
                    if ("Catering".equals(service.getServiceType())) {
                        Intent intent = new Intent(getContext(), Customer_interface_caterer1.class);
                        intent.putExtra("vendor_name", service.getCompanyName());
                        startActivity(intent);
                    }
                }
        );
        recycler.setAdapter(adapter);

        vendorsRef = FirebaseDatabase.getInstance().getReference("Vendors");
        catererRef = FirebaseDatabase.getInstance().getReference("caterer_details");

        fetchCaterers();

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

    private void fetchCaterers() {
        vendorsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                serviceList.clear();
                List<ServiceModel> tempList = new ArrayList<>();
                final int[] pending = {0};

                for (DataSnapshot vSnap : snap.getChildren()) {
                    String company  = vSnap.child("companyName").getValue(String.class);
                    String service  = vSnap.child("service").getValue(String.class);
                    String location = vSnap.child("location").getValue(String.class);
                    String imgUrl   = vSnap.child("imageUrl").getValue(String.class);

                    String svc = (service == null ? "" : service.toLowerCase());
                    if (svc.contains("cater")) {
                        pending[0]++;
                        catererRef.child(company)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot cSnap) {
                                        String veg    = cSnap.child("veg_plate_rate").getValue(String.class);
                                        String nonveg = cSnap.child("nonveg_plate_rate").getValue(String.class);
                                        if (veg    == null || veg.isEmpty())    veg = "N/A";
                                        if (nonveg == null || nonveg.isEmpty()) nonveg = "N/A";
                                        String details = "Veg: " + veg + ", Non-Veg: " + nonveg;

                                        tempList.add(new ServiceModel(
                                                company,
                                                "Catering",
                                                details,
                                                location,
                                                imgUrl
                                        ));
                                        if (--pending[0] == 0) finalizeList(tempList);
                                    }
                                    @Override public void onCancelled(@NonNull DatabaseError e) {
                                        if (--pending[0] == 0) finalizeList(tempList);
                                    }
                                });
                    }
                }

                if (pending[0] == 0) {
                    // no async calls needed
                    finalizeList(tempList);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void finalizeList(List<ServiceModel> all) {
        fullList.clear();
        fullList.addAll(all);
        serviceList.clear();
        serviceList.addAll(all);
        adapter.notifyDataSetChanged();
    }

    private void filter(String text) {
        String q = (text == null ? "" : text.toLowerCase().trim());
        serviceList.clear();
        if (q.isEmpty()) {
            serviceList.addAll(fullList);
        } else {
            for (ServiceModel m : fullList) {
                if (m.getCompanyName().toLowerCase().contains(q)
                        || m.getServiceType().toLowerCase().contains(q)
                        || m.getDetailText().toLowerCase().contains(q)
                        || m.getLocation().toLowerCase().contains(q)) {
                    serviceList.add(m);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}
