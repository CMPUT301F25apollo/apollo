package com.example.apollo.ui.organizer.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.apollo.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * OrganizerEventDetailsFragment.java
 *
 * Purpose:
 * Displays detailed information for a specific event created by an organizer.
 * Provides options to edit the event, send a lottery, and view participant lists.
 *
 * Design Pattern:
 * - Implements the Controller role in the MVC pattern.
 * - Interacts with Firestore (Model) to fetch and display event details in the View.
 * - Uses Android Navigation for screen transitions.
 *
 * Outstanding Issues / TODOs:
 * - Implement "Send Lottery" functionality to automate or notify entrants.
 * - Add a confirmation prompt before navigation or destructive actions.
 * - Consider improving UI error handling for missing event fields.
 */
public class OrganizerEventDetailsFragment extends Fragment {

    private FirebaseFirestore db;
    private TextView textEventTitle, textEventDescription, textEventSummary;
    private Button buttonEditEvent, buttonSendLottery, buttonViewParticipants;
    private ImageView eventPosterImage;
    private String eventId;
    private String eventName = "Event";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_organizer_event_details, container, false);

        db = FirebaseFirestore.getInstance();

        // Initialize views
        textEventTitle = view.findViewById(R.id.textEventTitle);
        textEventDescription = view.findViewById(R.id.textEventDescription);
        textEventSummary = view.findViewById(R.id.textEventSummary);
        buttonEditEvent = view.findViewById(R.id.buttonEditEvent);
        buttonSendLottery = view.findViewById(R.id.buttonSendLottery);
        buttonViewParticipants = view.findViewById(R.id.buttonViewParticipants);
        eventPosterImage = view.findViewById(R.id.eventPosterImage);

        // Get eventId passed from navigation
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            loadEventDetails(eventId);
        }

        // Back button → returns to event list
        ImageButton backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_navigation_organizer_event_details_to_navigation_organizer_events);
        });

        // Edit event button → navigates to AddEvent screen
        buttonEditEvent.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_organizer_add_event, bundle);
        });

        // Send lottery button (placeholder)
        buttonSendLottery.setOnClickListener(v ->
                Log.d("Organizer", "Send Lottery clicked for event " + eventId));

        // View participants button → navigates to waitlist fragment
        buttonViewParticipants.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_event_waitlist, bundle);
        });

        return view;
    }

    /**
     * Loads event details from Firestore using the event ID and displays them in the UI.
     *
     * @param eventId The Firestore document ID of the event to load.
     */
    private void loadEventDetails(String eventId) {
        if (eventId == null) return;

        DocumentReference eventRef = db.collection("events").document(eventId);
        eventRef.get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Extract event data
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

                        // Log the URL for debugging
                        Log.d("OrganizerEvent", "Poster URL: " + posterUrl);

                        // Load event poster image using Glide
                        if (posterUrl != null && !posterUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(posterUrl)
                                    .into(eventPosterImage);
                        }

                        // Build readable text
                        String registrationPeriod = (registrationOpen != null && registrationClose != null)
                                ? registrationOpen + " - " + registrationClose
                                : "Not specified";
                        String capacityText = (eventCapacity != null)
                                ? "Capacity: " + eventCapacity
                                : "Capacity: N/A";
                        String waitlistText = (waitlistCapacity != null)
                                ? "Waitlist: " + waitlistCapacity
                                : "Waitlist: N/A";
                        String dateText = (date != null) ? date : "N/A";
                        String timeText = (time != null) ? time : "N/A";
                        String priceText = (price != null) ? "$" + price : "Free";
                        String locationText = (location != null) ? location : "TBD";

                        // Update text views
                        textEventTitle.setText(title != null ? title : "Untitled Event");
                        textEventDescription.setText(description != null ? description : "No description available");
                        textEventSummary.setText(
                                "Location: " + locationText + "\n" +
                                        "Date: " + dateText + "\n" +
                                        "Time: " + timeText + "\n" +
                                        "Price: " + priceText + "\n" +
                                        "Registration: " + registrationPeriod + "\n" +
                                        capacityText + "\n" +
                                        waitlistText
                        );
                    } else {
                        Log.w("Firestore", "No such event found with ID: " + eventId);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error loading event details", e));
    }
}
