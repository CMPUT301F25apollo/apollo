package com.example.apollo.ui.entrant.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apollo.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.VH> {

    public interface OnNotificationAction {
        void onAccept(NotificationsViewModel n, int position);
        void onDecline(NotificationsViewModel n, int position);
    }

    private final List<NotificationsViewModel> data = new ArrayList<>();
    private final OnNotificationAction listener;

    public NotificationsAdapter(OnNotificationAction listener) {
        this.listener = listener;
    }

    public void setData(List<NotificationsViewModel> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        NotificationsViewModel n = data.get(position);

        holder.title.setText(n.title);
        holder.message.setText(n.message);

        boolean isWin = "lottery_win".equals(n.type);
        holder.acceptBtn.setVisibility(isWin ? View.VISIBLE : View.GONE);
        holder.declineBtn.setVisibility(isWin ? View.VISIBLE : View.GONE);

        // Disable buttons if already responded
        if ("accepted".equals(n.status) || "declined".equals(n.status)) {
            holder.acceptBtn.setEnabled(false);
            holder.declineBtn.setEnabled(false);
            holder.itemView.setAlpha(0.5f);
        } else {
            holder.acceptBtn.setEnabled(true);
            holder.declineBtn.setEnabled(true);
            holder.itemView.setAlpha(1.0f);

            // Set listeners only if action hasn't been taken
            holder.acceptBtn.setOnClickListener(v -> listener.onAccept(n, position));
            holder.declineBtn.setOnClickListener(v -> listener.onDecline(n, position));
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, message;
        Button acceptBtn, declineBtn;

        VH(@NonNull View itemView) {
            super(itemView);
            title      = itemView.findViewById(R.id.title);
            message    = itemView.findViewById(R.id.message);
            acceptBtn  = itemView.findViewById(R.id.btnAccept);
            declineBtn = itemView.findViewById(R.id.btnDecline);
        }
    }
}