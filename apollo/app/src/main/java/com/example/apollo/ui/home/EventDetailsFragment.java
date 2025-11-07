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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class EventDetailsFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView textEventTitle, textEventDescription, textEventSummary, loginText;
    private TextView textWaitlistCount;
    private Button buttonJoinWaitlist;
    private ImageButton backButton;

    private String eventId;
    private String uid;

    // derived UI state
    private enum State { NONE, WAITING, INVITED, REGISTERED }
    private State state = State.NONE;

    // keep latest snapshots to avoid races
    private Boolean hasRegistered = null, hasInvited = null, hasWaiting = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_event_details, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        textEventTitle = view.findViewById(R.id.textEventTitle);
        textEventDescription = view.findViewById(R.id.textEventDescription);
        textEventSummary = view.findViewById(R.id.textEventSummary);
        buttonJoinWaitlist = view.findViewById(R.id.buttonJoinWaitlist);
        loginText = view.findViewById(R.id.loginText);
        backButton = view.findViewById(R.id.back_button);
        textWaitlistCount = view.findViewById(R.id.textWaitlistCount);

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            loadEventDetails(eventId);
            listenToWaitlistCount(eventId);
        }

        backButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_navigation_event_details_to_navigation_home);
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            loginText.setVisibility(View.VISIBLE);
            buttonJoinWaitlist.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Login to join waitlist", Toast.LENGTH_SHORT).show());
            loginText.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) getActivity().finish();
            });
        } else {
            loginText.setVisibility(View.GONE);
            uid = currentUser.getUid();
            observeUserEventState();   // live state from invites/registrations/waitlist
            wireJoinLeaveAction();     // join/leave with extra server-side checks
        }

        return view;
    }

    private void loadEventDetails(String eventId) {
        if (eventId == null) return;
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) { Log.w("Firestore","No such event "+eventId); return; }

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
                            ? registrationOpen + " - " + registrationClose : "Not specified";
                    String capacityText  = (eventCapacity != null)   ? "Capacity: " + eventCapacity : "Capacity: N/A";
                    String waitlistText  = (waitlistCapacity != null) ? "Waitlist: " + waitlistCapacity : "Waitlist: N/A";
                    String dateText  = (date != null) ? date : "N/A";
                    String timeText  = (time != null) ? time : "N/A";
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
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading event details", e));
    }

    // ---------- live state listeners ----------
    private void observeUserEventState() {
        if (eventId == null || uid == null) return;

        // registrations => REGISTERED (highest)
        db.collection("events").document(eventId)
                .collection("registrations").document(uid)
                .addSnapshotListener((doc, e) -> {
                    hasRegistered = (doc != null && doc.exists());
                    recomputeState();
                });

        // invites => INVITED
        db.collection("events").document(eventId)
                .collection("invites").document(uid)
                .addSnapshotListener((doc, e) -> {
                    hasInvited = (doc != null && doc.exists());
                    recomputeState();
                });

        // waitlist => WAITING
        waitlistRef().addSnapshotListener((doc, e) -> {
            hasWaiting = (doc != null && doc.exists());
            recomputeState();
        });
    }

    private void recomputeState() {
        State newState = State.NONE;
        if (Boolean.TRUE.equals(hasRegistered))          newState = State.REGISTERED;
        else if (Boolean.TRUE.equals(hasInvited))        newState = State.INVITED;
        else if (Boolean.TRUE.equals(hasWaiting))        newState = State.WAITING;

        if (newState != state) {
            state = newState;
            renderButton();
        }
    }

    // ---------- join/leave with double-checks ----------
    private void wireJoinLeaveAction() {
        buttonJoinWaitlist.setOnClickListener(v -> {
            if (state == State.INVITED)   { toast("You’ve already been invited. Check Notifications."); return; }
            if (state == State.REGISTERED){ toast("You’re already registered for this event."); return; }

            if (state == State.WAITING) {
                setLoading(true);
                waitlistRef().delete()
                        .addOnSuccessListener(ok -> { toast("Left waitlist"); setLoading(false); })
                        .addOnFailureListener(e -> { toast("Failed to leave: " + e.getMessage()); setLoading(false); });
                return;
            }

            // state == NONE → try to join, but **double-check** nothing changed on server
            setLoading(true);
            DocumentReference regRef   = db.collection("events").document(eventId).collection("registrations").document(uid);
            DocumentReference invRef   = db.collection("events").document(eventId).collection("invites").document(uid);
            DocumentReference wlRef    = waitlistRef();

            regRef.get().continueWithTask(t1 -> {
                DocumentSnapshot r = t1.getResult();
                if (r != null && r.exists()) throw new IllegalStateException("Already registered");
                return invRef.get();
            }).continueWithTask(t2 -> {
                DocumentSnapshot i = t2.getResult();
                if (i != null && i.exists()) throw new IllegalStateException("Already invited");
                return wlRef.get();
            }).continueWithTask(t3 -> {
                DocumentSnapshot w = t3.getResult();
                if (w != null && w.exists()) throw new IllegalStateException("You’ve already joined");
                HashMap<String, Object> data = new HashMap<>();
                data.put("joinedAt", FieldValue.serverTimestamp());
                data.put("state", "waiting");
                return wlRef.set(data);
            }).addOnSuccessListener(ok -> {
                toast("Joined waitlist");
                setLoading(false);
            }).addOnFailureListener(e -> {
                // map messages to friendly text
                String msg = e.getMessage();
                if (msg == null) msg = "Failed";
                if (msg.contains("Already invited"))    msg = "You’ve already been invited.";
                if (msg.contains("Already registered")) msg = "You’re already registered.";
                if (msg.contains("already joined"))     msg = "You’ve already joined this waitlist.";
                toast(msg);
                setLoading(false);
            });
        });
    }

    private DocumentReference waitlistRef() {
        return db.collection("events").document(eventId)
                .collection("waitlist").document(uid);
    }

    private void listenToWaitlistCount(String eventId) {
        if (eventId == null) return;
        db.collection("events").document(eventId)
                .collection("waitlist")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { Log.e("Firestore", "Listen failed: ", e); return; }
                    textWaitlistCount.setText(
                            snapshots != null ? ("Waitlist count: " + snapshots.size()) : "Waitlist count: N/A"
                    );
                });
    }

    // ---------- UI helpers ----------
    private void renderButton() {
        switch (state) {
            case REGISTERED:
                buttonJoinWaitlist.setText("REGISTERED");
                buttonJoinWaitlist.setEnabled(false);
                buttonJoinWaitlist.setBackgroundTintList(
                        ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
                buttonJoinWaitlist.setTextColor(
                        ContextCompat.getColor(requireContext(), android.R.color.white));
                break;

            case INVITED:
                buttonJoinWaitlist.setText("INVITED — CHECK NOTIFICATIONS");
                buttonJoinWaitlist.setEnabled(false);
                buttonJoinWaitlist.setBackgroundTintList(
                        ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
                buttonJoinWaitlist.setTextColor(
                        ContextCompat.getColor(requireContext(), android.R.color.white));
                break;

            case WAITING:
                buttonJoinWaitlist.setText("LEAVE WAITLIST");
                buttonJoinWaitlist.setEnabled(true);
                buttonJoinWaitlist.setBackgroundTintList(
                        ContextCompat.getColorStateList(requireContext(), android.R.color.black));
                buttonJoinWaitlist.setTextColor(
                        ContextCompat.getColor(requireContext(), android.R.color.white));
                break;

            case NONE:
            default:
                buttonJoinWaitlist.setText("JOIN WAITLIST");
                buttonJoinWaitlist.setEnabled(true);
                buttonJoinWaitlist.setBackgroundTintList(
                        ContextCompat.getColorStateList(requireContext(), R.color.lightblue));
                buttonJoinWaitlist.setTextColor(
                        ContextCompat.getColor(requireContext(), android.R.color.black));
        }
    }

    private void setLoading(boolean loading) {
        buttonJoinWaitlist.setEnabled(!loading);
        if (loading) buttonJoinWaitlist.setText("Please wait…");
    }

    private void toast(String m) {
        if (getContext() != null) Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show();
    }
}
