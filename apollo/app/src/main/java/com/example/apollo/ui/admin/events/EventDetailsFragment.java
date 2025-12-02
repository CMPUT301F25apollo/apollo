/**
 * EventDetailsFragment.java
 *
 * This fragment allows admin users to view all details of a selected event.
 * It pulls event information from Firestore including title, description,
 * date/time, registration window, capacity values, and the event poster.
 * The fragment also loads and displays the current waitlist count.
 *
 * This is a read-only screen for admins to verify event data and understand
 * how many entrants are currently registered or waiting.
 */
package com.example.apollo.ui.admin.events;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.apollo.R;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Fragment that displays detailed information about a single event for admins.
 * It retrieves event data and waitlist size from Firestore and updates the UI
 * with all relevant fields and the event poster if available.
 */
public class EventDetailsFragment extends Fragment {

    private FirebaseFirestore db;
    private ImageView posterImage;
    private TextView titleText, descriptionText, waitlistText, summaryText;
    private String eventId;

    /**
     * Inflates the event details layout and initializes all UI components.
     * If an event ID is passed through arguments, this method begins loading
     * the event information from Firestore.
     *
     * @param inflater  LayoutInflater used to inflate the fragment layout.
     * @param container Parent layout container.
     * @param savedInstanceState Previously saved state (not used here).
     * @return The inflated root view for this fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_event_details_admin, container, false);

        db = FirebaseFirestore.getInstance();

        posterImage = view.findViewById(R.id.eventPosterImage);
        titleText = view.findViewById(R.id.textEventTitle);
        descriptionText = view.findViewById(R.id.textEventDescription);
        waitlistText = view.findViewById(R.id.textWaitlistCount);
        summaryText = view.findViewById(R.id.textEventSummary);

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            if (eventId != null) {
                loadEventDetails(eventId);
            } else {
                Toast.makeText(getContext(), "Event ID is missing", Toast.LENGTH_SHORT).show();
            }
        }

        return view;
    }

    /**
     * Retrieves event details and waitlist size from Firestore. Once the data
     * is loaded, this method fills the UI with the event title, description,
     * date/time information, registration window, capacities, and poster image.
     *
     * @param eventId The Firestore ID of the event document to load.
     */
    private void loadEventDetails(String eventId) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {

                        String title = document.getString("title");
                        String description = document.getString("description");
                        String location = document.getString("location");
                        String date = document.getString("date");
                        String time = document.getString("time");
                        String registrationOpen = document.getString("registrationOpen");
                        String registrationClose = document.getString("registrationClose");
                        Long eventCapacity = document.getLong("eventCapacity");
                        Long waitlistCapacity = document.getLong("waitlistCapacity");
                        Double price = document.getDouble("price");
                        String posterUrl = document.getString("eventPosterUrl");

                        // Waitlist fetch
                        db.collection("events").document(eventId)
                                .collection("waitlist")
                                .get()
                                .addOnSuccessListener(waitlistSnapshot -> {

                                    int waitlistCount = waitlistSnapshot.size();

                                    titleText.setText(title != null ? title : "No Title");
                                    descriptionText.setText(description != null ? description : "No Description");

                                    summaryText.setText(
                                            " Location: " + (location != null ? location : "N/A") +
                                                    "\n Date: " + (date != null ? date : "N/A") +
                                                    "\n Time: " + (time != null ? time : "N/A") +
                                                    "\n Price: $" + (price != null ? price : 0) +
                                                    "\n Registration Opens: " + (registrationOpen != null ? registrationOpen : "N/A") +
                                                    "\n Registration Closes: " + (registrationClose != null ? registrationClose : "N/A") +
                                                    "\n Event Capacity: " + (eventCapacity != null ? eventCapacity : 0) +
                                                    "\n Waitlist Capacity: " + (waitlistCapacity != null ? waitlistCapacity : 0)
                                    );

                                    waitlistText.setText(
                                            "Waitlist: " + waitlistCount + "/" +
                                                    (waitlistCapacity != null ? waitlistCapacity : 0)
                                    );

                                    // Poster image
                                    if (posterUrl != null && !posterUrl.isEmpty()) {
                                        Glide.with(getContext())
                                                .load(posterUrl)
                                                .into(posterImage);
                                    }
                                });

                    } else {
                        Toast.makeText(getContext(), "Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load event", Toast.LENGTH_SHORT).show()
                );
    }
}
