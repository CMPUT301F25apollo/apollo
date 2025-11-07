package com.example.apollo.ui.organizer.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    private String eventId;
    // NEW: hold event title for notification text
    private String eventName = "Event";

    /**
     * Inflates the layout for the event details screen, initializes UI elements,
     * and sets up click listeners for navigation and actions.
     *
     * @param inflater  LayoutInflater used to inflate the fragment layout.
     * @param container Parent ViewGroup for the fragment.
     * @param savedInstanceState Bundle containing saved instance state, if any.
     * @return The root view for the fragment layout.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_organizer_event_details, container, false);

        db = FirebaseFirestore.getInstance();


        textEventTitle = view.findViewById(R.id.textEventTitle);
        textEventDescription = view.findViewById(R.id.textEventDescription);
        textEventSummary = view.findViewById(R.id.textEventSummary);
        buttonEditEvent = view.findViewById(R.id.buttonEditEvent);
        buttonSendLottery = view.findViewById(R.id.buttonSendLottery);
        buttonViewParticipants = view.findViewById(R.id.buttonViewParticipants);

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            loadEventDetails(eventId);
        }

        // Handles back navigation to the event list.
        ImageButton backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_navigation_organizer_event_details_to_navigation_organizer_events);
        });

        // Navigates to the AddEventFragment for editing the event.
        buttonEditEvent.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_organizer_add_event, bundle);
        });

        // Logs a message when the "Send Lottery" button is pressed.
        buttonSendLottery.setOnClickListener(v ->
                Log.d("Organizer", "Send Lottery clicked for event " + eventId));

        // Navigates to the event waitlist fragment.
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
        DocumentReference eventRef = db.collection("events").document(eventId);
        eventRef.get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Extract event data fields safely.
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

                        // Construct readable strings.
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

                        // Update the UI with event details.
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
