package com.example.apollo.ui.entrant.profile;

import android.content.Context;
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

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private List<Event> events;
    private OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public EventsAdapter(List<Event> events) {
        this.events = events;
    }

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

        // Title
        if (event.getTitle() != null) {
            holder.title.setText(event.getTitle());
        } else {
            holder.title.setText("Untitled Event");
        }

        // Poster Image
        if (event.getEventPosterUrl() != null && !event.getEventPosterUrl().trim().isEmpty()) {
            Glide.with(holder.poster.getContext())
                    .load(event.getEventPosterUrl())
                    .into(holder.poster);
        } else {
            // placeholder for missing image
            holder.poster.setImageResource(R.drawable.placeholder_image);
        }

        // Click event
        holder.container.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(event);
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        ImageView poster;
        LinearLayout container;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);

            container = itemView.findViewById(R.id.eventCard);
            title = itemView.findViewById(R.id.eventTitle);
            poster = itemView.findViewById(R.id.eventPosterImage);
        }
    }
}
