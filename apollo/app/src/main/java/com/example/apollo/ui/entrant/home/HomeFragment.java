/**
 * HomeFragment.java
 *
 * This fragment is the main screen that shows all available events.
 * It loads events from Firestore, lets users open the details for each event,
 * and includes filters to toggle between open and closed events and also dims past events
 * that are now closed
 *
 * The screen also includes an info button that explains how the lottery
 * selection process works (shown in a popup dialog)
 *
 * Design pattern: Basic MVC, Firestore acts as the data (Model), this fragment
 * handles the view and user logic (Controller/View), it also uses the Fragment
 * Result API to pass data between HomeFragment and FilterFragment.
 *
 * Current Issues
 * - No loading indicator when fetching events from Firestore
 * - Event sorting and pagination not yet implemented
 * - Error handling for missing or invalid event data could be improved
 */
package com.example.apollo.ui.entrant.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;



public class HomeFragment extends Fragment {

    //keeps track of whether open events should be shown in the list
    private boolean showOpen = true;

    // keeps track of whether closed events should be shown in the list
    private boolean showClosed = true;

    //Firestore database instance for fetching event data
    private FirebaseFirestore db;

    //the layout container that holds dynamically added event cards. */
    private LinearLayout eventsContainer;

    //List of all events currently fetched from Firestore. */
    private final List<Event> allEvents = new ArrayList<>();

    /**
     * Called to create the fragment's view hierarchy.
     * Initializes Firestore, sets up button listeners, and loads event data.
     *
     * @param inflater  The LayoutInflater used to inflate the layout.
     * @param container The parent ViewGroup to which the fragment's view will be attached.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The root View for this fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Bind views
        ImageButton infoButton = view.findViewById(R.id.buttonInfo);

        //how it works desc.
        infoButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("How it works?")
                    .setMessage(
                            "Events use a lottery system to keep things fair:\n\n" +
                                    "• Join the waiting list before registration closes.\n" +
                                    "• When the deadline hits, entrants are randomly selected for available spots.\n" +
                                    "• If you’re chosen, you’ll get a notification to confirm.\n" +
                                    "• If someone declines or times out, the system picks the next person on the waitlist."
                    )
                    .setPositiveButton("Got it", null)
                    .show();
        });

        ImageButton filterButton = view.findViewById(R.id.buttonFilter);
        eventsContainer = view.findViewById(R.id.eventsContainer);

        // Navigate to FilterFragment when filter icon is clicked
        filterButton.setOnClickListener(v -> {
            //pass current filter states to FilterFragment
            Bundle args = new Bundle();
            args.putBoolean("open", showOpen);
            args.putBoolean("closed", showClosed);

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_navigation_home_to_navigation_filter, args);
        });

        //listen for filter results once
        getParentFragmentManager().setFragmentResultListener("filters", this, (reqKey, bundle) -> {
            showOpen = bundle.getBoolean("open");
            showClosed = bundle.getBoolean("closed");
            filterEvents(showOpen, showClosed);
        });

        ImageButton qrButton = view.findViewById(R.id.buttonQrScanner);

        qrButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_navigation_home_to_qrScannerFragment);
        });

        // Load events from Firestore
        loadEventsFromFirestore();

        return view;
    }



    /**
     * Fetches event documents from Firestore and dynamically populates
     * the event list on the Home screen
     * Each event card is clickable leading to its detailed view.
     */
    private void loadEventsFromFirestore() {
        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    eventsContainer.removeAllViews();
                    allEvents.clear();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String eventId = document.getId();
                        String title = document.getString("title");
                        String location = document.getString("location");
                        String openDateStr = document.getString("registrationOpen");
                        String closeDateStr = document.getString("registrationClose");
                        String posterUrl = document.getString("eventPosterUrl");

                        boolean isClosed = false;
                        boolean isOpen = false;

                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                            Date today = new Date();

                            Date openDate = openDateStr != null ? sdf.parse(openDateStr) : null;
                            Date closeDate = closeDateStr != null ? sdf.parse(closeDateStr) : null;

                            if (closeDate != null && closeDate.before(today)) {
                                // past event = closed
                                isClosed = true;
                                isOpen = false;
                            } else if (openDate != null && openDate.after(today)) {
                                // not started yet → treat as open
                                isClosed = false;
                                isOpen = true;
                            } else {
                                // ongoing or missing dates → treat as open (still available)
                                isClosed = false;
                                isOpen = true;
                            }
                        } catch (Exception e) {
                            Log.w("DateParse", "Failed to parse registration dates", e);
                            // if parsing fails, assume event is open
                            isOpen = true;
                        }

                        View card = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_event_card, eventsContainer, false);

                        TextView titleView = card.findViewById(R.id.eventTitle);
                        titleView.setText(title != null ? title : "Untitled Event");

                        ImageView posterView = card.findViewById(R.id.eventPosterImage);

                        if (posterUrl != null && !posterUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(posterUrl)
                                    .into(posterView);
                        }

                        // dim past (closed) events
                        card.setAlpha(isClosed ? 0.4f : 1.0f);

                        final String finalEventId = eventId;
                        final boolean finalIsClosed = isClosed;

                        card.setOnClickListener(v -> {
                            Bundle bundle = new Bundle();
                            bundle.putString("eventId", finalEventId);
                            bundle.putBoolean("isClosed", finalIsClosed); // 👈 pass it here
                            NavController navController = NavHostFragment.findNavController(this);
                            navController.navigate(R.id.action_navigation_home_to_navigation_event_details, bundle);
                        });

                        eventsContainer.addView(card);
                        allEvents.add(new Event(title, location, isOpen, isClosed, card));
                    }

                    // always reapply the current filter after loading
                    filterEvents(showOpen, showClosed);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading events", e));
    }

    /**
     * Filters events based on whether open and/or closed events should be shown.
     * If both filters are off, all events are displayed.
     *
     * @param showOpen   True to show open events.
     * @param showClosed True to show closed events.
     */
    private void filterEvents(boolean showOpen, boolean showClosed) {
        // if both are false, show all
        boolean showAll = !showOpen && !showClosed;

        for (Event e : allEvents) {
            boolean show = false;

            if (showAll) {
                show = true;
            } else {
                if (showOpen && e.isOpen()) show = true;
                if (showClosed && e.isClosed()) show = true;
            }

            e.getView().setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }


    /**
     * Represents an event object on the Home screen,
     * storing its open/closed state and view reference.
     */
    private static class Event {
        private final boolean isOpen;
        private final boolean isClosed;
        private final View view;

        public Event(String title, String location, boolean isOpen, boolean isClosed, View view) {
            this.isOpen = isOpen;
            this.isClosed = isClosed;
            this.view = view;
        }

        public boolean isOpen() { return isOpen; }
        public boolean isClosed() { return isClosed; }
        public View getView() { return view; }
    }
}