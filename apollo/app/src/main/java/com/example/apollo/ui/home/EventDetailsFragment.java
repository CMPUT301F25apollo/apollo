package com.example.apollo.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.LoginActivity;
import com.example.apollo.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class EventDetailsFragment extends Fragment {

    private FirebaseFirestore db;
    private TextView textEventTitle, textEventDescription, textEventSummary, loginText;
    private Button buttonJoinWaitlist;

    private String eventId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_event_details, container, false);

        db = FirebaseFirestore.getInstance();

        textEventTitle = view.findViewById(R.id.textEventTitle);
        textEventDescription = view.findViewById(R.id.textEventDescription);
        textEventSummary = view.findViewById(R.id.textEventSummary);
        buttonJoinWaitlist = view.findViewById(R.id.buttonJoinWaitlist);
        loginText =  view.findViewById(R.id.loginText);

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            loadEventDetails(eventId);
        }

        // Back navigation example (if you add a back button later)
        ImageButton backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_navigation_event_details_to_navigation_home);
        });


        buttonJoinWaitlist.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Login to join waitlist", Toast.LENGTH_SHORT).show();
        });
        loginText.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });


        return view;
    }

    private void loadEventDetails(String eventId) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        eventRef.get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Extract all fields safely
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

                        // Build formatted strings safely (with fallbacks)
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

                        // Update UI
                        textEventTitle.setText(title != null ? title : "Untitled Event");
                        textEventDescription.setText(description != null ? description : "No description available");

                        textEventSummary.setText(
                                "Location: " + locationText +
                                        "\nDate: " + dateText +
                                        "\nTime: " + timeText +
                                        "\nPrice: " + priceText +
                                        "\nRegistration: " + registrationPeriod +
                                        "\n" + capacityText +
                                        "\n" + waitlistText
                        );


                    } else {
                        Log.w("Firestore", "No such event found with ID: " + eventId);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error loading event details", e));
    }

}
