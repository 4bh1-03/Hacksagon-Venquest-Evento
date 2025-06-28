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
import java.util.Collections;
import java.util.List;

public class decorator extends Fragment {

    private RecyclerView recycler;
    private ServiceAdapter adapter;
    private List<ServiceModel> serviceList = new ArrayList<>();
    private List<ServiceModel> fullList = new ArrayList<>();

    private DatabaseReference vendorsRef;
    private DatabaseReference decorateRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_homefragment, container, false);

        // Recycler setup
        recycler = root.findViewById(R.id.recyclerView3);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        // Adapter with click listener for Decoration items
        adapter = new ServiceAdapter(getContext(), serviceList, new ServiceAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ServiceModel service) {
                // Only Decoration items are shown here, but double‑check:
                if ("Decoration".equals(service.getServiceType())) {
                    Intent intent = new Intent(getContext(), customer_interface_decorator1.class);
                    intent.putExtra("vendor_name", service.getCompanyName());
                    startActivity(intent);
                }
            }
        });
        recycler.setAdapter(adapter);

        // Firebase references
        vendorsRef = FirebaseDatabase.getInstance().getReference("Vendors");
        decorateRef = FirebaseDatabase.getInstance().getReference("decorations");

        // Load decorators only
        fetchDecorators();

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

    private void fetchDecorators() {
        vendorsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                serviceList.clear();
                List<ServiceModel> tempList = new ArrayList<>();
                int[] pending = {0};

                for (DataSnapshot vSnap : snap.getChildren()) {
                    final String company = vSnap.child("companyName").getValue(String.class);
                    final String service = vSnap.child("service").getValue(String.class);
                    final String location = vSnap.child("location").getValue(String.class);
                    final String imgUrl = vSnap.child("imageUrl").getValue(String.class);

                    String svc = service == null ? "" : service.toLowerCase();
                    if (svc.contains("decor")) {
                        pending[0]++;
                        decorateRef.child(company).child("items")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dSnap) {
                                        List<Integer> prices = new ArrayList<>();
                                        for (DataSnapshot item : dSnap.getChildren()) {
                                            String p = item.child("price").getValue(String.class);
                                            if (p != null && !p.isEmpty()) {
                                                try {
                                                    prices.add(Integer.parseInt(p));
                                                } catch (NumberFormatException ignored) {}
                                            }
                                        }
                                        if (!prices.isEmpty()) {
                                            int low = Collections.min(prices);
                                            int high = Collections.max(prices);
                                            String details = "Price Range: " + low + " - " + high;
                                            tempList.add(new ServiceModel(company, "Decoration", details, location, imgUrl));
                                        }
                                        checkAndUpdate(tempList, pending);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError e) {
                                        checkAndUpdate(tempList, pending);
                                    }
                                });
                    }
                }

                if (pending[0] == 0) {
                    finalizeList(tempList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkAndUpdate(List<ServiceModel> tempList, int[] pending) {
        pending[0]--;
        if (pending[0] == 0) {
            finalizeList(tempList);
        }
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