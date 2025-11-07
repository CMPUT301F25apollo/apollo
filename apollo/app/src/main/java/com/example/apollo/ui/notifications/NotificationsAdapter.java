package com.example.apollo.ui.notifications;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apollo.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.VH> {

    public interface OnNotificationClick {
        void onClick(NotificationsViewModel n, int position);
    }

    private final List<NotificationsViewModel> data = new ArrayList<>();
    private final OnNotificationClick onClick;

    public NotificationsAdapter(OnNotificationClick onClick) {
        this.onClick = onClick;
    }

    public void setData(List<NotificationsViewModel> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        NotificationsViewModel n = data.get(pos);

        h.title.setText(n.title == null ? "Notification" : n.title);
        String msg = n.message == null ? "" : n.message.replace(" Tap to register.", "").trim();
        h.message.setText(msg);

        // Dot + title style (system colors only; no custom color resources)
        h.dot.setVisibility(View.VISIBLE);
        int unreadColor = ContextCompat.getColor(h.itemView.getContext(), android.R.color.holo_blue_dark);
        int readColor   = ContextCompat.getColor(h.itemView.getContext(), android.R.color.darker_gray);
        h.dot.setBackgroundColor(n.read ? readColor : unreadColor);

        h.title.setTypeface(null, n.read ? Typeface.NORMAL : Typeface.BOLD);

        h.itemView.setOnClickListener(null);
        h.itemView.setClickable(false);
        h.itemView.setFocusable(false);
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, message;
        View dot;
        VH(@NonNull View itemView) {
            super(itemView);
            title   = itemView.findViewById(R.id.title);
            message = itemView.findViewById(R.id.message);
            dot     = itemView.findViewById(R.id.dot);
        }
    }
}
