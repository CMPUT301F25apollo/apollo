package com.example.apollo.ui.entrant.notifications;

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

/**
 * NotificationsAdapter.java
 *
 * Purpose:
 * Displays a list of notifications in a RecyclerView.
 * Each item shows a title, message, and a colored dot indicating whether the notification
 * has been read or not.
 *
 * Design Pattern:
 * Implements the Adapter design pattern to connect notification data (model)
 * with the RecyclerView UI (view).
 *
 * Notes:
 * - Currently, click functionality is disabled.
 * - Can be expanded later to handle click actions or mark notifications as read.
 */
public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.VH> {

    /**
     * Interface for handling click events on notifications.
     */
    public interface OnNotificationClick {
        void onClick(NotificationsViewModel n, int position);
    }

    private final List<NotificationsViewModel> data = new ArrayList<>();
    private final OnNotificationClick onClick;

    /**
     * Constructor for the adapter.
     *
     * @param onClick Callback for handling notification clicks.
     */
    public NotificationsAdapter(OnNotificationClick onClick) {
        this.onClick = onClick;
    }

    /**
     * Updates the adapter with a new list of notifications.
     *
     * @param items The list of notification items to display.
     */
    public void setData(List<NotificationsViewModel> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    /**
     * Creates a new ViewHolder for a notification item.
     *
     * @param parent The parent view group.
     * @param viewType The type of view for the item.
     * @return A new ViewHolder instance.
     */
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    /**
     * Binds data to a ViewHolder at a specific position.
     * Sets the title, message, and read/unread visual styles.
     *
     * @param h The ViewHolder to bind data to.
     * @param pos The position of the item in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        NotificationsViewModel n = data.get(pos);

        // Set notification title and message
        h.title.setText(n.title == null ? "Notification" : n.title);
        String msg = n.message == null ? "" : n.message.replace(" Tap to register.", "").trim();
        h.message.setText(msg);

        // Update dot color and title style based on read status
        h.dot.setVisibility(View.VISIBLE);
        int unreadColor = ContextCompat.getColor(h.itemView.getContext(), android.R.color.holo_blue_dark);
        int readColor   = ContextCompat.getColor(h.itemView.getContext(), android.R.color.darker_gray);
        h.dot.setBackgroundColor(n.read ? readColor : unreadColor);

        h.title.setTypeface(null, n.read ? Typeface.NORMAL : Typeface.BOLD);

        // Disable click functionality for now
//        h.itemView.setOnClickListener(null);
//        h.itemView.setClickable(false);
//        h.itemView.setFocusable(false);


        //making it clickable
        h.itemView.setOnClickListener(v -> {
            if (onClick != null) {
                onClick.onClick(n, pos);
            }
        });
    }

    /**
     * Returns the total number of notification items.
     *
     * @return Number of items in the adapter.
     */
    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * ViewHolder class for holding notification item views.
     */
    static class VH extends RecyclerView.ViewHolder {
        TextView title, message;
        View dot;

        /**
         * Constructs a ViewHolder for a single notification item.
         *
         * @param itemView The view of the notification item.
         */
        VH(@NonNull View itemView) {
            super(itemView);
            title   = itemView.findViewById(R.id.title);
            message = itemView.findViewById(R.id.message);
            dot     = itemView.findViewById(R.id.dot);
        }
    }
}