package com.example.apollo.ui.entrant.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.apollo.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private boolean showOpen = true;
    private boolean showClosed = true;

    private FirebaseFirestore db;
    private LinearLayout eventsContainer;
    private final List<Event> allEvents = new ArrayList<>();

    private String titleKeyword = "";
    private String locationKeyword = "";
    private String dateFilter = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Bind views
        ImageButton infoButton = view.findViewById(R.id.buttonInfo);
        ImageButton filterButton = view.findViewById(R.id.buttonFilter);
        ImageButton qrButton = view.findViewById(R.id.buttonQrScanner);
        eventsContainer = view.findViewById(R.id.eventsContainer);

        // Info dialog
        infoButton.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("How it works?")
                .setMessage(
                        "Events use a lottery system to keep things fair:\n\n" +
                                "• Join the waiting list before registration closes.\n" +
                                "• When the deadline hits, entrants are randomly selected for available spots.\n" +
                                "• If you’re chosen, you’ll get a notification to confirm.\n" +
                                "• If someone declines or times out, the system picks the next person on the waitlist."
                )
                .setPositiveButton("Got it", null)
                .show()
        );

        // Navigate to FilterFragment
        filterButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("open", showOpen);
            args.putBoolean("closed", showClosed);
            args.putString("titleKeyword", titleKeyword);
            args.putString("locationKeyword", locationKeyword);
            args.putString("date", dateFilter);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_navigation_home_to_navigation_filter, args);
        });

        // Listen for filter results
        getParentFragmentManager().setFragmentResultListener("filters", this, (reqKey, bundle) -> {
            showOpen = bundle.getBoolean("open");
            showClosed = bundle.getBoolean("closed");

            titleKeyword = bundle.getString("titleKeyword", "").toLowerCase();
            locationKeyword = bundle.getString("locationKeyword", "").toLowerCase();
            dateFilter = bundle.getString("date", "");

            filterEvents();
        });

        // Navigate to QR Scanner
        qrButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_navigation_home_to_qrScannerFragment);
        });

        // Load events from Firestore
        loadEventsFromFirestore();

        return view;
    }

    private void loadEventsFromFirestore() {
        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    eventsContainer.removeAllViews();
                    allEvents.clear();

                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                    Date today = null;
                    try {
                        today = sdf.parse(sdf.format(new Date()));
                    } catch (Exception e) {
                        today = new Date();
                    }

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String eventId = document.getId();
                        String title = document.getString("title");
                        String location = document.getString("location");
                        String openDateStr = document.getString("registrationOpen");
                        String closeDateStr = document.getString("registrationClose");
                        String posterUrl = document.getString("eventPosterUrl");

                        boolean isOpen = true;
                        boolean isClosed = false;
                        Date openDate = null;
                        Date closeDate = null;

                        try {
                            openDate = openDateStr != null ? sdf.parse(openDateStr) : null;
                            closeDate = closeDateStr != null ? sdf.parse(closeDateStr) : null;

                            if (closeDate != null && closeDate.before(today)) {
                                isClosed = true;
                                isOpen = false;
                            } else if (openDate != null && openDate.after(today)) {
                                isOpen = true;
                                isClosed = false;
                            }
                        } catch (Exception e) {
                            Log.w("DateParse", "Failed to parse registration dates", e);
                        }

                        View card = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_event_card, eventsContainer, false);

                        TextView titleView = card.findViewById(R.id.eventTitle);
                        titleView.setText(title != null ? title : "Untitled Event");

                        ImageView posterView = card.findViewById(R.id.eventPosterImage);
                        if (posterUrl != null && !posterUrl.isEmpty()) {
                            Glide.with(this).load(posterUrl).into(posterView);
                        }

                        card.setAlpha(isClosed ? 0.4f : 1.0f);

                        card.setOnClickListener(v -> {
                            Bundle bundle = new Bundle();
                            bundle.putString("eventId", eventId);
                            NavHostFragment.findNavController(this)
                                    .navigate(R.id.action_navigation_home_to_navigation_event_details, bundle);
                        });

                        eventsContainer.addView(card);

                        allEvents.add(new Event(title, location, isOpen, isClosed, card, openDate, closeDate));
                    }

                    // Apply current filters
                    filterEvents();
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading events", e));
    }

    private void filterEvents() {
        boolean showAll = !showOpen && !showClosed;

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        Date filterDate = null;
        if (!dateFilter.isEmpty()) {
            try { filterDate = sdf.parse(dateFilter); } catch (Exception ignored) {}
        }

        for (Event e : allEvents) {
            boolean show = false;

            if (showAll) show = true;
            else {
                if (showOpen && e.isOpen()) show = true;
                if (showClosed && e.isClosed()) show = true;
            }

            if (show && !titleKeyword.isEmpty() && !e.getTitle().toLowerCase().contains(titleKeyword)) show = false;
            if (show && !locationKeyword.isEmpty() && !e.getLocation().toLowerCase().contains(locationKeyword)) show = false;

            if (show && filterDate != null && !e.isAvailableOn(filterDate)) show = false;

            e.getView().setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private static class Event {
        private final String title;
        private final String location;
        private final boolean isOpen;
        private final boolean isClosed;
        private final View view;
        private final Date registrationOpen;
        private final Date registrationClose;

        public Event(String title, String location, boolean isOpen, boolean isClosed, View view,
                     Date registrationOpen, Date registrationClose) {
            this.title = title;
            this.location = location;
            this.isOpen = isOpen;
            this.isClosed = isClosed;
            this.view = view;
            this.registrationOpen = registrationOpen;
            this.registrationClose = registrationClose;
        }

        public boolean isOpen() { return isOpen; }
        public boolean isClosed() { return isClosed; }
        public View getView() { return view; }
        public String getTitle() { return title; }
        public String getLocation() { return location; }

        public boolean isAvailableOn(Date date) {
            if (date == null) return true;
            if (registrationOpen != null && date.before(registrationOpen)) return false;
            if (registrationClose != null && date.after(registrationClose)) return false;
            return true;
        }
    }
}
