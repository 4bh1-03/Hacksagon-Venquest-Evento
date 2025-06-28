package com.example.evento;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Caterer_detail_upload extends AppCompatActivity {
    private ProgressDialog progressDialog;
    private DatabaseReference mDatabase;

    // RecyclerView & Adapter for Items only
    private RecyclerView itemsRecyclerView;
    private ItemAdapter itemAdapter;

    // UI
    private EditText vendorNameInput, vegCostInput, nonVegCostInput;
    private ImageView addItemButton;
    private Button uploadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caterer_detail_upload);

        // Firebase
        mDatabase = FirebaseDatabase.getInstance()
                .getReference("caterer_details");

        // Views
        vendorNameInput   = findViewById(R.id.catererName);
        vegCostInput      = findViewById(R.id.vegCostInput);
        nonVegCostInput   = findViewById(R.id.nonVegCostInput);

        itemsRecyclerView = findViewById(R.id.recyclerView2);
        addItemButton     = findViewById(R.id.imageView8);
        uploadButton      = findViewById(R.id.button5);

        // Progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // Set up ItemAdapter
        itemAdapter = new ItemAdapter();
        itemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemsRecyclerView.setAdapter(itemAdapter);

        // Listeners
        addItemButton.setOnClickListener(v -> itemAdapter.addNewItem());
        uploadButton .setOnClickListener(v -> uploadAllDetails());
    }

    /**
     * Uploads under /caterer_details/{vendor}:
     *   - items: List<ItemPrice>
     *   - veg_plate_rate: String
     *   - nonveg_plate_rate: String
     */
    private void uploadAllDetails() {
        String vendor = vendorNameInput.getText().toString().trim();
        if (vendor.isEmpty()) {
            Toast.makeText(this, "Enter caterer name", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ItemPrice> items = itemAdapter.getItemList();
        if (items.isEmpty()) {
            Toast.makeText(this, "Add at least one item", Toast.LENGTH_SHORT).show();
            return;
        }

        String vegRate    = vegCostInput.getText().toString().trim();
        String nonVegRate = nonVegCostInput.getText().toString().trim();
        if (vegRate.isEmpty() || nonVegRate.isEmpty()) {
            Toast.makeText(this, "Enter both veg and non-veg plate rates", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Uploading details…");
        progressDialog.show();

        // Build one map with everything under this vendor
        Map<String, Object> data = new HashMap<>();
        data.put("items",             items);
        data.put("veg_plate_rate",    vegRate);
        data.put("nonveg_plate_rate", nonVegRate);

        mDatabase.child(vendor)
                .setValue(data)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "All details uploaded!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /*** ITEM ADAPTER ***/
    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
        private final List<ItemPrice> itemList = new ArrayList<>();

        class ItemViewHolder extends RecyclerView.ViewHolder {
            final EditText itemName, itemPrice;
            final ImageView removeItem;

            ItemViewHolder(android.view.View itemView) {
                super(itemView);
                itemName   = itemView.findViewById(R.id.itemName);
                itemPrice  = itemView.findViewById(R.id.itemPrice);
                removeItem = itemView.findViewById(R.id.removeImage);

                // Update model as user types
                itemName.addTextChangedListener(new SimpleTextWatcher(s ->
                        itemList.get(getAdapterPosition()).setItemName(s)
                ));
                itemPrice.addTextChangedListener(new SimpleTextWatcher(s ->
                        itemList.get(getAdapterPosition()).setPrice(s)
                ));

                removeItem.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        itemList.remove(pos);
                        notifyItemRemoved(pos);
                    }
                });
            }
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.custom_recycler2, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            ItemPrice ip = itemList.get(position);
            holder.itemName.setText(ip.getItemName());
            holder.itemPrice.setText(ip.getPrice());
        }

        @Override public int getItemCount() { return itemList.size(); }

        void addNewItem() {
            itemList.add(new ItemPrice("", ""));
            notifyItemInserted(itemList.size() - 1);
        }

        List<ItemPrice> getItemList() {
            return new ArrayList<>(itemList);
        }
    }

    /*** SIMPLE TEXT WATCHER HELPER ***/
    // (captures only onTextChanged and forwards the new String)
    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final java.util.function.Consumer<String> onText;
        SimpleTextWatcher(java.util.function.Consumer<String> onText) {
            this.onText = onText;
        }
        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void afterTextChanged(android.text.Editable s) {}
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
            onText.accept(s.toString());
        }
    }

    /*** ITEM PRICE MODEL ***/
    public static class ItemPrice {
        private String itemName, price;
        public ItemPrice(){}  // for Firebase
        public ItemPrice(String n, String p) { itemName = n; price = p; }
        public String getItemName() { return itemName; }
        public String getPrice()     { return price; }
        public void setItemName(String n) { itemName = n; }
        public void setPrice(String p)    { price = p; }
    }
}
