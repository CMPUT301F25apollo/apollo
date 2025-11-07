/**
 * *hi
 * EventDetailsFragment.java
 *
 * This fragment shows all the details about a specific event (title, description,
 * date, time, capacity, etc) and lets users join or leave the event’s waitlist.
 * It also updates the screen automatically when the user’s registration status
 * changes in Firestore.
 *
 * The fragment listens to live updates from Firestore collections
 * (registrations, invites, waitlist) and changes the button text or state
 * based on the user’s current status.
 *
 * Design pattern: Follows a simple MVC-style setup — this fragment acts as the
 * controller/view while Firestore acts as the model layer.
 *
 * Current issues:
 * - Minimal error handling for failed Firestore operations
 * - Could improve visuals or loading states for async actions
 */
package com.example.apollo.ui.home;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
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
    private ImageView eventPosterImage;

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

        ImageView qrButton = view.findViewById(R.id.qrButton);
        qrButton.setOnClickListener(v -> showQrCodeModal());


        textEventTitle = view.findViewById(R.id.textEventTitle);
        textEventDescription = view.findViewById(R.id.textEventDescription);
        textEventSummary = view.findViewById(R.id.textEventSummary);
        buttonJoinWaitlist = view.findViewById(R.id.buttonJoinWaitlist);
        loginText = view.findViewById(R.id.loginText);
        textWaitlistCount = view.findViewById(R.id.textWaitlistCount);
        eventPosterImage = view.findViewById(R.id.eventPosterImage);

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            loadEventDetails(eventId);
            listenToWaitlistCount(eventId);
        }

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

                        if (posterUrl != null && !posterUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(posterUrl)
                                    .into(eventPosterImage);
                        }

                        String registrationPeriod = (registrationOpen != null && registrationClose != null)
                                ? registrationOpen + " - " + registrationClose
                                : "Not specified";
                        String capacityText = (eventCapacity != null) ? "Capacity: " + eventCapacity : "Capacity: N/A";
                        String waitlistText = (waitlistCapacity != null) ? "Waitlist: " + waitlistCapacity : "Waitlist: N/A";
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
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading event details", e));
    }

    // ========= live state =========
    private void observeUserEventState() {
        if (eventId == null || uid == null) return;

        // 1) registrations/{uid} => REGISTERED wins highest priority
        db.collection("events").document(eventId)
                .collection("registrations").document(uid)
                .addSnapshotListener((doc, e) -> {
                    boolean registered = (doc != null && doc.exists());
                    recalcState(registered, /*invited*/null, /*waiting*/null);
                });

        // 2) invites/{uid} => INVITED second
        db.collection("events").document(eventId)
                .collection("invites").document(uid)
                .addSnapshotListener((doc, e) -> {
                    boolean invited = (doc != null && doc.exists());
                    recalcState(/*registered*/null, invited, /*waiting*/null);
                });

        // 3) waitlist/{uid} => WAITING otherwise
        waitlistRef().addSnapshotListener((doc, e) -> {
            boolean waiting = (doc != null && doc.exists());
            recalcState(/*registered*/null, /*invited*/null, waiting);
        });
    }

    // keep the latest view of each signal and then choose the priority

    private void recalcState(Boolean registered, Boolean invited, Boolean waiting) {
        if (registered != null) hasRegistered = registered;
        if (invited != null)   hasInvited = invited;
        if (waiting != null)   hasWaiting = waiting;

        State newState = State.NONE;
        if (Boolean.TRUE.equals(hasRegistered)) {
            newState = State.REGISTERED;
        } else if (Boolean.TRUE.equals(hasInvited)) {
            newState = State.INVITED;
        } else if (Boolean.TRUE.equals(hasWaiting)) {
            newState = State.WAITING;
        }

        if (newState != state) {
            state = newState;
            renderButton();
        }
    }

    // ========= join/leave logic (unchanged behavior for WAITING) =========
    private void wireJoinLeaveAction() {
        buttonJoinWaitlist.setOnClickListener(v -> {
            if (state == State.INVITED) {
                toast("You’ve already been invited. Check Notifications to proceed.");
                return;
            }
            if (state == State.REGISTERED) {
                toast("You’re already registered for this event.");
                return;
            }
            if (state == State.WAITING) {
                // leave
                setLoading(true);
                waitlistRef().delete()
                        .addOnSuccessListener(ok -> {
                            toast("Left waitlist");
                            setLoading(false);
                        })
                        .addOnFailureListener(e -> {
                            toast("Failed to leave: " + e.getMessage());
                            setLoading(false);
                        });
            } else {
                // join as waiting
                setLoading(true);
                HashMap<String, Object> data = new HashMap<>();
                data.put("joinedAt", FieldValue.serverTimestamp());
                data.put("state", "waiting");
                waitlistRef().set(data)
                        .addOnSuccessListener(ok -> {
                            toast("Joined waitlist");
                            setLoading(false);
                        })
                        .addOnFailureListener(e -> {
                            toast("Failed to join: " + e.getMessage());
                            setLoading(false);
                        });
            }
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
                    if (snapshots != null) {
                        int count = snapshots.size();
                        textWaitlistCount.setText("Waitlist count: " + count);
                    } else {
                        textWaitlistCount.setText("Waitlist count: N/A");
                    }
                });
    }

    // ========= UI helpers =========
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

    private void showQrCodeModal() {
        if (eventId == null) return;

        // Inflate a custom layout for the modal
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_qr_code, null);
        ImageView qrImageView = dialogView.findViewById(R.id.qrCodeImageView);
        Button closeButton = dialogView.findViewById(R.id.closeButton);

        // Generate QR code bitmap from eventId (or qrData)
        Bitmap qrBitmap = generateQRCode(eventId); // You need a method that returns a Bitmap
        qrImageView.setImageBitmap(qrBitmap);

        // Create dialog
        final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private Bitmap generateQRCode(String data) {
        try {
            com.google.zxing.MultiFormatWriter writer = new com.google.zxing.MultiFormatWriter();
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(data, com.google.zxing.BarcodeFormat.QR_CODE, 500, 500);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? android.graphics.Color.BLACK : android.graphics.Color.WHITE);
                }
            }
            return bmp;
        } catch (Exception e) {
            Log.e("QR", "Error generating QR code", e);
            return null;
        }
    }


}