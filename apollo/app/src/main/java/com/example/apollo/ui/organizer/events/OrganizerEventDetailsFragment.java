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

public class OrganizerEventDetailsFragment extends Fragment {

    private FirebaseFirestore db;
    private TextView textEventTitle, textEventDescription, textEventSummary;
    private Button buttonEditEvent, buttonSendLottery, buttonViewParticipants;

    private String eventId;
    // NEW: hold event title for notification text
    private String eventName = "Event";

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

        ImageButton backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_navigation_organizer_event_details_to_navigation_organizer_events);
        });

        buttonEditEvent.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_organizer_add_event, bundle);
        });

        // CHANGED: wire lottery action
        buttonSendLottery.setOnClickListener(v -> {
            if (eventId == null) return;
            askForWinnerCountAndRunLottery(eventId, eventName);
        });

        buttonViewParticipants.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_event_waitlist, bundle);
        });

        return view;
    }

    private void loadEventDetails(String eventId) {
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

                        // NEW: keep a safe event name for notifications
                        eventName = (title != null && !title.isEmpty()) ? title : "Event";

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

    // ===================== LOTTERY HELPERS (added) =====================

    private void askForWinnerCountAndRunLottery(@NonNull String eventId, @NonNull String eventName) {
        EditText input = new EditText(requireContext());
        input.setHint("Number of winners");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(requireContext())
                .setTitle("Hit Lottery")
                .setMessage("How many entrants should be selected?")
                .setView(input)
                .setPositiveButton("Run", (dlg, which) -> {
                    String s = input.getText().toString().trim();
                    if (s.isEmpty()) {
                        Toast.makeText(getContext(), "Enter a number", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int k;
                    try {
                        k = Integer.parseInt(s);
                    } catch (Exception e) {
                        k = 0;
                    }
                    if (k <= 0) {
                        Toast.makeText(getContext(), "Must be > 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    runLottery(eventId, eventName, k);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void runLottery(@NonNull String eventId, @NonNull String eventName, int winnersToPick) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events").document(eventId)
                .collection("waitlist")
                .get()
                .addOnSuccessListener(snap -> {
                    List<String> candidates = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String st = d.getString("state");
                        if (st == null || "waiting".equals(st)) candidates.add(d.getId());
                    }

                    Log.d("Lottery", "event=" + eventId + " total=" + snap.size()
                            + " eligible=" + candidates.size() + " need=" + winnersToPick);

                    if (candidates.isEmpty()) {
                        Toast.makeText(getContext(), "No entrants in waitlist.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // unbiased shuffle
                    Collections.shuffle(candidates, java.util.concurrent.ThreadLocalRandom.current());
                    int K = Math.min(winnersToPick, candidates.size());
                    List<String> winners = new ArrayList<>(candidates.subList(0, K));
                    Log.d("Lottery", "winners=" + winners);

                    WriteBatch batch = db.batch();
                    for (String uid : winners) {
                        // 1) record invite
                        DocumentReference inviteRef = db.collection("events").document(eventId)
                                .collection("invites").document(uid);
                        Map<String,Object> invite = new HashMap<>();
                        invite.put("status","invited");
                        invite.put("invitedAt", FieldValue.serverTimestamp());
                        batch.set(inviteRef, invite, SetOptions.merge());

                        // 2) push user notification
                        DocumentReference notifRef = db.collection("users").document(uid)
                                .collection("notifications").document();
                        Map<String,Object> notif = new HashMap<>();
                        notif.put("type","lottery_win");
                        notif.put("eventId", eventId);
                        notif.put("title","You were selected!");
                        notif.put("message","You won the lottery for " + eventName + ". Tap to register.");
                        notif.put("createdAt", FieldValue.serverTimestamp());
                        notif.put("read", false);
                        batch.set(notifRef, notif);

                        // 3) remove from waitlist (OR set state:"invited" if you want to keep a row)
                        DocumentReference wlRef = db.collection("events").document(eventId)
                                .collection("waitlist").document(uid);
                        batch.delete(wlRef);
                        // alternative instead of delete:
                        // batch.set(wlRef, Collections.singletonMap("state","invited"), SetOptions.merge());
                    }

                    batch.commit()
                            .addOnSuccessListener(u -> Toast.makeText(getContext(),
                                    "Lottery sent to " + K + " entrant(s).", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(),
                                    "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Waitlist load failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

}