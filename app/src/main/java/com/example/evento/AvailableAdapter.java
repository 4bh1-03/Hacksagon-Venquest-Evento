package com.example.evento;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AvailableAdapter extends RecyclerView.Adapter<AvailableAdapter.ViewHolder> {
    List<CaterItem> items;
    OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemAdd(CaterItem item);
    }

    public AvailableAdapter(List<CaterItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView item, price;
        ImageView add;

        public ViewHolder(View v) {
            super(v);
            item = v.findViewById(R.id.textView25);
            price = v.findViewById(R.id.textView26);
            add = v.findViewById(R.id.imageView12);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_recycler7, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CaterItem item = items.get(position);
        holder.item.setText(item.itemName);
        holder.price.setText(item.price);
        holder.add.setOnClickListener(v -> listener.onItemAdd(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}