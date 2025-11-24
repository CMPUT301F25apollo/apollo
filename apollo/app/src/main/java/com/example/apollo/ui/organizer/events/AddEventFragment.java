package com.example.apollo.ui.organizer.events;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.apollo.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * AddEventFragment.java
 *
 * Purpose:
 * Allows organizers to create or edit events. Users can input event details such as title,
 * description, date, time, capacity, registration period, and upload an event image.
 * Handles image uploads to Firebase Storage and event data storage in Firestore.
 *
 * Design Pattern:
 * Acts as a Controller in the MVC pattern, managing interactions between the view (form inputs)
 * and the model (Firestore and Firebase Storage).
 *
 * Notes:
 * - Validates input fields before saving or updating events.
 * - Handles both new event creation and editing existing events.
 * - Displays simple error messages using Toasts for user feedback.
 */
public class AddEventFragment extends Fragment {

    private static final int IMAGE_PICK_REQUEST = 1001;

    private TextInputEditText eventTitle, eventDescription, eventDate, eventTime, eventLocation,
            eventCapacity, eventPrice, waitlistCapacity, registrationOpen, registrationClose;
    private Button buttonAM, buttonPM, buttonSaveEvent, buttonSelectImage, buttonRemoveImage;
    private ImageView eventImagePreview;
    private String ampm = "";
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private Uri selectedImageUri = null;
    private String existingImageUrl = null; // used in edit mode
    private String eventId = null; // indicates edit mode
    private FirebaseAuth mAuth;

    /**
     * Called when the fragment’s view is created.
     * Initializes all UI elements, Firebase instances, and event listeners.
     *
     * @param inflater Used to inflate the fragment layout.
     * @param container The parent view group.
     * @param savedInstanceState The saved instance state, if available.
     * @return The root view of the fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_event, container, false);

        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Initialize view elements
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
        buttonSelectImage = view.findViewById(R.id.buttonSelectImage);
        buttonRemoveImage = view.findViewById(R.id.buttonRemoveImage);
        eventImagePreview = view.findViewById(R.id.eventImagePreview);

        // Check if editing an existing event
        if (getArguments() != null && getArguments().containsKey("eventId")) {
            eventId = getArguments().getString("eventId");
            buttonSaveEvent.setText("Update Event");
            loadEventDataForEditing(eventId);
        }

        // Set listeners for AM/PM buttons
        buttonAM.setOnClickListener(v -> selectAMPM("AM"));
        buttonPM.setOnClickListener(v -> selectAMPM("PM"));

        // Image selection and removal
        buttonSelectImage.setOnClickListener(v -> openImagePicker());
        buttonRemoveImage.setOnClickListener(v -> {
            selectedImageUri = null;
            existingImageUrl = null;
            eventImagePreview.setImageResource(android.R.color.transparent);
            Toast.makeText(getContext(), "Image removed", Toast.LENGTH_SHORT).show();
        });

        // Save or update event
        buttonSaveEvent.setOnClickListener(v -> {
            if (validateInputs()) {
                if (selectedImageUri != null) {
                    uploadImageAndSaveEvent();
                } else {
                    saveEvent(existingImageUrl);
                }
            }
        });

        return view;
    }

    /**
     * Handles AM/PM button styling and value selection.
     *
     * @param selection The selected time period ("AM" or "PM").
     */
    private void selectAMPM(String selection) {
        ampm = selection;
        if ("AM".equals(selection)) {
            buttonAM.setBackgroundColor(getResources().getColor(R.color.purple_200));
            buttonPM.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            buttonPM.setBackgroundColor(getResources().getColor(R.color.purple_200));
            buttonAM.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    /**
     * Opens an image picker for the user to select an event image.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), IMAGE_PICK_REQUEST);
    }

    /**
     * Called when the user selects an image from the picker.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            eventImagePreview.setImageURI(selectedImageUri);
        }
    }

    /**
     * Uploads the selected image to Firebase Storage, then saves or updates the event.
     */
    private void uploadImageAndSaveEvent() {
        if (selectedImageUri == null) return;

        String filename = UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child("event_posters/" + filename);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> saveEvent(uri.toString()))
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), "Failed to get image URL", Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Saves or updates event data in Firestore.
     *
     * @param imageUrl The URL of the uploaded image (if available).
     */
    private void saveEvent(String imageUrl) {
        Map<String, Object> event = buildEventMap();
        if (imageUrl != null) event.put("eventPosterUrl", imageUrl);

        if (eventId != null) {
            // Update existing event
            db.collection("events").document(eventId)
                    .set(event)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Event updated successfully!", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            // Create new event
            db.collection("events")
                    .add(event)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(getContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Loads event details from Firestore when editing an existing event.
     *
     * @param eventId The ID of the event to edit.
     */
    private void loadEventDataForEditing(String eventId) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        eventRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                eventTitle.setText(document.getString("title"));
                eventDescription.setText(document.getString("description"));
                eventLocation.setText(document.getString("location"));
                eventDate.setText(document.getString("date"));

                String timeValue = document.getString("time");
                if (timeValue != null) {
                    eventTime.setText(timeValue.replaceAll("(AM|PM)", "").trim());
                    if (timeValue.contains("PM")) selectAMPM("PM");
                    else selectAMPM("AM");
                }

                if (document.contains("eventCapacity"))
                    eventCapacity.setText(String.valueOf(document.getLong("eventCapacity")));

                if (document.contains("price"))
                    eventPrice.setText(String.valueOf(document.getDouble("price")));

                if (document.contains("waitlistCapacity"))
                    waitlistCapacity.setText(String.valueOf(document.getLong("waitlistCapacity")));

                registrationOpen.setText(document.getString("registrationOpen"));
                registrationClose.setText(document.getString("registrationClose"));

                // Load image if exists
                if (document.contains("eventPosterUrl")) {
                    existingImageUrl = document.getString("eventPosterUrl");
                    Glide.with(requireContext())
                            .load(existingImageUrl)
                            .into(eventImagePreview);
                }
            }
        }).addOnFailureListener(e -> Log.e("Firestore", "Error loading event for edit", e));
    }

