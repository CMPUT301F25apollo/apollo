package com.example.apollo.ui.organizer.events;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
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

import android.text.TextUtils;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import androidx.preference.PreferenceManager;
import org.osmdroid.util.BoundingBox;



/**
 * OrganizerEventDetailsFragment.java
 *
 * Purpose:
 * Displays detailed information about a specific event created by an organizer.
 * Allows the organizer to edit event details, run a lottery for entrants, and view the waitlist.
 * Also generates a QR code for event identification.
 *
 * Design Pattern:
 * - Acts as the Controller in the MVC pattern.
 * - Connects Firestore (Model) with the layout views (View).
 * - Uses Android Navigation for transitions between screens.
 *
 * Notes:
 * - Lottery selection randomly picks entrants from the waitlist and sends notifications.
 * - Supports QR code generation for event access.
 */
public class OrganizerEventDetailsFragment extends Fragment {

    private FirebaseFirestore db;
    private TextView textEventTitle, textEventDescription, textEventSummary;
    private Button buttonEditEvent, buttonSendLottery, buttonViewParticipants, buttonDrawReplacement;
    private boolean canDrawReplacement = false;
    private ImageView eventPosterImage;
    private String eventId;
    private MapView mapView;
    private String eventName = "Event";
    private static final String TAG = "LotteryFix";
    private boolean showMap = false;


