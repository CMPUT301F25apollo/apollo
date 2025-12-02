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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.apollo.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * AddEventFragment.java
 *
 * Organizer-facing screen used to create a new event or edit an existing one.
 * Allows entry of basic details (title, description, dates, capacity, price),
 * optional poster image upload, category selection, and a geolocation toggle.
 *
 * Behavior:
 * - If an eventId is passed in arguments → load event data and switch to "Update" mode.
 * - Otherwise → create a new event with a generated QR id and default flags.
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
    private FirebaseAuth mAuth;

    private Uri selectedImageUri = null;
    private String existingImageUrl = null;
    private String eventId = null;

    private Switch switchButton;

    // Category checkboxes
    private CheckBox catYoga, catFitness, catKidsSports, catMartialArts, catTennis,
            catAquatics, catAdultSports, catWellness, catCreative, catCamps;

    /**
     * Inflates the add/edit event layout, initializes Firebase references and UI elements,
     * and wires up button actions (AM/PM, image selection, save/update).
     *
     * If an "eventId" argument is present, the fragment loads existing event data for editing.
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

        // Initialize UI
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
        switchButton = view.findViewById(R.id.switchButton);

        // Categories
        catYoga = view.findViewById(R.id.catYoga);
        catFitness = view.findViewById(R.id.catFitness);
        catKidsSports = view.findViewById(R.id.catKidsSports);
        catMartialArts = view.findViewById(R.id.catMartialArts);
        catTennis = view.findViewById(R.id.catTennis);
        catAquatics = view.findViewById(R.id.catAquatics);
        catAdultSports = view.findViewById(R.id.catAdultSports);
        catWellness = view.findViewById(R.id.catWellness);
        catCreative = view.findViewById(R.id.catCreative);
        catCamps = view.findViewById(R.id.catCamps);

        // Editing existing event
        if (getArguments() != null && getArguments().containsKey("eventId")) {
            eventId = getArguments().getString("eventId");
            buttonSaveEvent.setText("Update Event");
            loadEventDataForEditing(eventId);
        }

        // AM/PM buttons
        buttonAM.setOnClickListener(v -> selectAMPM("AM"));
        buttonPM.setOnClickListener(v -> selectAMPM("PM"));

        // Image selection/removal
        buttonSelectImage.setOnClickListener(v -> openImagePicker());
        buttonRemoveImage.setOnClickListener(v -> {
            selectedImageUri = null;
            existingImageUrl = null;
            eventImagePreview.setImageResource(android.R.color.transparent);
        });

        // Save/Update event
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
     * Updates the AM/PM state and toggles the background color
     * of the two time-selection buttons.
     *
     * @param selection "AM" or "PM"
     */
    private void selectAMPM(String selection) {
        ampm = selection;

        if (selection.equals("AM")) {
            buttonAM.setBackgroundColor(getResources().getColor(R.color.purple_200));
            buttonPM.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            buttonPM.setBackgroundColor(getResources().getColor(R.color.purple_200));
            buttonAM.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    /**
     * Launches an implicit intent to pick an image from the device gallery.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), IMAGE_PICK_REQUEST);
    }

    /**
     * Handles the result of the image picker and updates the preview.
     */
    @Override
    public void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (req == IMAGE_PICK_REQUEST && res == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            eventImagePreview.setImageURI(selectedImageUri);
        }
    }

    /**
     * Uploads the selected image to Firebase Storage, then calls {@link #saveEvent(String)}
     * with the resulting download URL.
     */
    private void uploadImageAndSaveEvent() {
        if (selectedImageUri == null) return;

        String filename = UUID.randomUUID().toString() + ".jpg";
        StorageReference imgRef = storageRef.child("event_posters/" + filename);

        imgRef.putFile(selectedImageUri)
                .addOnSuccessListener(t -> imgRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> saveEvent(uri.toString())))
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Image upload failed", Toast.LENGTH_SHORT).show());
    }

    /**
     * Saves a new event or updates an existing one in Firestore.
     * If imageUrl is not null, it is stored as the event poster URL.
     *
     * @param imageUrl Download URL of the event poster (may be null).
     */
    private void saveEvent(String imageUrl) {
        Map<String, Object> event = buildEventMap();

        if (imageUrl != null) event.put("eventPosterUrl", imageUrl);

        // New event extras
        if (eventId == null) {
            event.put("eventQR", UUID.randomUUID().toString());
            event.put("lotteryDone", false);

            db.collection("events")
                    .add(event)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(getContext(), "Event created!", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    });
        } else {
            db.collection("events").document(eventId)
                    .set(event, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Event updated.", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    });
        }
    }

    /**
     * Loads existing event data from Firestore and populates
     * the form fields and category checkboxes for editing.
     *
     * @param id ID of the event document to edit.
     */
    private void loadEventDataForEditing(String id) {
        db.collection("events").document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    eventTitle.setText(doc.getString("title"));
                    eventDescription.setText(doc.getString("description"));
                    eventLocation.setText(doc.getString("location"));
                    eventDate.setText(doc.getString("date"));

                    String timeVal = doc.getString("time");
                    if (timeVal != null) {
                        eventTime.setText(timeVal.replaceAll("(AM|PM)", "").trim());
                        selectAMPM(timeVal.contains("PM") ? "PM" : "AM");
                    }

                    eventCapacity.setText(String.valueOf(doc.getLong("eventCapacity")));
                    eventPrice.setText(String.valueOf(doc.getDouble("price")));
                    waitlistCapacity.setText(String.valueOf(doc.getLong("waitlistCapacity")));
                    registrationOpen.setText(doc.getString("registrationOpen"));
                    registrationClose.setText(doc.getString("registrationClose"));

                    existingImageUrl = doc.getString("eventPosterUrl");
                    if (existingImageUrl != null) {
                        Glide.with(requireContext()).load(existingImageUrl).into(eventImagePreview);
                    }

                    List<String> cats = (List<String>) doc.get("categories");
                    if (cats != null) {
                        catYoga.setChecked(cats.contains("Yoga and Mindfulness"));
                        catFitness.setChecked(cats.contains("Strength and Fitness Classes"));
                        catKidsSports.setChecked(cats.contains("Kids Sports Programs"));
                        catMartialArts.setChecked(cats.contains("Martial Arts"));
                        catTennis.setChecked(cats.contains("Tennis and Racquet Sports"));
                        catAquatics.setChecked(cats.contains("Aquatics and Swimming Lessons"));
                        catAdultSports.setChecked(cats.contains("Adult Drop-In Sports"));
                        catWellness.setChecked(cats.contains("Health and Wellness Workshops"));
                        catCreative.setChecked(cats.contains("Arts, Music and Creative Programs"));
                        catCamps.setChecked(cats.contains("Special Events and Camps"));
                    }

                });
    }

    /**
     * Basic form validation: checks required fields and numeric inputs
     * (capacity, price, waitlist). Shows inline errors or toasts when invalid.
     *
     * @return true if all inputs look valid; false otherwise.
     */
    private boolean validateInputs() {
        if (eventTitle.getText().toString().trim().isEmpty()) return error(eventTitle);
        if (eventDescription.getText().toString().trim().isEmpty()) return error(eventDescription);
        if (eventLocation.getText().toString().trim().isEmpty()) return error(eventLocation);
        if (eventDate.getText().toString().trim().isEmpty()) return toast("Enter a date");
        if (eventTime.getText().toString().trim().isEmpty() || ampm.isEmpty())
            return toast("Enter time and AM/PM");

        try {
            Integer.parseInt(eventCapacity.getText().toString().trim());
            Double.parseDouble(eventPrice.getText().toString().trim());
            Integer.parseInt(waitlistCapacity.getText().toString().trim());
        } catch (Exception e) {
            return toast("Invalid numeric value");
        }

        return true;
    }

    /**
     * Marks a TextInputEditText as required and returns false for use in validation chains.
     */
    private boolean error(TextInputEditText field) {
        field.setError("Required");
        return false;
    }

    /**
     * Convenience helper to show a short Toast and return false
     * so it can be used directly in validation branches.
     */
    private boolean toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        return false;
    }

    /**
     * Builds the Firestore event document map from the current form state,
     * including title, description, registration data, capacity, price,
     * geolocation flag, and selected categories.
     *
     * @return A mutable map ready to be written to Firestore.
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
        event.put("geolocation", switchButton.isChecked());

        if (eventId == null) {
            event.put("coordinate", new ArrayList<>());
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            event.put("creatorId", user.getUid());
        }

        // Categories
        List<String> categories = new ArrayList<>();

        if (catYoga.isChecked()) categories.add("Yoga and Mindfulness");
        if (catFitness.isChecked()) categories.add("Strength and Fitness Classes");
        if (catKidsSports.isChecked()) categories.add("Kids Sports Programs");
        if (catMartialArts.isChecked()) categories.add("Martial Arts");
        if (catTennis.isChecked()) categories.add("Tennis and Racquet Sports");
        if (catAquatics.isChecked()) categories.add("Aquatics and Swimming Lessons");
        if (catAdultSports.isChecked()) categories.add("Adult Drop-In Sports");
        if (catWellness.isChecked()) categories.add("Health and Wellness Workshops");
        if (catCreative.isChecked()) categories.add("Arts, Music and Creative Programs");
        if (catCamps.isChecked()) categories.add("Special Events and Camps");

        event.put("categories", categories);

        return event;
    }
}