    /**
     * Validates all event input fields before saving.
     *
     * @return true if all inputs are valid, false otherwise.
     */
    private boolean validateInputs() {
        if (eventTitle.getText().toString().trim().isEmpty()) {
            eventTitle.setError("Required");
            return false;
        }
        if (eventDescription.getText().toString().trim().isEmpty()) {
            eventDescription.setError("Required");
            return false;
        }
        if (eventLocation.getText().toString().trim().isEmpty()) {
            eventLocation.setError("Required");
            return false;
        }
        if (eventDate.getText().toString().trim().isEmpty() ||
                eventTime.getText().toString().trim().isEmpty() ||
                ampm.isEmpty()) {
            Toast.makeText(getContext(), "Enter event date, time, and AM/PM", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            int capacity = Integer.parseInt(eventCapacity.getText().toString().trim());
            if (capacity <= 0) throw new NumberFormatException();
            double price = Double.parseDouble(eventPrice.getText().toString().trim());
            if (price < 0) throw new NumberFormatException();
            int waitlist = Integer.parseInt(waitlistCapacity.getText().toString().trim());
            if (waitlist < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid numeric value", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (registrationOpen.getText().toString().trim().isEmpty() ||
                registrationClose.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Enter registration dates", Toast.LENGTH_SHORT).show();
            return false;
        }

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
                Toast.makeText(getContext(), "Registration open must be before close", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (regClose.after(eventDateTime)) {
                Toast.makeText(getContext(), "Registration close must be before event date", Toast.LENGTH_SHORT).show();
                return false;
            }
//            if (regOpen.before(now) || regClose.before(now)) {
//                Toast.makeText(getContext(), "Registration dates cannot be in the past", Toast.LENGTH_SHORT).show();
//                return false;
//            }

        } catch (ParseException e) {
            Toast.makeText(getContext(), "Invalid date format", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            String timeStr = eventTime.getText().toString().trim();

            // Expecting format like "10:30"
            String[] parts = timeStr.split(":");
            if (parts.length != 2) {
                Toast.makeText(getContext(), "Invalid time format. Please use hh:mm format.", Toast.LENGTH_SHORT).show();
                return false;
            }

            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            if (hour < 1 || hour > 12) {
                Toast.makeText(getContext(), "Hour must be between 1 and 12 for AM/PM format.", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (minute < 0 || minute >= 60) {
                Toast.makeText(getContext(), "Minutes must be between 00 and 59.", Toast.LENGTH_SHORT).show();
                return false;
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "Invalid time format. Please use hh:mm format.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * Builds a map of event data to be stored in Firestore.
     *
     * @return A map containing all event fields and values.
     */
    private Map<String, Object> buildEventMap() {
        Map<String, Object> event = new HashMap<>();
        event.put("title", eventTitle.getText().toString().trim());
        event.put("description", eventDescription.getText().toString().trim());
        event.put("location", eventLocation.getText().toString().trim());
        event.put("date", eventDate.getText().toString().trim());
        event.put("time", eventTime.getText().toString().trim() + " " + ampm);
        event.put("eventCapacity", Integer.parseInt(eventCapacity.getText().toString().trim()));
        event.put("price", Double.parseDouble(eventPrice.getText().toString().trim()));
        event.put("waitlistCapacity", Integer.parseInt(waitlistCapacity.getText().toString().trim()));
        event.put("registrationOpen", registrationOpen.getText().toString().trim());
        event.put("registrationClose", registrationClose.getText().toString().trim());
        event.put("updatedAt", new Date());

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            event.put("creatorId", currentUser.getUid());
        }

        return event;
    }
}
