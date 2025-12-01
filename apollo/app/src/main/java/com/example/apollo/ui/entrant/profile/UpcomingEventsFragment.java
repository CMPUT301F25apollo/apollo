package com.example.apollo.ui.entrant.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apollo.R;
import com.example.apollo.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * UpcomingEventsFragment.java
 *
 * Displays a list of upcoming events that the user is *registered* for.
 * This fragment filters events by date (must be in the future) and checks
 * if the user's ID exists in the event's registrations subcollection.
 *
 * Responsibilities:
 * - Query all events from Firestore
 * - Determine which ones occur in the future
 * - Filter to only those the user is registered in
 * - Display them in a RecyclerView using EventsAdapter
 */
public class UpcomingEventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventsAdapter adapter;
    private List<Event> events = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerEvents);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new EventsAdapter(events);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadUpcomingEvents();

        adapter.setOnEventClickListener(event -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getId());

            NavHostFragment.findNavController(this)
                    .navigate(R.id.navigation_event_details, bundle);
        });
    }

    /**
     * Loads all events from Firestore, filters to future dates,
     * then checks if the current user is registered for them.
     */
    private void loadUpcomingEvents() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("events")
                .get()
                .addOnSuccessListener(snapshot -> {
                    events.clear();

                    for (DocumentSnapshot doc : snapshot) {
                        Event event = doc.toObject(Event.class);
                        if (event == null || event.getDate() == null) continue;

                        event.setId(doc.getId());

                        if (!isFuture(event.getDate())) continue;

                        // Check if user is registered
                        doc.getReference()
                                .collection("registrations")
                                .document(uid)
                                .get()
                                .addOnSuccessListener(reg -> {
                                    if (reg.exists()) {
                                        events.add(event);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                    }
                });
    }

    /**
     * Checks if an event date is in the future.
     *
     * @param dateStr A date formatted as MM/dd/yyyy.
     * @return true if the event happens after today.
     */
    private boolean isFuture(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            Date eventDate = sdf.parse(dateStr);
            return eventDate.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
