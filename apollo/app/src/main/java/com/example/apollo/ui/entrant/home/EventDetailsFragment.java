/**
 *
 * EventDetailsFragment.java
 *
 * This fragment shows detailed event information:
 * - Title, description, date/time, capacity, price, location
 * - Poster image
 * - Current waitlist count
 *
 * It allows users to:
 * - Join waitlist
 * - Leave waitlist
 * - See status updates in real time (registered, invited, waiting, none)
 * - SIGN UP when invited (new functionality)
 *
 * It listens to:
 * - /registrations/{uid}
 * - /invites/{uid}
 * - /waitlist/{uid}
 *
 * And updates the UI reactively based on priority:
 * REGISTERED > INVITED > WAITING > NONE
 *
 */

package com.example.apollo.ui.entrant.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.apollo.ui.login.LoginActivity;
import com.example.apollo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.SetOptions;

public class EventDetailsFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextView textEventTitle, textEventDescription, textEventSummary, loginText;
    private TextView textWaitlistCount;
    private Button buttonJoinWaitlist;

    // ⭐ NEW
    private Button buttonSignUp;

    private ImageView eventPosterImage;

    private String eventId;
    private String uid;
    private HashMap<String, Object> pendingData;

    private FusedLocationProviderClient fusedLocationClient;

    private enum State { NONE, WAITING, INVITED, REGISTERED }
    private State state = State.NONE;

    private Boolean hasRegistered = null, hasInvited = null, hasWaiting = null, isGeolocation = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_event_details, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        textEventTitle = view.findViewById(R.id.textEventTitle);
        textEventDescription = view.findViewById(R.id.textEventDescription);
        textEventSummary = view.findViewById(R.id.textEventSummary);
        buttonJoinWaitlist = view.findViewById(R.id.buttonJoinWaitlist);
        loginText = view.findViewById(R.id.loginText);
        textWaitlistCount = view.findViewById(R.id.textWaitlistCount);
        eventPosterImage = view.findViewById(R.id.eventPosterImage);

        // ⭐ NEW — SIGN UP BUTTON
        buttonSignUp = view.findViewById(R.id.buttonSignUp);
        buttonSignUp.setVisibility(View.GONE);
        buttonSignUp.setOnClickListener(v -> handleSignUp());

        // QR Button
        ImageView qrButton = view.findViewById(R.id.qrButton);
        qrButton.setOnClickListener(v -> showQrCodeModal());

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            loadEventDetails(eventId);
            listenToWaitlistCount(eventId);
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            loginText.setVisibility(View.VISIBLE);

            buttonJoinWaitlist.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Login to join waitlist", Toast.LENGTH_SHORT).show()
            );

            loginText.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) getActivity().finish();
            });

        } else {
            loginText.setVisibility(View.GONE);
            uid = currentUser.getUid();
            observeUserEventState();
            wireJoinLeaveAction();
        }

        return view;
    }

    // -------------------------------
    // NEW: SIGN UP LOGIC
    // -------------------------------
    private void handleSignUp() {
        if (uid == null || eventId == null) return;

        setLoading(true);

        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference regRef = eventRef.collection("registrations").document(uid);
        DocumentReference inviteRef = eventRef.collection("invites").document(uid);
        DocumentReference waitRef = eventRef.collection("waitlist").document(uid);

        Map<String, Object> regData = new HashMap<>();
        regData.put("registeredAt", FieldValue.serverTimestamp());
        regData.put("uid", uid);

        regRef.set(regData)
                .addOnSuccessListener(ok -> {
                    waitRef.delete();
                    inviteRef.delete();
                    toast("Successfully registered!");
                    setLoading(false);
                })
                .addOnFailureListener(err -> {
                    toast("Registration failed: " + err.getMessage());
                    setLoading(false);
                });
    }

    // ============================================================
    // LOAD EVENT DETAILS
    // ============================================================

    private void loadEventDetails(String eventId) {
        if (eventId == null) return;

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Log.w("Firestore", "Event not found: " + eventId);
                        return;
                    }

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
                    isGeolocation = Boolean.TRUE.equals(document.getBoolean("geolocation"));

                    if (posterUrl != null && !posterUrl.isEmpty()) {
                        Glide.with(this)
                                .load(posterUrl)
                                .into(eventPosterImage);
                    }

                    String regPeriod =
                            (registrationOpen != null && registrationClose != null)
                                    ? registrationOpen + " - " + registrationClose
                                    : "Not specified";

                    textEventTitle.setText(title != null ? title : "Untitled Event");
                    textEventDescription.setText(description != null ? description : "No description");
                    textEventSummary.setText(
                            "Location: " + (location != null ? location : "TBD") +
                                    "\nDate: " + (date != null ? date : "N/A") +
                                    "\nTime: " + (time != null ? time : "N/A") +
                                    "\nPrice: " + (price != null ? "$" + price : "Free") +
                                    "\nRegistration: " + regPeriod +
                                    "\nEvent Capacity: " + (eventCapacity != null ? eventCapacity : "N/A") +
                                    "\nWaitlist Capacity: " + (waitlistCapacity != null ? waitlistCapacity : "N/A")
                    );

                    // Capacity check
                    if (waitlistCapacity != null) {
                        db.collection("events").document(eventId)
                                .collection("waitlist")
                                .whereEqualTo("state", "waiting")
                                .get()
                                .addOnSuccessListener(snapshot -> {
                                    int count = snapshot.size();
                                    if (count >= waitlistCapacity) {
                                        buttonJoinWaitlist.setText("WAITLIST FULL");
                                        buttonJoinWaitlist.setEnabled(false);
                                        buttonJoinWaitlist.setBackgroundTintList(
                                                ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
                                    }
                                });
                    }

                    // Close registration if needed
                    boolean closed = false;

                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                        Date today = new Date();

                        Date openDate = registrationOpen != null ? sdf.parse(registrationOpen) : null;
                        Date closeDate = registrationClose != null ? sdf.parse(registrationClose) : null;

                        if (closeDate != null && closeDate.before(today)) closed = true;
                    } catch (Exception ignored) {}

                    if (closed) {
                        buttonJoinWaitlist.setText("Registration closed");
                        buttonJoinWaitlist.setEnabled(false);
                    }

                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error loading event details", e)
                );
    }

    // ============================================================
    // LISTEN FOR LIVE STATUS
    // ============================================================

    private void observeUserEventState() {
        if (eventId == null || uid == null) return;

        // Registered
        db.collection("events").document(eventId)
                .collection("registrations").document(uid)
                .addSnapshotListener((doc, e) -> {
                    recalcState(doc != null && doc.exists(), null, null);
                });

        // Invited
        db.collection("events").document(eventId)
                .collection("invites").document(uid)
                .addSnapshotListener((doc, e) -> {
                    recalcState(null, (doc != null && doc.exists()), null);
                });

        // Waitlist
        waitlistRef().addSnapshotListener((doc, e) -> {
            boolean waiting = (doc != null && doc.exists() &&
                    "waiting".equals(doc.getString("state")));
            recalcState(null, null, waiting);
        });
    }

    private void recalcState(Boolean registered, Boolean invited, Boolean waiting) {

        if (registered != null) hasRegistered = registered;
        if (invited != null) hasInvited = invited;
        if (waiting != null) hasWaiting = waiting;

        State newState = State.NONE;

        if (Boolean.TRUE.equals(hasRegistered)) newState = State.REGISTERED;
        else if (Boolean.TRUE.equals(hasInvited)) newState = State.INVITED;
        else if (Boolean.TRUE.equals(hasWaiting)) newState = State.WAITING;

        if (newState != state) {
            state = newState;
            renderButton();
        }
    }

    // ============================================================
    // GET LOCATION
    // ============================================================

    private void getUserLocation(LocationCallback callback) {

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    2002
            );
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null)
                        callback.onComplete(location.getLatitude(), location.getLongitude());
                    else
                        callback.onComplete(null, null);
                })
                .addOnFailureListener(e -> callback.onComplete(null, null));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 2002 &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            getUserLocation((lat, lon) -> {

                if (lat != null && lon != null) {
                    pendingData.put("latitude", lat);
                    pendingData.put("longitude", lon);

                    DocumentReference eventRef = db.collection("events").document(eventId);

                    eventRef.get().addOnSuccessListener(doc -> {
                        List<Map<String, Object>> coords =
                                (List<Map<String, Object>>) doc.get("coordinate");
                        if (coords == null) coords = new ArrayList<>();

                        Map<String, Object> newPoint = new HashMap<>();
                        newPoint.put("lat", lat);
                        newPoint.put("lon", lon);
                        coords.add(newPoint);

                        eventRef.update("coordinate", coords)
                                .addOnSuccessListener(a ->
                                        waitlistRef().set(pendingData, SetOptions.merge())
                                                .addOnSuccessListener(ok -> toast("Joined waitlist"))
                                                .addOnFailureListener(err ->
                                                        toast("Failed: " + err.getMessage())))
                                .addOnFailureListener(err ->
                                        toast("Coord update failed: " + err.getMessage()));
                    });

                } else {
                    waitlistRef().set(pendingData, SetOptions.merge());
                }
            });

        } else {
            toast("Location permission denied");
        }
    }

    // ============================================================
    // JOIN / LEAVE WAITLIST
    // ============================================================

    private void wireJoinLeaveAction() {

        buttonJoinWaitlist.setOnClickListener(v -> {

            if (state == State.INVITED) {
                toast("You’ve been invited! Use the SIGN-UP button.");
                return;
            }

            if (state == State.REGISTERED) {
                toast("You are already registered.");
                return;
            }

            if (state == State.WAITING) {
                setLoading(true);
                waitlistRef().delete()
                        .addOnSuccessListener(ok -> {
                            toast("Left waitlist");
                            setLoading(false);
                        })
                        .addOnFailureListener(e -> {
                            toast("Error leaving waitlist");
                            setLoading(false);
                        });
                return;
            }

            // JOIN WAITLIST
            setLoading(true);
            HashMap<String, Object> data = new HashMap<>();
            data.put("joinedAt", FieldValue.serverTimestamp());
            data.put("state", "waiting");
            pendingData = data;

            if (isGeolocation) {
                getUserLocation((lat, lon) -> {
                    if (lat != null && lon != null) {
                        data.put("latitude", lat);
                        data.put("longitude", lon);

                        DocumentReference eventRef = db.collection("events").document(eventId);

                        eventRef.get().addOnSuccessListener(doc -> {

                            List<Map<String, Object>> coords =
                                    (List<Map<String, Object>>) doc.get("coordinate");
                            if (coords == null) coords = new ArrayList<>();

                            Map<String, Object> p = new HashMap<>();
                            p.put("lat", lat);
                            p.put("lon", lon);
                            coords.add(p);

                            eventRef.update("coordinate", coords)
                                    .addOnSuccessListener(a ->
                                            waitlistRef().set(data, SetOptions.merge())
                                                    .addOnSuccessListener(ok -> {
                                                        toast("Joined waitlist");
                                                        setLoading(false);
                                                    })
                                                    .addOnFailureListener(e ->
                                                            toast("Failed: " + e.getMessage())))
                                    .addOnFailureListener(e -> {
                                        toast("Failed to add location");
                                        setLoading(false);
                                    });
                        });

                    } else {
                        waitlistRef().set(data, SetOptions.merge())
                                .addOnSuccessListener(ok -> {
                                    toast("Joined waitlist");
                                    setLoading(false);
                                })
                                .addOnFailureListener(e -> {
                                    toast("Failed: " + e.getMessage());
                                    setLoading(false);
                                });
                    }
                });

            } else {

                waitlistRef().set(data, SetOptions.merge())
                        .addOnSuccessListener(ok -> {
                            toast("Joined waitlist");
                            setLoading(false);
                        })
                        .addOnFailureListener(e -> {
                            toast("Failed: " + e.getMessage());
                            setLoading(false);
                        });
            }
        });
    }

    private DocumentReference waitlistRef() {
        return db.collection("events").document(eventId)
                .collection("waitlist").document(uid);
    }

    // ============================================================
    // LISTEN TO WAITLIST COUNT
    // ============================================================

    private void listenToWaitlistCount(String eventId) {
        DocumentReference eventRef = db.collection("events").document(eventId);

        eventRef.addSnapshotListener((eventSnapshot, eventError) -> {

            if (eventSnapshot == null || eventError != null) {
                Log.e("Firestore", "Event listener error", eventError);
                return;
            }

            Long waitCap = eventSnapshot.getLong("waitlistCapacity");
            if (waitCap == null) waitCap = 0L;

            Long finalCap = waitCap;

            eventRef.collection("waitlist")
                    .whereEqualTo("state", "waiting")
                    .addSnapshotListener((waitSnapshot, waitError) -> {

                        if (waitError != null) return;

                        int count = waitSnapshot != null ? waitSnapshot.size() : 0;
                        textWaitlistCount.setText("Waitlist count: " + count);

                        if (count >= finalCap && finalCap > 0) {
                            buttonJoinWaitlist.setText("WAITLIST FULL");
                            buttonJoinWaitlist.setEnabled(false);
                        }
                    });
        });
    }

    // ============================================================
    // RENDER BUTTON STATES
    // ============================================================

    private void renderButton() {

        if (!isAdded() || getContext() == null) return;

        android.content.Context ctx = getContext();

        switch (state) {

            case REGISTERED:
                buttonSignUp.setVisibility(View.GONE);
                buttonJoinWaitlist.setText("REGISTERED");
                buttonJoinWaitlist.setEnabled(false);
                buttonJoinWaitlist.setBackgroundTintList(
                        ContextCompat.getColorStateList(ctx, android.R.color.darker_gray));
                break;

            case INVITED:
                buttonSignUp.setVisibility(View.VISIBLE);
                buttonSignUp.setEnabled(true);

                buttonJoinWaitlist.setText("INVITED — SEE SIGN-UP BUTTON");
                buttonJoinWaitlist.setEnabled(false);
                buttonJoinWaitlist.setBackgroundTintList(
                        ContextCompat.getColorStateList(ctx, android.R.color.darker_gray));
                break;

            case WAITING:
                buttonSignUp.setVisibility(View.GONE);
                buttonJoinWaitlist.setText("LEAVE WAITLIST");
                buttonJoinWaitlist.setEnabled(true);
                buttonJoinWaitlist.setBackgroundTintList(
                        ContextCompat.getColorStateList(ctx, android.R.color.black));
                break;

            case NONE:
            default:
                buttonSignUp.setVisibility(View.GONE);
                buttonJoinWaitlist.setText("JOIN WAITLIST");
                buttonJoinWaitlist.setEnabled(true);
                buttonJoinWaitlist.setBackgroundTintList(
                        ContextCompat.getColorStateList(ctx, R.color.lightblue));
        }
    }

    private void setLoading(boolean loading) {
        buttonJoinWaitlist.setEnabled(!loading);
        buttonSignUp.setEnabled(!loading);
        if (loading) buttonJoinWaitlist.setText("Please wait…");
    }

    private void toast(String m) {
        if (getContext() != null)
            Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show();
    }

    // ============================================================
    // QR CODE
    // ============================================================

    private void showQrCodeModal() {
        if (eventId == null) return;

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_qr_code, null);

        ImageView qrImageView = dialogView.findViewById(R.id.qrCodeImageView);
        Button closeButton = dialogView.findViewById(R.id.closeButton);

        Bitmap qrBitmap = generateQRCode(eventId);
        qrImageView.setImageBitmap(qrBitmap);

        final android.app.AlertDialog dialog =
                new android.app.AlertDialog.Builder(getContext())
                        .setView(dialogView)
                        .create();

        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private Bitmap generateQRCode(String data) {
        try {
            com.google.zxing.MultiFormatWriter writer =
                    new com.google.zxing.MultiFormatWriter();

            com.google.zxing.common.BitMatrix bitMatrix =
                    writer.encode(data,
                            com.google.zxing.BarcodeFormat.QR_CODE,
                            500,
                            500);

            Bitmap bmp = Bitmap.createBitmap(500, 500, Bitmap.Config.RGB_565);

            for (int x = 0; x < 500; x++) {
                for (int y = 0; y < 500; y++) {
                    bmp.setPixel(x, y,
                            bitMatrix.get(x, y)
                                    ? android.graphics.Color.BLACK
                                    : android.graphics.Color.WHITE);
                }
            }

            return bmp;

        } catch (Exception e) {
            Log.e("QR", "Error generating QR code", e);
            return null;
        }
    }

    private interface LocationCallback {
        void onComplete(@Nullable Double lat, @Nullable Double lon);
    }
}
