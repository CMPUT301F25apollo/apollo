package com.example.apollo.ui.admin.events;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.apollo.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EventsFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout eventsContainer;
    private final List<Event> allEvents = new ArrayList<>();
    private TextView searchInput;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events_admin, container, false);

        db = FirebaseFirestore.getInstance();
        eventsContainer = view.findViewById(R.id.eventsContainer);
        searchInput = view.findViewById(R.id.search_events_input);

        loadEventsFromFirestore();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });



        return view;
    }

    private void loadEventsFromFirestore() {
        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allEvents.clear();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String eventId = document.getId();
                        String title = document.getString("title");
                        String posterUrl = document.getString("eventPosterUrl");
                        String location = document.getString("location");
                        Long capacity = document.getLong("eventCapacity");
                        long waitlist =  document.getLong("waitlistCapacity");

                        // Now get waitlist count
                        db.collection("events")
                                .document(eventId)
                                .collection("waitlist")
                                .get()
                                .addOnSuccessListener(waitlistSnapshot -> {

                                    int waitlistCount = waitlistSnapshot.size();

                                    allEvents.add(new Event(
                                            eventId, title, posterUrl, location, capacity, waitlist, waitlistCount
                                    ));

                                    filterEvents(searchInput.getText().toString());
                                });
                    }

                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading events", e));

    }

    private void filterEvents(String query) {
        eventsContainer.removeAllViews();
        String lowerQuery = query.toLowerCase();

        for (Event event : allEvents) {
            if (event.getTitle() != null && event.getTitle().toLowerCase().contains(lowerQuery)) {
                // Inflate a fresh card each time
                View card = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_event_card_admin, eventsContainer, false);

                TextView titleView = card.findViewById(R.id.eventTitle);
                ImageView posterView = card.findViewById(R.id.eventPosterImage);
                ImageView deleteButton = card.findViewById(R.id.delete_button);
                TextView locationView = card.findViewById(R.id.eventLocation);
                TextView capacityView = card.findViewById(R.id.eventCapacity);
                TextView waitlistView = card.findViewById(R.id.eventWaitlist);

                locationView.setText("Location: " + event.getLocation());
                capacityView.setText("Event Capacity: " + event.getCapacity());
                waitlistView.setText("Waitlist Capacity: " + event.waitlistCount + "/" + event.capacity);


                titleView.setText(event.getTitle());

                if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                    Glide.with(getContext())
                            .load(event.getPosterUrl())
                            .into(posterView);
                }

                // Click to navigate to details
                card.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("eventId", event.getId());

                    // Use the fragment reference correctly
                    NavController navController = NavHostFragment.findNavController(EventsFragment.this);

                    // Make sure this action exists in nav_graph.xml
                    navController.navigate(R.id.action_navigation_events_to_navigation_event_details, bundle);
                });


                // Click to delete
                deleteButton.setOnClickListener(v ->
                        showDeleteConfirmationDialog(event.getId(), card, event.getTitle()));

                eventsContainer.addView(card);
            }
        }
    }

    private static class Event {
        private final String id;
        private final String title;
        private final String posterUrl;
        private final String location;
        private final Long capacity;
        private final Long waitlist;
        int waitlistCount;

        public Event(String id, String title, String posterUrl, String location,
                     Long capacity, Long waitlist, int waitlistCount) {
            this.id = id;
            this.title = title;
            this.posterUrl = posterUrl;
            this.location = location;
            this.capacity = capacity;
            this.waitlist = waitlist;
            this.waitlistCount = waitlistCount;

        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getPosterUrl() { return posterUrl; }
        public String getLocation() { return location; }
        public Long getCapacity() { return capacity; }
        public Long getWaitlist() { return waitlist; }
    }


    private void showDeleteConfirmationDialog(String eventId, View card, String eventTitle) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete \"" + eventTitle + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent(eventId, card))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteEvent(String eventId, View card) {
        db.collection("events").document(eventId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    eventsContainer.removeView(card);
                    Toast.makeText(getContext(), "Event deleted", Toast.LENGTH_SHORT).show();
                    Log.d("Firestore", "Deleted event: " + eventId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete event", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error deleting event", e);
                });
    }
}
