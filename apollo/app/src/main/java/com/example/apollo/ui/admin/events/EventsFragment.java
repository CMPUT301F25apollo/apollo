/**
 * EventsFragment.java
 *
 * This fragment shows a list of all events for admin users.
 * It loads events from Firestore, displays them as cards, supports
 * simple text search, and allows admins to view details or delete events.
 */
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
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that lists all events for admins.
 * Events are fetched from Firestore and shown as cards with basic details.
 * Admins can search by title, tap a card to see full details, or delete an event.
 */
public class EventsFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout eventsContainer;
    private final List<Event> allEvents = new ArrayList<>();
    private TextView searchInput;

    /**
     * Inflates the admin events layout, initializes UI components,
     * and starts loading events from Firestore. Also attaches a text
     * listener to filter events as the user types in the search box.
     *
     * @param inflater  LayoutInflater used to inflate the UI.
     * @param container Parent view group for this fragment.
     * @param savedInstanceState Previously saved state (not used here).
     * @return The root view for this fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events_admin, container, false);

        db = FirebaseFirestore.getInstance();
        eventsContainer = view.findViewById(R.id.eventsContainer);
        searchInput = view.findViewById(R.id.search_events_input);

        loadEventsFromFirestore();

        // Filter the list as the admin types in the search bar
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

    /**
     * Loads all events from the "events" collection in Firestore.
     * For each event, it also loads the waitlist size and stores
     * the result in {@code allEvents}, then refreshes the filtered list.
     */
    private void loadEventsFromFirestore() {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allEvents.clear();
                    List<Task<QuerySnapshot>> waitlistTasks = new ArrayList<>();
                    List<Event> unprocessedEvents = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String eventId = document.getId();
                        String title = document.getString("title");
                        String posterUrl = document.getString("eventPosterUrl");
                        String location = document.getString("location");
                        Long capacity = document.getLong("eventCapacity");
                        Long waitlistCapacity = document.getLong("waitlistCapacity");

                        Event event = new Event(eventId, title, posterUrl, location, capacity, waitlistCapacity, 0);
                        unprocessedEvents.add(event);

                        Task<QuerySnapshot> waitlistTask = db.collection("events")
                                .document(eventId)
                                .collection("waitlist")
                                .get();
                        waitlistTasks.add(waitlistTask);
                    }

                    Tasks.whenAllSuccess(waitlistTasks).addOnSuccessListener(waitlistSnapshots -> {
                        for (int i = 0; i < waitlistSnapshots.size(); i++) {
                            int waitlistCount = ((QuerySnapshot) waitlistSnapshots.get(i)).size();
                            unprocessedEvents.get(i).setWaitlistCount(waitlistCount);
                        }
                        allEvents.addAll(unprocessedEvents);
                        if (isAdded() && getContext() != null) {
                            filterEvents(searchInput.getText().toString());
                        }
                    });

                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading events", e));
    }

    /**
     * Filters the in-memory list of events by title and rebuilds the
     * cards shown in the container. Matches are case-insensitive.
     *
     * @param query The search text typed by the admin.
     */
    private void filterEvents(String query) {
        eventsContainer.removeAllViews();
        String lowerQuery = query.toLowerCase();

        for (Event event : allEvents) {
            if (event.getTitle() != null && event.getTitle().toLowerCase().contains(lowerQuery)) {
                View card = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_event_card_admin, eventsContainer, false);

                TextView titleView = card.findViewById(R.id.eventTitle);
                ImageView posterView = card.findViewById(R.id.eventPosterImage);
                ImageView deleteButton = card.findViewById(R.id.delete_button);
                TextView locationView = card.findViewById(R.id.eventLocation);
                TextView capacityView = card.findViewById(R.id.eventCapacity);
                TextView waitlistView = card.findViewById(R.id.eventWaitlist);

                locationView.setText("Location: " + (event.getLocation() != null ? event.getLocation() : "N/A"));
                capacityView.setText("Event Capacity: " + (event.getCapacity() != null ? event.getCapacity() : "0"));
                waitlistView.setText("Waitlist: " + event.getWaitlistCount() + "/" + (event.getWaitlist() != null ? event.getWaitlist() : "0"));

                titleView.setText(event.getTitle());

                if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                    Glide.with(getContext())
                            .load(event.getPosterUrl())
                            .into(posterView);
                }

                card.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("eventId", event.getId());
                    NavController navController = NavHostFragment.findNavController(EventsFragment.this);
                    navController.navigate(R.id.action_navigation_events_to_navigation_event_details, bundle);
                });

                deleteButton.setOnClickListener(v ->
                        showDeleteConfirmationDialog(event.getId(), card, event.getTitle()));

                eventsContainer.addView(card);
            }
        }
    }

    /**
     * Simple model class representing an event item in the admin list.
     * Holds basic display information and waitlist stats.
     */
    private static class Event {
        private final String id;
        private final String title;
        private final String posterUrl;
        private final String location;
        private final Long capacity;
        private final Long waitlist;
        private int waitlistCount;

        /**
         * Creates a new Event model used by the admin list.
         *
         * @param id            Firestore document ID of the event.
         * @param title         Title of the event.
         * @param posterUrl     URL for the event poster image.
         * @param location      Location of the event.
         * @param capacity      Maximum number of attendees.
         * @param waitlist      Maximum waitlist size.
         * @param waitlistCount Current number of people on the waitlist.
         */
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

        /** @return The Firestore ID of this event. */
        public String getId() { return id; }

        /** @return The title of this event. */
        public String getTitle() { return title; }

        /** @return URL for the event poster image, or null if not set. */
        public String getPosterUrl() { return posterUrl; }

        /** @return The location string for this event. */
        public String getLocation() { return location; }

        /** @return The event capacity (max attendees). */
        public Long getCapacity() { return capacity; }

        /** @return The maximum size of the waitlist. */
        public Long getWaitlist() { return waitlist; }
        public int getWaitlistCount() { return waitlistCount; }
        public void setWaitlistCount(int waitlistCount) { this.waitlistCount = waitlistCount; }
    }

    /**
     * Shows a confirmation dialog before deleting an event.
     *
     * @param eventId    ID of the event to delete.
     * @param card       The view representing the event card in the list.
     * @param eventTitle Title of the event, used in the dialog message.
     */
    private void showDeleteConfirmationDialog(String eventId, View card, String eventTitle) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete \"" + eventTitle + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent(eventId, card))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes an event document from Firestore and removes its card from the UI
     * if the delete succeeds. Logs and shows a toast for both success and failure.
     *
     * @param eventId ID of the event to delete.
     * @param card    The card view to remove from the container on success.
     */
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