    /**
     * Called when the fragment’s view is created.
     * Initializes Firestore and UI components, loads event details,
     * and sets up button click actions.
     *
     * @param inflater Used to inflate the layout.
     * @param container The parent view group.
     * @param savedInstanceState Bundle with saved state, if any.
     * @return The root view for this fragment.
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
        eventPosterImage = view.findViewById(R.id.eventPosterImage);
        buttonDrawReplacement = view.findViewById(R.id.buttonDrawReplacement);
        updateDrawReplacementButtonEnabled(false);
        mapView = view.findViewById(R.id.map);

        Configuration.getInstance().load(
                getContext(),
                PreferenceManager.getDefaultSharedPreferences(getContext())
        );
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            loadEventDetails(eventId);
        }

        if (eventId != null && !eventId.isEmpty()) {
            db.collection("events")
                    .document(eventId)
                    .collection("invites")
                    .whereEqualTo("status", "declined")   // only declined invites
                    .addSnapshotListener((snap, e) -> {
                        if (e != null) {
                            Log.e(TAG, "Error listening for declined invites", e);
                            updateDrawReplacementButtonEnabled(false);
                            return;
                        }

                        boolean hasDeclined = (snap != null && !snap.isEmpty());
                        updateDrawReplacementButtonEnabled(hasDeclined);
                    });
        }

        // Navigate to edit event screen
        // Prompt organizer to input number of winners, then run lottery
        buttonSendLottery.setOnClickListener(v -> {
            if (eventId == null || eventId.isEmpty()) {
                Toast.makeText(getContext(), "Invalid event.", Toast.LENGTH_SHORT).show();
                return;
            }
            askForWinnerCountAndRunLottery(eventId, eventName);
        });
        buttonDrawReplacement.setOnClickListener(v -> {
            if (!canDrawReplacement) {
                Toast.makeText(getContext(),
                        "You can only draw replacements after a selected entrant declines.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (eventId == null || eventId.isEmpty()) {
                Toast.makeText(getContext(), "Invalid event.", Toast.LENGTH_SHORT).show();
                return;
            }
            askForReplacementCountAndRunLottery(eventId, eventName);
        });


        // Navigate to waitlist participant screen
        buttonViewParticipants.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_event_waitlist, bundle);
        });

        // QR button opens a modal with generated QR code
        ImageView qrButton = view.findViewById(R.id.qrButton);
        qrButton.setOnClickListener(v -> showQrCodeModal());

        return view;
    }

    /**
     * Loads event details from Firestore and displays them in the UI.
     *
     * @param eventId The ID of the event to load.
     */
    private void loadEventDetails(String eventId) {
//        mapView = requireView().findViewById(R.id.map);


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
                        String posterUrl = document.getString("eventPosterUrl");
                        boolean showMap = Boolean.TRUE.equals(document.getBoolean("geolocation"));



                        if (posterUrl != null && !posterUrl.isEmpty()) {
                            Glide.with(this).load(posterUrl).into(eventPosterImage);
                        }

                        String registrationPeriod = (registrationOpen != null && registrationClose != null)
                                ? registrationOpen + " - " + registrationClose
                                : "Not specified";

                        String capacityText = (eventCapacity != null)
                                ? "Event Capacity : " + eventCapacity
                                : "Event Capacity: N/A";

                        String waitlistText = (waitlistCapacity != null)
                                ? "Waitlist Capacity: " + waitlistCapacity
                                : "Waitlist Capacity: N/A";

                        String priceText = (price != null) ? "$" + price : "Free";
                        String locationText = (location != null) ? location : "TBD";

                        eventName = (title != null && !title.isEmpty()) ? title : "Event";

                        textEventTitle.setText(title != null ? title : "Untitled Event");
                        textEventDescription.setText(description != null ? description : "No description available");
                        textEventSummary.setText(
                                "Location: " + locationText + "\n" +
                                        "Date: " + date + "\n" +
                                        "Time: " + time + "\n" +
                                        "Price: " + priceText + "\n" +
                                        "Registration: " + registrationPeriod + "\n" +
                                        capacityText + "\n" +
                                        waitlistText
                        );

                        if (showMap) {
                            mapView.setVisibility(View.VISIBLE);
                            mapView.setTileSource(TileSourceFactory.MAPNIK);
                            mapView.setMultiTouchControls(true);


                            // ----- Load coordinates array from Firestore -----
                            List<Map<String, Object>> coords =
                                    (List<Map<String, Object>>) document.get("coordinate");

                            if (coords != null && !coords.isEmpty()) {

                                // Center map on the first point
                                Map<String, Object> first = coords.get(0);
                                double lat = (double) first.get("lat");
                                double lon = (double) first.get("lon");

                                mapView.getController().setZoom(13.0);
                                mapView.getController().setCenter(new GeoPoint(lat, lon));

                                // Add markers for every coordinate point
                                for (Map<String, Object> point : coords) {
                                    double pLat = (double) point.get("lat");
                                    double pLon = (double) point.get("lon");

                                    Marker marker = new Marker(mapView);
                                    marker.setPosition(new GeoPoint(pLat, pLon));
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                    marker.setTitle("Entrant Location");

                                    mapView.getOverlays().add(marker);
                                }

                                mapView.invalidate(); // refresh map
                            } else {
                                Log.d("MAP", "No coordinates stored in Firestore.");
                            }
                        }



                    } else {
                        Log.w("Firestore", "No such event found with ID: " + eventId);
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading event details", e));
    }

