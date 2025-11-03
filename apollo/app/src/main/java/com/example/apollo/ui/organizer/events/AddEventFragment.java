package com.example.apollo.ui.organizer.events;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.apollo.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddEventFragment extends Fragment {

    private TextInputEditText eventTitle, eventDescription, eventDate, eventTime, eventLocation,
            eventCapacity, eventPrice, waitlistCapacity, registrationOpen, registrationClose;
    private Button buttonAM, buttonPM, buttonSaveEvent;
    private String ampm = "";
    private FirebaseFirestore db;
    private String eventId = null; // Used for edit mode

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_event, container, false);

        db = FirebaseFirestore.getInstance();

        // Initialize views
        eventTitle = view.findViewById(R.id.eventTitle);
        eventDescription = view.findViewById(R.id.eventDescription);
        eventLocation = view.findViewById(R.id.eventLocation);
        eventDate = view.findViewById(R.id.eventDate);
        eventTime = view.findViewById(R.id.eventTime);
        eventCapacity = view.findViewById(R.id.eventCapacity);
        eventPrice = view.findViewById(R.id.eventPrice);
        waitlistCapacity = view.findViewById(R.id.waitlistCapacity);
        registrationOpen = view.findViewById(R.id.registrationOpen);
        registrationClose = view.findViewById(R.id.registrationClose);
        buttonAM = view.findViewById(R.id.buttonAM);
        buttonPM = view.findViewById(R.id.buttonPM);
        buttonSaveEvent = view.findViewById(R.id.buttonSaveEvent);

        // Check if eventId passed → EDIT MODE
        if (getArguments() != null && getArguments().containsKey("eventId")) {
            eventId = getArguments().getString("eventId");
            buttonSaveEvent.setText("Update Event");
            loadEventDataForEditing(eventId);
        }

        // AM/PM buttons
        buttonAM.setOnClickListener(v -> {
            ampm = "AM";
            buttonAM.setBackgroundColor(Color.parseColor("#FFBB86FC"));
            buttonPM.setBackgroundColor(Color.parseColor("#D3D3D3"));
        });

        buttonPM.setOnClickListener(v -> {
            ampm = "PM";
            buttonPM.setBackgroundColor(Color.parseColor("#FFBB86FC"));
            buttonAM.setBackgroundColor(Color.parseColor("#D3D3D3"));
        });

        // Save/Update button
        buttonSaveEvent.setOnClickListener(v -> {
            if (validateInputs()) {
                if (eventId != null) updateEventInFirestore(eventId);
                else saveEventToFirestore();
            }
        });

        // Back button
        ImageButton backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Changes discarded", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        });

        return view;
    }

    // 🔹 Load data for editing
    private void loadEventDataForEditing(String eventId) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        eventRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                eventTitle.setText(document.getString("title"));
                eventDescription.setText(document.getString("description"));
                eventDate.setText(document.getString("date"));
                eventTime.setText(document.getString("time").replaceAll("(AM|PM)", "").trim());

                String timeValue = document.getString("time");
                if (timeValue != null && timeValue.contains("PM")) {
                    ampm = "PM";
                    buttonPM.setBackgroundColor(Color.parseColor("#FFBB86FC"));
                    buttonAM.setBackgroundColor(Color.parseColor("#D3D3D3"));
                } else {
                    ampm = "AM";
                    buttonAM.setBackgroundColor(Color.parseColor("#FFBB86FC"));
                    buttonPM.setBackgroundColor(Color.parseColor("#D3D3D3"));
                }

                if (document.contains("eventCapacity"))
                    eventCapacity.setText(String.valueOf(document.getLong("eventCapacity")));

                if (document.contains("price"))
                    eventPrice.setText(String.valueOf(document.getDouble("price")));

                if (document.contains("waitlistCapacity"))
                    waitlistCapacity.setText(String.valueOf(document.getLong("waitlistCapacity")));

                registrationOpen.setText(document.getString("registrationOpen"));
                registrationClose.setText(document.getString("registrationClose"));
            }
        }).addOnFailureListener(e -> Log.e("Firestore", "Error loading event for edit", e));
    }

    private boolean validateInputs() {
        // Title
        if (eventTitle.getText().toString().trim().isEmpty()) {
            eventTitle.setError("Required");
            return false;
        }

        // Description
        if (eventDescription.getText().toString().trim().isEmpty()) {
            eventDescription.setError("Required");
            return false;
        }

        // Date and Time
        if (eventDate.getText().toString().trim().isEmpty() ||
                eventTime.getText().toString().trim().isEmpty() ||
                ampm.isEmpty()) {
            Toast.makeText(getContext(), "Enter event date, time, and AM/PM", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Capacity, Price, and Waitlist
        try {
            int capacity = Integer.parseInt(eventCapacity.getText().toString().trim());
            if (capacity <= 0) {
                Toast.makeText(getContext(), "Capacity must be greater than 0", Toast.LENGTH_SHORT).show();
                return false;
            }

            double price = Double.parseDouble(eventPrice.getText().toString().trim());
            if (price < 0) {
                Toast.makeText(getContext(), "Price cannot be negative", Toast.LENGTH_SHORT).show();
                return false;
            }

            int waitlist = Integer.parseInt(waitlistCapacity.getText().toString().trim());
            if (waitlist < 0) {
                Toast.makeText(getContext(), "Waitlist cannot be negative", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Registration Open/Close Dates
        if (registrationOpen.getText().toString().trim().isEmpty() ||
                registrationClose.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Enter registration open and close dates", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Date Logic Validation
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US);
        try {
            String eventDateTimeStr = eventDate.getText().toString().trim() + " " +
                    eventTime.getText().toString().trim() + " " + ampm;
            Date eventDateTime = dateFormat.parse(eventDateTimeStr);

            Date regOpen = new SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    .parse(registrationOpen.getText().toString().trim());
            Date regClose = new SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    .parse(registrationClose.getText().toString().trim());

            Date now = new Date();

            if (eventDateTime.before(now)) {
                Toast.makeText(getContext(), "Event date/time cannot be in the past", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (regOpen.after(regClose)) {
                Toast.makeText(getContext(), "Registration open date must be before close date", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (regClose.after(eventDateTime)) {
                Toast.makeText(getContext(), "Registration close must be before event date", Toast.LENGTH_SHORT).show();
                return false;
            }

        } catch (ParseException e) {
            Toast.makeText(getContext(), "Invalid date format", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // 🔹 Add new event
    private void saveEventToFirestore() {
        Map<String, Object> event = buildEventMap();
        db.collection("events")
                .add(event)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "Event added successfully!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // 🔹 Update existing event
    private void updateEventInFirestore(String eventId) {
        Map<String, Object> event = buildEventMap();
        db.collection("events").document(eventId)
                .update(event)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Event updated successfully!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // 🔹 Helper to build map for Firestore
    private Map<String, Object> buildEventMap() {
        Map<String, Object> event = new HashMap<>();
        event.put("title", eventTitle.getText().toString().trim());
        event.put("description", eventDescription.getText().toString().trim());
        event.put("location", eventLocation.getText().toString().trim()); // <-- add this
        event.put("date", eventDate.getText().toString().trim());
        event.put("time", eventTime.getText().toString().trim() + " " + ampm);
        event.put("eventCapacity", Integer.parseInt(eventCapacity.getText().toString().trim()));
        event.put("price", Double.parseDouble(eventPrice.getText().toString().trim()));
        event.put("waitlistCapacity", Integer.parseInt(waitlistCapacity.getText().toString().trim()));
        event.put("registrationOpen", registrationOpen.getText().toString().trim());
        event.put("registrationClose", registrationClose.getText().toString().trim());
        event.put("updatedAt", new Date());
        return event;
    }
}
