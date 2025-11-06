package com.example.apollo.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.apollo.ui.login.LoginActivity;
import com.example.apollo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class EventDetailsFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView textEventTitle, textEventDescription, textEventSummary, loginText;
    private Button buttonJoinWaitlist;
    private ImageButton backButton;

    private String eventId;
    private String uid;
    private boolean joined = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_event_details, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Bind views
        textEventTitle = view.findViewById(R.id.textEventTitle);
        textEventDescription = view.findViewById(R.id.textEventDescription);
        textEventSummary = view.findViewById(R.id.textEventSummary);
        buttonJoinWaitlist = view.findViewById(R.id.buttonJoinWaitlist);
        loginText = view.findViewById(R.id.loginText);
        backButton = view.findViewById(R.id.back_button);

        // Get arguments
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            loadEventDetails(eventId);
        }

        // Back navigation
        backButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_navigation_event_details_to_navigation_home);
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Not signed in
            loginText.setVisibility(View.VISIBLE);
            buttonJoinWaitlist.setEnabled(false);

            loginText.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
        } else {
            // Signed in
            loginText.setVisibility(View.GONE);
            uid = currentUser.getUid();
            initWaitlistState();
        }

        return view;
    }

    private void loadEventDetails(String eventId) {
        if (eventId == null) return;
        DocumentReference eventRef = db.collection("events").document(eventId);
        eventRef.get()
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

    // 🔹 Waitlist logic below

    private void initWaitlistState() {
        setLoading(true);
        waitlistRef().get()
                .addOnSuccessListener(snap -> {
                    joined = snap.exists();
                    renderButton();
                    setLoading(false);

                    buttonJoinWaitlist.setOnClickListener(v -> {
                        setLoading(true);
                        if (joined) {
                            // Leave waitlist
                            waitlistRef().delete()
                                    .addOnSuccessListener(ok -> {
                                        joined = false;
                                        renderButton();
                                        setLoading(false);
                                        toast("Left waitlist");
                                    })
                                    .addOnFailureListener(e -> {
                                        setLoading(false);
                                        toast("Failed to leave: " + e.getMessage());
                                    });
                        } else {
                            // Join waitlist
                            HashMap<String, Object> data = new HashMap<>();
                            data.put("joinedAt", FieldValue.serverTimestamp());
                            waitlistRef().set(data)
                                    .addOnSuccessListener(ok -> {
                                        joined = true;
                                        renderButton();
                                        setLoading(false);
                                        toast("Joined waitlist");
                                    })
                                    .addOnFailureListener(e -> {
                                        setLoading(false);
                                        toast("Failed to join: " + e.getMessage());
                                    });
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Check failed: " + e.getMessage());
                });
    }

    private DocumentReference waitlistRef() {
        return db.collection("events").document(eventId)
                .collection("waitlist").document(uid);
    }

    private void renderButton() {
        if (joined) {
            buttonJoinWaitlist.setText("LEAVE WAITLIST");
            buttonJoinWaitlist.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), android.R.color.black));
            buttonJoinWaitlist.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        } else {
            buttonJoinWaitlist.setText("JOIN WAITLIST");
            buttonJoinWaitlist.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.lightblue));
            buttonJoinWaitlist.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        }
        buttonJoinWaitlist.setEnabled(true);
    }

    private void setLoading(boolean loading) {
        buttonJoinWaitlist.setEnabled(!loading);
        if (loading) buttonJoinWaitlist.setText("Please wait…");
    }

    private void toast(String m) {
        if (getContext() != null)
            Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show();
    }
}
