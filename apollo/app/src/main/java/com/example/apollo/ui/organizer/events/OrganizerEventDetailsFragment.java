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
import com.google.firebase.auth.FirebaseAuth;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;




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
    private Button buttonEditEvent, buttonSendLottery, buttonViewParticipants;

    private ImageView eventPosterImage;
    private String eventId;
    private MapView mapView;
    private String eventName = "Event";
    private static final String TAG = "LotteryFix";
    private boolean showMap = false;
    private String organizerId;
    private Boolean lotteryDone = false;
    private boolean registrationClosed = false;

    /**
     * Called when the fragment’s view is created.
     * Initializes Firestore and UI components, loads event details,
     * and sets up button click actions.
     *
     * @param inflater           Used to inflate the layout.
     * @param container          The parent view group.
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


        mapView = view.findViewById(R.id.map);

        buttonSendLottery.setOnClickListener(v -> {

            if (eventId == null || eventId.isEmpty()) {
                Toast.makeText(getContext(), "Invalid event.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Registration still open → allow click but show toast
            if (!registrationClosed) {
                Toast.makeText(
                        getContext(),
                        "Registration must be closed before running the lottery.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            // Already done
            if (Boolean.TRUE.equals(lotteryDone)) {
                Toast.makeText(getContext(), "Lottery already sent.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Run dialog to choose winners
            askForWinnerCountAndRunLottery(eventId, eventName);
        });



        Button buttonNotifySelected = view.findViewById(R.id.buttonNotifySelected);
        Button buttonNotifyCancelled = view.findViewById(R.id.buttonNotifyCancelled);

        buttonNotifySelected.setOnClickListener(v -> {
            sendBulkNotification(eventId, "invited",
                    "You’re still invited!",
                    "Just a reminder that you were selected for " + eventName + ".");
        });

        buttonNotifyCancelled.setOnClickListener(v -> {
            sendBulkNotification(eventId, "cancelled",
                    "Update about your registration",
                    "Your registration for " + eventName + " was marked as cancelled.");
        });

        buttonEditEvent.setOnClickListener(v -> {
            if (eventId == null || eventId.isEmpty()) {
                Toast.makeText(getContext(), "Invalid event.", Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);

            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_organizer_add_event, bundle);
        });

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
                    .addSnapshotListener((snap, e) -> {
                        if (e != null || snap == null) {
                            Log.e(TAG, "Error listening for invites", e);
                            return;
                        }

                        for (com.google.firebase.firestore.DocumentChange change : snap.getDocumentChanges()) {
                            String status = change.getDocument().getString("status");

                            // invites that become "declined"
                            boolean isDeclined = "declined".equals(status);
                            boolean isNewOrUpdated =
                                    change.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED
                                            || change.getType() == com.google.firebase.firestore.DocumentChange.Type.MODIFIED;

                            if (isDeclined && isNewOrUpdated) {
                                Log.d(TAG, "Invite declined → running replacement lottery");
                                // run replacement lottery
                                runLottery(eventId, eventName, 1);

                                // create a notification for the organizer (no Toast)
                                if (organizerId != null && !organizerId.isEmpty()) {
                                    DocumentReference orgNotifRef = db.collection("users")
                                            .document(organizerId)
                                            .collection("notifications")
                                            .document();

                                    Map<String, Object> orgNotif = new HashMap<>();
                                    orgNotif.put("type", "replacement_drawn");
                                    orgNotif.put("eventId", eventId);
                                    orgNotif.put("title", "Replacement entrant drawn");
                                    orgNotif.put("message",
                                            "A replacement entrant was drawn automatically for \"" + eventName + "\".");
                                    orgNotif.put("createdAt", FieldValue.serverTimestamp());
                                    orgNotif.put("read", false);

                                    orgNotifRef.set(orgNotif)
                                            .addOnFailureListener(err ->
                                                    Log.e(TAG, "Failed to write organizer replacement notification", err));
                                }
                            }

                        }
                    });
        }



        // Navigate to waitlist participant screen
        buttonViewParticipants.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_event_waitlist, bundle);
        });

        // QR button opens a modal with generated QR code
        ImageView qrButton = view.findViewById(R.id.qrButton);
        qrButton.setOnClickListener(v -> {
            db.collection("events").document(eventId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) return;

                        String qrValue = doc.getString("eventQR");
                        if (qrValue == null) {
                            Toast.makeText(getContext(), "No QR saved for this event.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        generateAndShowQR(qrValue); // function below
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "QR load failed", Toast.LENGTH_SHORT).show());
        });


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

                        organizerId = document.getString("creatorId");


                        lotteryDone = document.getBoolean("lotteryDone");
                        if (lotteryDone == null) lotteryDone = false;
                        updateLotteryButtonUi();


                        lotteryDone = document.getBoolean("lotteryDone");
                        if (lotteryDone == null) lotteryDone = false;

// figure out if registration has closed yet
                        registrationClosed = false;
                        if (registrationClose != null && time != null) {
                            try {
                                String registrationCloseDateTime = registrationClose + " " + time;
                                SimpleDateFormat fmt =
                                        new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault());

                                long closeMillis = fmt.parse(registrationCloseDateTime).getTime();
                                long now = System.currentTimeMillis();
                                registrationClosed = now >= closeMillis;   // 👈 true only AFTER close time

                            } catch (ParseException e) {
                                Log.e(TAG, "Failed to parse registrationClose/time for registrationClosed", e);
                                registrationClosed = false;
                            }
                        }

// now update the button based on lotteryDone + registrationClosed
                        updateLotteryButtonUi();

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
                                        waitlistText +
                                        "\n\nLottery can be run after registration closes."
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
     * @param eventId   The ID of the event.
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
                    try {
                        k = Integer.parseInt(s);
                    } catch (Exception ignore) {
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




    /**
     * Prompts the organizer to enter how many replacement winners to select.
     * Reuses the same lottery logic, but is conceptually for filling spots
     * when invited users decline or cancel.
     */



    /**
     * Selects a random number of winners from the event’s waitlist and sends them notifications.
     *
     * @param eventId       The ID of the event.
     * @param eventName     The name of the event.
     * @param winnersToPick The number of winners to select.
     */
    public void runLottery(@NonNull String eventId, @NonNull String eventName, int winnersToPick) {
        if (getContext() == null) return;

        FirebaseFirestore fdb = FirebaseFirestore.getInstance();

        // Organizer ID for logging
        String organizerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "unknown";

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

                    // Gather candidates
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String uid = d.getId();
                        if (TextUtils.isEmpty(uid)) {
                            Object alt = d.get("uid");
                            if (alt != null) uid = String.valueOf(alt);
                        }
                        if (!TextUtils.isEmpty(uid)) candidates.add(uid);
                    }

                    // Deduplicate
                    candidates = new ArrayList<>(new java.util.LinkedHashSet<>(candidates));

                    if (candidates.isEmpty()) {
                        Toast.makeText(getContext(), "No entrants in waitlist.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Pick winners
                    Collections.shuffle(candidates, new Random());
                    int numberOfWinner = Math.min(winnersToPick, candidates.size());
                    List<String> winners = candidates.subList(0, numberOfWinner);

                    // Losers = everyone else
                    List<String> losers = new ArrayList<>(candidates);
                    losers.removeAll(winners);

                    WriteBatch batch = fdb.batch();

                    // ------------------------------------------------------------
                    // WINNERS
                    // ------------------------------------------------------------
                    for (String uid : winners) {

                        // ⬅️ Win log in event.lotteryResults
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

                        // ⬅️ Create invite
                        DocumentReference inviteRef = fdb.collection("events")
                                .document(eventId)
                                .collection("invites")
                                .document(uid);

                        Map<String, Object> invite = new HashMap<>();
                        invite.put("status", "invited");
                        invite.put("invitedAt", FieldValue.serverTimestamp());
                        batch.set(inviteRef, invite, SetOptions.merge());

                        // ⬅️ User notification
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

                        // ⬅️ waitlist update
                        DocumentReference wlRef = fdb.collection("events")
                                .document(eventId)
                                .collection("waitlist")
                                .document(uid);

                        Map<String, Object> wlUpdate = new HashMap<>();
                        wlUpdate.put("state", "invited");
                        wlUpdate.put("updatedAt", FieldValue.serverTimestamp());
                        batch.set(wlRef, wlUpdate, SetOptions.merge());

                        // --------------------------------------------------------
                        // NEW: GLOBAL notification_logs entry
                        // --------------------------------------------------------
                        DocumentReference logRef = fdb.collection("notification_logs").document();

                        Map<String, Object> logData = new HashMap<>();
                        logData.put("eventId", eventId);
                        logData.put("timestamp", FieldValue.serverTimestamp());
                        logData.put("organizerId", organizerId);
                        logData.put("recipientId", uid);
                        logData.put("notificationType", "lottery_win");
                        logData.put("notificationTitle", "You were selected!");
                        logData.put("notificationMessage", "You won the lottery for " + eventName + ".");

                        batch.set(logRef, logData);
                    }

                    // ------------------------------------------------------------
                    // LOSERS
                    // ------------------------------------------------------------
                    for (String uid : losers) {

                        // ⬅️ Loser log in event.lotteryResults
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

                        // ⬅️ Notification to loser
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

                        // ⬅️ Update waitlist entry
                        DocumentReference wlRef = fdb.collection("events")
                                .document(eventId)
                                .collection("waitlist")
                                .document(uid);

                        Map<String, Object> wlUpdate = new HashMap<>();
                        wlUpdate.put("updatedAt", FieldValue.serverTimestamp());
                        wlUpdate.put("lastResult", "not_selected");
                        batch.set(wlRef, wlUpdate, SetOptions.merge());

                        // --------------------------------------------------------
                        // NEW: GLOBAL notification_logs entry
                        // --------------------------------------------------------
                        DocumentReference logRef = fdb.collection("notification_logs").document();

                        Map<String, Object> logData = new HashMap<>();
                        logData.put("eventId", eventId);
                        logData.put("timestamp", FieldValue.serverTimestamp());
                        logData.put("organizerId", organizerId);
                        logData.put("recipientId", uid);
                        logData.put("notificationType", "lottery_loss");
                        logData.put("notificationTitle", "Not Selected This Time");
                        logData.put("notificationMessage", "You were not selected in the lottery for " + eventName + ".");

                        batch.set(logRef, logData);
                    }

                    // ------------------------------------------------------------
                    // COMMIT BATCH
                    // ------------------------------------------------------------
                    batch.commit()
                            .addOnSuccessListener(u -> {
                                Toast.makeText(getContext(),
                                        "Lottery sent to " + numberOfWinner + " entrant(s).",
                                        Toast.LENGTH_SHORT).show();

                                db.collection("events").document(eventId)
                                        .update("lotteryDone", true);

                                lotteryDone = true;
                                updateLotteryButtonUi();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("LotteryFix", "Batch commit failed", e);
                                Toast.makeText(getContext(),
                                        "Failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Waitlist load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    /**
     * Sends a notification to all entrants with a given invite status,
     * but ONLY if they have not opted out of notifications.
     */
    private void sendBulkNotification(String eventId, String status,
                                      String title, String message) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events")
                .document(eventId)
                .collection("invites")
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(snap -> {

                    if (snap == null || snap.isEmpty()) {
                        Toast.makeText(getContext(), "No entrants with status: " + status, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> uids = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        uids.add(d.getId());
                    }

                    // Now check each user’s opt-in setting
                    for (String uid : uids) {
                        db.collection("users").document(uid)
                                .get()
                                .addOnSuccessListener(userDoc -> {

                                    Boolean enabled = userDoc.getBoolean("notificationsEnabled");
                                    if (enabled == null) enabled = true; // default opt-in

                                    if (!enabled) return; // skip

                                    // Send the notification
                                    DocumentReference notifRef = db.collection("users")
                                            .document(uid)
                                            .collection("notifications")
                                            .document();

                                    Map<String, Object> notif = new HashMap<>();
                                    notif.put("type", "bulk_message");
                                    notif.put("eventId", eventId);
                                    notif.put("title", title);
                                    notif.put("message", message);
                                    notif.put("createdAt", FieldValue.serverTimestamp());
                                    notif.put("read", false);

                                    notifRef.set(notif);

                                });
                    }

                    Toast.makeText(getContext(),
                            "Notification sent to all opted-in entrants.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void generateAndShowQR(String content) {
        try {
            com.google.zxing.Writer writer = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix matrix = writer.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, 600, 600);

            Bitmap bmp = Bitmap.createBitmap(600, 600, Bitmap.Config.RGB_565);
            for (int x = 0; x < 600; x++) {
                for (int y = 0; y < 600; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            showQRPopup(bmp);

        } catch (Exception e) {
            Toast.makeText(getContext(), "QR generation failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void showQRPopup(Bitmap qrBitmap) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        ImageView img = new ImageView(requireContext());
        img.setImageBitmap(qrBitmap);

        builder.setView(img)
                .setPositiveButton("Close", null)
                .show();
    }
    private void updateLotteryButtonUi() {
        if (!isAdded() || getContext() == null || buttonSendLottery == null) return;

        if (Boolean.TRUE.equals(lotteryDone)) {
            // Lottery is finished
            buttonSendLottery.setText("LOTTERY SENT");
            buttonSendLottery.setEnabled(false);
            buttonSendLottery.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray)
            );
            buttonSendLottery.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.white)
            );

        } else {
            // Always show SEND LOTTERY
            buttonSendLottery.setText("SEND LOTTERY");

            // Style depends on if registration closed
            if (!registrationClosed) {
                // Greyed out but still clickable
                buttonSendLottery.setEnabled(true);
                buttonSendLottery.setBackgroundTintList(
                        ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray)
                );
                buttonSendLottery.setTextColor(
                        ContextCompat.getColor(requireContext(), android.R.color.white)
                );
            } else {
                // Active + real black button
                buttonSendLottery.setEnabled(true);
                buttonSendLottery.setBackgroundTintList(
                        ContextCompat.getColorStateList(requireContext(), android.R.color.black)
                );
                buttonSendLottery.setTextColor(
                        ContextCompat.getColor(requireContext(), android.R.color.white)
                );
            }
        }
    }
    /**
     * Logs a notification to the top-level "notification_logs" collection.
     */
    private void logNotificationToGlobal(
            String eventId,
            String organizerId,
            String recipientId,
            String type,
            String title,
            String message
    ) {
        FirebaseFirestore fdb = FirebaseFirestore.getInstance();
        DocumentReference logRef = fdb.collection("notification_logs").document();

        Map<String, Object> log = new HashMap<>();
        log.put("eventId", eventId);
        log.put("timestamp", FieldValue.serverTimestamp());
        log.put("organizerId", organizerId);
        log.put("recipientId", recipientId);
        log.put("notificationType", type);
        log.put("notificationTitle", title);
        log.put("notificationMessage", message);

        logRef.set(log)
                .addOnSuccessListener(a -> Log.d("NOTIF_LOG", "Log created"))
                .addOnFailureListener(e -> Log.e("NOTIF_LOG", "Error creating log", e));
    }



}

