package com.example.evento;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SelectedAdapter extends RecyclerView.Adapter<SelectedAdapter.ViewHolder> {
    List<CaterItem> items;
    OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemRemove(CaterItem item);
    }

    public SelectedAdapter(List<CaterItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView item, price;
        ImageView remove;

        public ViewHolder(View v) {
            super(v);
            item = v.findViewById(R.id.textView27);
            price = v.findViewById(R.id.textView28);
            remove = v.findViewById(R.id.imageView16);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_recycler8, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CaterItem item = items.get(position);
        holder.item.setText(item.itemName);
        holder.price.setText(item.price);
        holder.remove.setOnClickListener(v -> listener.onItemRemove(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
