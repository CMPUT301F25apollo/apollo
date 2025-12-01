package com.example.apollo.ui.entrant.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.apollo.R;
import com.example.apollo.models.Event;

import java.util.List;

/**
 * EventsAdapter.java
 *
 * Adapter used to display a list of events inside the profile section.
 * Each item shows the event's poster and title, and notifies a listener
 * when the user taps the event.
 *
 * Responsibilities:
 * - Bind Event model data to a RecyclerView card
 * - Show a placeholder image when no poster URL is available
 * - Relay click events through a simple callback interface
 */
public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private List<Event> events;
    private OnEventClickListener listener;

    /**
     * Listener interface for handling clicks on event cards.
     */
    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    /**
     * Creates an adapter with the given list of events.
     *
     * @param events List of Event models to display.
     */
    public EventsAdapter(List<Event> events) {
        this.events = events;
    }

    /**
     * Optional: assigns a callback for event clicks.
     *
     * @param listener the listener that handles card tap events
     */
    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        // Set title
        holder.title.setText(
                event.getTitle() != null ? event.getTitle() : "Untitled Event"
        );

        // Load poster or placeholder
        if (event.getEventPosterUrl() != null &&
                !event.getEventPosterUrl().trim().isEmpty()) {

            Glide.with(holder.poster.getContext())
                    .load(event.getEventPosterUrl())
                    .into(holder.poster);

        } else {
            holder.poster.setImageResource(R.drawable.placeholder_image);
        }

        // Handle click
        holder.container.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(event);
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder for an individual event card.
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        ImageView poster;
        LinearLayout container;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.eventCard);
            title     = itemView.findViewById(R.id.eventTitle);
            poster    = itemView.findViewById(R.id.eventPosterImage);
        }
    }
}
