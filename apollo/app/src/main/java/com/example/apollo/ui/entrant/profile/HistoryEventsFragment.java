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
 * HistoryEventsFragment.java
 *
 * Displays a list of *past* events that the user interacted with — including
 * events they registered for, were invited to, or joined the waitlist for.
 *
 * Flow:
 * - Collect all event IDs linked to the user (registrations, invites, waitlist)
 * - Fetch the full event documents from Firestore
 * - Show only events whose date has already passed
 */
public class HistoryEventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventsAdapter adapter;
    private List<Event> events = new ArrayList<>();
    private FirebaseFirestore db;

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

        adapter.setOnEventClickListener(event -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getId());

            NavHostFragment.findNavController(this)
                    .navigate(R.id.navigation_event_details, bundle);
        });

        loadHistoryEvents();
    }

    /**
     * Collects every event the user interacted with:
     * - registrations/
     * - invites/
     * - waitlist/
     * Then deduplicates the IDs and loads those events.
     */
    private void loadHistoryEvents() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        List<String> interactedEventIds = new ArrayList<>();

        // Registrations
        db.collection("registrations").document(uid).get()
                .addOnSuccessListener(regSnap -> {

                    if (regSnap.exists())
                        interactedEventIds.addAll(regSnap.getData().keySet());

                    // Invites
                    db.collection("invites").document(uid).get()
                            .addOnSuccessListener(invSnap -> {

                                if (invSnap.exists())
                                    interactedEventIds.addAll(invSnap.getData().keySet());

                                // Waitlist
                                db.collection("waitlist").document(uid).get()
                                        .addOnSuccessListener(waitSnap -> {

                                            if (waitSnap.exists())
                                                interactedEventIds.addAll(waitSnap.getData().keySet());

                                            // Remove duplicates
                                            List<String> uniqueIds =
                                                    new ArrayList<>(new java.util.HashSet<>(interactedEventIds));

                                            if (uniqueIds.isEmpty()) {
                                                events.clear();
                                                adapter.notifyDataSetChanged();
                                                return;
                                            }

                                            fetchPastInteractedEvents(uniqueIds);
                                        });
                            });
                });
    }

    /**
     * Fetches event documents for the given IDs,
     * keeps only those whose date has already passed.
     */
    private void fetchPastInteractedEvents(List<String> eventIds) {
        events.clear();

        for (String eventId : eventIds) {
            db.collection("events").document(eventId).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) return;

                        Event event = doc.toObject(Event.class);
                        if (event == null || event.getDate() == null) return;

                        event.setId(doc.getId());

                        if (isPast(event.getDate())) {
                            events.add(event);
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    /**
     * Checks whether the given event date is before today's date.
     *
     * @param dateStr Date in MM/dd/yyyy format.
     * @return true if the event happened in the past.
     */
    private boolean isPast(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            Date eventDate = sdf.parse(dateStr);
            return eventDate.before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
