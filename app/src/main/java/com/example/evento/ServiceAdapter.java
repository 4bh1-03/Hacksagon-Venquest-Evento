package com.example.evento;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.VH> {

    private Context ctx;
    private List<ServiceModel> items;
    private OnItemClickListener listener;

    // 👇 Click listener interface
    public interface OnItemClickListener {
        void onItemClick(ServiceModel serviceModel);
    }

    // 👇 Updated constructor with listener
    public ServiceAdapter(Context ctx, List<ServiceModel> items, OnItemClickListener listener) {
        this.ctx = ctx;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.custom_recycler3, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ServiceModel m = items.get(position);
        holder.tvName.setText(m.getCompanyName());
        holder.tvType.setText(m.getServiceType());
        holder.tvDetails.setText(m.getDetailText());
        holder.tvLocation.setText(m.getLocation());

        Picasso.get()
                .load(m.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .into(holder.iv);

        // 👇 Set click listener on the whole item
        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onItemClick(m);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView iv;
        TextView tvName, tvType, tvDetails, tvLocation;

        VH(@NonNull View v) {
            super(v);
            iv         = v.findViewById(R.id.imageView9);
            tvName     = v.findViewById(R.id.textView13);
            tvType     = v.findViewById(R.id.textView12);
            tvDetails  = v.findViewById(R.id.textView20);
            tvLocation = v.findViewById(R.id.textView22);
        }
    }
}