    /**
     * Prompts the organizer to enter how many winners should be selected for the lottery.
     *
     * @param eventId The ID of the event.
     * @param eventName The name of the event for notification messages.
     */
    private void askForWinnerCountAndRunLottery(@NonNull String eventId, @NonNull String eventName) {
        if (getContext() == null) return;

        EditText input = new EditText(requireContext());
        input.setHint("Number of winners");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Run Lottery")
                .setMessage("How many entrants should be selected?")
                .setView(input)
                .setPositiveButton("Run", (dlg, which) -> {
                    String s = (input.getText() == null) ? "" : input.getText().toString().trim();
                    int k = 0;
                    try { k = Integer.parseInt(s); } catch (Exception ignore) {}
                    if (k <= 0) {
                        Toast.makeText(getContext(), "Must be > 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    runLottery(eventId, eventName, k);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateDrawReplacementButtonEnabled(boolean enabled) {
        canDrawReplacement = enabled;
        if (!isAdded() || buttonDrawReplacement == null || getContext() == null) return;

        int bgColor = enabled ? android.R.color.black : android.R.color.darker_gray;
        int textColor = android.R.color.white;

        buttonDrawReplacement.setEnabled(enabled);
        buttonDrawReplacement.setBackgroundTintList(
                ContextCompat.getColorStateList(requireContext(), bgColor));
        buttonDrawReplacement.setTextColor(
                ContextCompat.getColor(requireContext(), textColor));
    }


    /**
     * Prompts the organizer to enter how many replacement winners to select.
     * Reuses the same lottery logic, but is conceptually for filling spots
     * when invited users decline or cancel.
     */
    private void askForReplacementCountAndRunLottery(@NonNull String eventId, @NonNull String eventName) {
        if (getContext() == null) return;

        EditText input = new EditText(requireContext());
        input.setHint("Number of replacements");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Draw Replacement")
                .setMessage("How many replacement entrants should be selected?")
                .setView(input)
                .setPositiveButton("Draw", (dlg, which) -> {
                    String s = (input.getText() == null) ? "" : input.getText().toString().trim();
                    int k = 0;
                    try { k = Integer.parseInt(s); } catch (Exception ignore) {}
                    if (k <= 0) {
                        Toast.makeText(getContext(), "Must be > 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Reuse the same lottery logic
                    runLottery(eventId, eventName, k);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    /**
     * Selects a random number of winners from the event’s waitlist and sends them notifications.
     *
     * @param eventId The ID of the event.
     * @param eventName The name of the event.
     * @param winnersToPick The number of winners to select.
     */
    public void runLottery(@NonNull String eventId, @NonNull String eventName, int winnersToPick) {
        if (getContext() == null) return;

        FirebaseFirestore fdb = FirebaseFirestore.getInstance();

        fdb.collection("events").document(eventId)
                .collection("waitlist")
                .whereEqualTo("state", "waiting")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null) {
                        Toast.makeText(getContext(), "Failed to load waitlist.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    List<String> candidates = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String uid = d.getId();
                        if (TextUtils.isEmpty(uid)) {
                            Object alt = d.get("uid");
                            if (alt != null) uid = String.valueOf(alt);
                        }
                        if (!TextUtils.isEmpty(uid)) candidates.add(uid);
                    }

                    // Remove duplicates just in case
                    candidates = new ArrayList<>(new java.util.LinkedHashSet<>(candidates));

                    if (candidates.isEmpty()) {
                        Toast.makeText(getContext(), "No entrants in waitlist.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Shuffle and pick winners
                    Collections.shuffle(candidates, new Random());
                    int numberOfWinner = Math.min(winnersToPick, candidates.size());
                    List<String> winners = candidates.subList(0, numberOfWinner);
//                    List<String> losers = candidates.subList(numberOfWinner, candidates.size());

                    // Losers = all candidates who weren't picked
                    List<String> losers = new ArrayList<>(candidates);
                    losers.removeAll(winners);

                    WriteBatch batch = fdb.batch();

                    // Process winners
                    for (String uid : winners) {

                        // 1. Log winner to lotteryResults/winners
                        DocumentReference lotteryWinnerRef = fdb.collection("events")
                                .document(eventId)
                                .collection("lotteryResults")
                                .document("winners")
                                .collection("users")
                                .document(uid);

                        Map<String, Object> winnerLog = new HashMap<>();
                        winnerLog.put("uid", uid);
                        winnerLog.put("status", "invited");
                        winnerLog.put("timestamp", FieldValue.serverTimestamp());
                        batch.set(lotteryWinnerRef, winnerLog);


                        DocumentReference inviteRef = fdb.collection("events")
                                .document(eventId)
                                .collection("invites")
                                .document(uid);

                        Map<String, Object> invite = new HashMap<>();
                        invite.put("status", "invited");
                        invite.put("invitedAt", FieldValue.serverTimestamp());
                        batch.set(inviteRef, invite, SetOptions.merge());

                        // 3. Send notification to user
                        DocumentReference notifRef = fdb.collection("users")
                                .document(uid)
                                .collection("notifications")
                                .document();

                        Map<String, Object> notif = new HashMap<>();
                        notif.put("type", "lottery_win");
                        notif.put("eventId", eventId);
                        notif.put("title", "You were selected!");
                        notif.put("message", "You won the lottery for " + eventName + ".");
                        notif.put("createdAt", FieldValue.serverTimestamp());
                        notif.put("read", false);
                        batch.set(notifRef, notif);

                        // 4. Update waitlist entry
                        DocumentReference wlRef = fdb.collection("events")
                                .document(eventId)
                                .collection("waitlist")
                                .document(uid);

                        Map<String, Object> wlUpdate = new HashMap<>();
                        wlUpdate.put("state", "invited");
                        wlUpdate.put("updatedAt", FieldValue.serverTimestamp());
                        batch.set(wlRef, wlUpdate, SetOptions.merge());
                    }


// Process losers
                    for (String uid : losers) {

                        // 1. Log loser to lotteryResults/losers
                        DocumentReference lotteryLoserRef = fdb.collection("events")
                                .document(eventId)
                                .collection("lotteryResults")
                                .document("losers")
                                .collection("users")
                                .document(uid);

                        Map<String, Object> loserLog = new HashMap<>();
                        loserLog.put("uid", uid);
                        loserLog.put("status", "rejected");
                        loserLog.put("timestamp", FieldValue.serverTimestamp());
                        batch.set(lotteryLoserRef, loserLog);

                        // 2. Send notification to loser
                        DocumentReference notifRef = fdb.collection("users")
                                .document(uid)
                                .collection("notifications")
                                .document();

                        Map<String, Object> notif = new HashMap<>();
                        notif.put("type", "lottery_loss");
                        notif.put("eventId", eventId);
                        notif.put("title", "Not Selected This Time");
                        notif.put("message", "You were not selected in the lottery for " + eventName + ".");
                        notif.put("createdAt", FieldValue.serverTimestamp());
                        notif.put("read", false);
                        batch.set(notifRef, notif);

                        // 3. Update waitlist entry
                        DocumentReference wlRef = fdb.collection("events")
                                .document(eventId)
                                .collection("waitlist")
                                .document(uid);

                        Map<String, Object> wlUpdate = new HashMap<>();
                        wlUpdate.put("state", "rejected");
                        wlUpdate.put("updatedAt", FieldValue.serverTimestamp());
                        batch.set(wlRef, wlUpdate, SetOptions.merge());
                    }




                    batch.commit()
                            .addOnSuccessListener(u ->
                                    Toast.makeText(getContext(), "Lottery sent to " + numberOfWinner + " entrant(s).", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Batch commit failed", e);
                                Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });


                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Waitlist load failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Generates a QR code image from a given string.
     *
     * @param data The string to encode in the QR code.
     * @return A Bitmap representing the QR code, or null if generation fails.
     */
    private Bitmap generateQRCode(String data) {
        try {
            com.google.zxing.MultiFormatWriter writer = new com.google.zxing.MultiFormatWriter();
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(data, com.google.zxing.BarcodeFormat.QR_CODE, 500, 500);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y)
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

    /**
     * Displays a modal showing the QR code for the current event.
     */
    private void showQrCodeModal() {
        if (eventId == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_qr_code, null);
        ImageView qrImageView = dialogView.findViewById(R.id.qrCodeImageView);
        Button closeButton = dialogView.findViewById(R.id.closeButton);

        Bitmap qrBitmap = generateQRCode(eventId);
        qrImageView.setImageBitmap(qrBitmap);

        final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    public void setDb(FirebaseFirestore firestore) {
        this.db = firestore;
    }

}