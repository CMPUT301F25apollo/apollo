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

import android.text.TextUtils;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import androidx.preference.PreferenceManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * OrganizerEventDetailsFragment.java
 *
 * Purpose:
 * - Displays full details about a specific event created by an organizer.
 * - Allows organizers to:
 *   • Edit the event
 *   • Run the main lottery for entrants
 *   • Automatically run replacement lotteries when invites are declined/cancelled
 *   • View and export the waitlist
 *   • Send targeted notifications to invitees and waitlisted users
 *   • View a map of entrant geolocations (when enabled)
 *   • Generate and view a QR code for the event
 *
 * Design pattern:
 * - Acts as the Controller in an MVC-style design:
 *   • Firestore is the data/model layer
 *   • The XML layout is the view
 *   • This fragment coordinates user actions and data updates
 *
 * Notes:
 * - Enforces that the main lottery can only be run after registration is closed.
 * - Uses Firestore batch writes for consistent lottery + notification updates.
 * - Logs user-facing notifications into a global "notification_logs" collection.
 */
public class OrganizerEventDetailsFragment extends Fragment {

    private FirebaseFirestore db;
    private TextView textEventTitle, textEventDescription, textEventSummary;
    private Button buttonEditEvent, buttonSendLottery, buttonViewParticipants;
    private ImageView eventPosterImage;
    private MapView mapView;

    private String eventId;
    private String eventName = "Event";
    private static final String TAG = "LotteryFix";
    private String organizerId;
    private Boolean lotteryDone = false;
    private boolean registrationClosed = false;

    /**
     * Called when the fragment’s view is created.
     * Initializes Firestore, UI components, event listeners, and loads the event details.
     *
     * @param inflater           Layout inflater.
     * @param container          Parent container.
     * @param savedInstanceState Previously saved state, or null.
     * @return The inflated view hierarchy for this fragment.
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

        // MAIN LOTTERY BUTTON
        buttonSendLottery.setOnClickListener(v -> {

            if (eventId == null || eventId.isEmpty()) {
                Toast.makeText(getContext(), "Invalid event.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Registration still open mean don't allow running lottery yet
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

            // Ask for how many winners to select
            askForWinnerCountAndRunLottery(eventId, eventName);
        });

        Button notifyWaitlistBtn = view.findViewById(R.id.notifyWaitlistBtn);
        Button buttonNotifySelected = view.findViewById(R.id.buttonNotifySelected);
        Button buttonNotifyCancelled = view.findViewById(R.id.buttonNotifyCancelled);

        // Reminder for invited users
        buttonNotifySelected.setOnClickListener(v ->
                sendBulkNotification(
                        eventId,
                        "invited",
                        "You’re still invited!",
                        "Just a reminder that you were selected for " + eventName + "."
                )
        );

        // Status update for cancelled registrations
        buttonNotifyCancelled.setOnClickListener(v ->
                sendBulkNotification(
                        eventId,
                        "cancelled",
                        "Update about your registration",
                        "Your registration for " + eventName + " was marked as cancelled."
                )
        );

        // Notify all waitlisted entrants
        notifyWaitlistBtn.setOnClickListener(v -> sendNotificationToWaitlist(eventId));

        // Edit event
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

        // Load event details
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            loadEventDetails(eventId);
        }

        // Listen for invites that get declined/cancelled auto draw a replacement
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

                            DocumentSnapshot doc = change.getDocument();
                            String status = doc.getString("status");
                            Boolean replacementProcessed = doc.getBoolean("replacementProcessed");

                            boolean isDeclinedOrCancelled =
                                    "declined".equals(status) || "cancelled".equals(status);
                            boolean alreadyHandled = Boolean.TRUE.equals(replacementProcessed);

                            if (isDeclinedOrCancelled && !alreadyHandled) {
                                Log.d(TAG, "Invite declined/cancelled → running replacement lottery");

                                //  Run replacement lottery for exactly 1 entrant
                                runLottery(eventId, eventName, 1);

                                //  Mark this invite as processed so it never triggers again
                                doc.getReference()
                                        .update("replacementProcessed", true)
                                        .addOnFailureListener(err ->
                                                Log.e(TAG, "Failed to mark replacementProcessed", err));

                                //  notify organizer about replacement draw
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

        // Navigate to waitlist / participant management screen
        buttonViewParticipants.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_event_waitlist, bundle);
        });

        // QR code button open dialog with QR
        ImageView qrButton = view.findViewById(R.id.qrButton);
        qrButton.setOnClickListener(v -> db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String qrValue = doc.getString("eventQR");
                    if (qrValue == null) {
                        Toast.makeText(getContext(), "No QR saved for this event.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    generateAndShowQR(qrValue);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "QR load failed", Toast.LENGTH_SHORT).show()
                ));

        return view;
    }

    /**
     * Loads event details from Firestore, derives some state (registrationClosed, lotteryDone),
     * configures the lottery button, shows the event poster, and (if enabled) shows a map of
     * entrant locations.
     *
     * @param eventId ID of the event document to load.
     */
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
                        String posterUrl = document.getString("eventPosterUrl");
                        boolean showMap = Boolean.TRUE.equals(document.getBoolean("geolocation"));

                        organizerId = document.getString("creatorId");

                        // Lottery state
                        lotteryDone = document.getBoolean("lotteryDone");
                        if (lotteryDone == null) lotteryDone = false;
                        updateLotteryButtonUi();

                        // Determine if registration is closed yet (date + time)
                        registrationClosed = false;
                        if (registrationClose != null && time != null) {
                            try {
                                String registrationCloseDateTime = registrationClose + " " + time;
                                SimpleDateFormat fmt =
                                        new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault());

                                long closeMillis = fmt.parse(registrationCloseDateTime).getTime();
                                long now = System.currentTimeMillis();
                                registrationClosed = now >= closeMillis;

                            } catch (ParseException e) {
                                Log.e(TAG, "Failed to parse registrationClose/time for registrationClosed", e);
                                registrationClosed = false;
                            }
                        }

                        // Update lottery button again with final flags
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
                        textEventDescription.setText(
                                description != null ? description : "No description available"
                        );
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

                        // Map showing entrant coordinates
                        if (showMap) {
                            mapView.setVisibility(View.VISIBLE);
                            mapView.setTileSource(TileSourceFactory.MAPNIK);
                            mapView.setMultiTouchControls(true);

                            List<Map<String, Object>> coords =
                                    (List<Map<String, Object>>) document.get("coordinate");

                            if (coords != null && !coords.isEmpty()) {

                                // Center map on the first point
                                Map<String, Object> first = coords.get(0);
                                double lat = (double) first.get("lat");
                                double lon = (double) first.get("lon");

                                mapView.getController().setZoom(13.0);
                                mapView.getController().setCenter(new GeoPoint(lat, lon));

                                // Add markers for all recorded entrant locations
                                for (Map<String, Object> point : coords) {
                                    double pLat = (double) point.get("lat");
                                    double pLon = (double) point.get("lon");

                                    Marker marker = new Marker(mapView);
                                    marker.setPosition(new GeoPoint(pLat, pLon));
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                    marker.setTitle("Entrant Location");

                                    mapView.getOverlays().add(marker);
                                }

                                mapView.invalidate();
                            } else {
                                Log.d("MAP", "No coordinates stored in Firestore.");
                            }
                        }

                    } else {
                        Log.w("Firestore", "No such event found with ID: " + eventId);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error loading event details", e)
                );
    }

    /**
     * Shows a dialog asking the organizer how many winners should be selected,
     * then calls {@link #runLottery(String, String, int)} with that number.
     *
     * @param eventId   Event ID.
     * @param eventName Event name for notification text.
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
                    } catch (Exception ignore) { }
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
     * Runs the lottery for the given event:
     * - Loads all waitlist entrants with state "waiting"
     * - Randomly selects up to {@code winnersToPick} winners
     * - Marks winners as "invited" and losers as "loser"
     * - Creates per-user notifications
     * - Logs all notifications to "notification_logs"
     * - Sets event.lotteryDone = true on success and updates the lottery button UI
     *
     * @param eventId       Event ID.
     * @param eventName     Event name for message text.
     * @param winnersToPick Number of winners to select.
     */
    public void runLottery(@NonNull String eventId, @NonNull String eventName, int winnersToPick) {
        if (getContext() == null) return;

        FirebaseFirestore fdb = FirebaseFirestore.getInstance();

        // Organizer ID used for logging in notification_logs
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

                    // Gather candidate UIDs
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

                    // Randomize and pick winners
                    Collections.shuffle(candidates, new Random());
                    int numberOfWinner = Math.min(winnersToPick, candidates.size());
                    List<String> winners = candidates.subList(0, numberOfWinner);

                    // Everyone else is a loser
                    List<String> losers = new ArrayList<>(candidates);
                    losers.removeAll(winners);

                    WriteBatch batch = fdb.batch();

                    for (String uid : winners) {

                        // Lottery result log (winners)
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

                        // Invite entry
                        DocumentReference inviteRef = fdb.collection("events")
                                .document(eventId)
                                .collection("invites")
                                .document(uid);

                        Map<String, Object> invite = new HashMap<>();
                        invite.put("status", "invited");
                        invite.put("invitedAt", FieldValue.serverTimestamp());
                        batch.set(inviteRef, invite, SetOptions.merge());

                        // User notification
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

                        // Update waitlist entry
                        DocumentReference wlRef = fdb.collection("events")
                                .document(eventId)
                                .collection("waitlist")
                                .document(uid);

                        Map<String, Object> wlUpdate = new HashMap<>();
                        wlUpdate.put("state", "invited");
                        wlUpdate.put("updatedAt", FieldValue.serverTimestamp());
                        batch.set(wlRef, wlUpdate, SetOptions.merge());

                        // winner
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

                   //loser stuff
                    for (String uid : losers) {

                        // losers
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

                        // Notification to losers
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

                        // Update waitlist entry
                        DocumentReference wlRef = fdb.collection("events")
                                .document(eventId)
                                .collection("waitlist")
                                .document(uid);

                        Map<String, Object> wlUpdate = new HashMap<>();
                        wlUpdate.put("state", "loser");
                        wlUpdate.put("updatedAt", FieldValue.serverTimestamp());
                        wlUpdate.put("lastResult", "not_selected");
                        batch.set(wlRef, wlUpdate, SetOptions.merge());

                        // loser
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
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Waitlist load failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    /**
     * Sends a "waitlist_message" notification to everyone on the waitlist
     * with state "waiting" for this event, respecting user opt-out settings.
     *
     * @param eventId Event ID.
     */
    private void sendNotificationToWaitlist(String eventId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .whereEqualTo("state", "waiting")
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Toast.makeText(getContext(), "No waitlisted entrants found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot doc : query) {
                        String uid = doc.getId();
                        sendNotificationToUser(uid, eventId);
                    }

                    Toast.makeText(getContext(),
                            "Notifications sent to waitlisted entrants!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to send notifications to waitlist.",
                                Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Sends a single "waitlist_message" notification to a user, only if they are opted in.
     *
     * @param uid     User ID.
     * @param eventId Event ID.
     */
    private void sendNotificationToUser(String uid, String eventId) {

        FirebaseFirestore fdb = FirebaseFirestore.getInstance();

        // Load user + check opt-in setting
        fdb.collection("users").document(uid)
                .get()
                .addOnSuccessListener(userDoc -> {

                    Boolean enabled = userDoc.getBoolean("notificationsEnabled");
                    if (enabled == null) enabled = true;

                    if (!enabled) {
                        Log.d("NOTIF", "User " + uid + " opted out. Skipping.");
                        return;
                    }

                    // Create user-facing notification
                    DocumentReference notifRef = fdb.collection("users")
                            .document(uid)
                            .collection("notifications")
                            .document();

                    Map<String, Object> notif = new HashMap<>();
                    notif.put("type", "waitlist_message");
                    notif.put("eventId", eventId);
                    notif.put("title", "Update About Your Waitlist Status");
                    notif.put("message", "You are currently on the waitlist for " + eventName);
                    notif.put("createdAt", FieldValue.serverTimestamp());
                    notif.put("read", false);

                    notifRef.set(notif)
                            .addOnSuccessListener(a -> {
                                // Log it globally
                                DocumentReference logRef =
                                        fdb.collection("notification_logs").document();

                                Map<String, Object> log = new HashMap<>();
                                log.put("eventId", eventId);
                                log.put("timestamp", FieldValue.serverTimestamp());
                                log.put("organizerId", organizerId);
                                log.put("recipientId", uid);
                                log.put("notificationType", "waitlist_message");
                                log.put("notificationTitle", "Update About Your Waitlist Status");
                                log.put("notificationMessage",
                                        "There is an update regarding the waitlist for this event.");

                                logRef.set(log);
                            })
                            .addOnFailureListener(e ->
                                    Log.e("NOTIF", "Failed to write notification for " + uid, e)
                            );
                })
                .addOnFailureListener(e ->
                        Log.e("NOTIF", "Failed to check opt-in for " + uid, e)
                );
    }

    /**
     * Sends the same notification to all invitees with a given status
     * (e.g., "invited" or "cancelled"), respecting per-user opt-out.
     *
     * @param eventId Event ID.
     * @param status  Invite status to filter by.
     * @param title   Notification title.
     * @param message Notification message body.
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
                        Toast.makeText(getContext(),
                                "No entrants with status: " + status,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> uids = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        uids.add(d.getId());
                    }

                    //
                    for (String uid : uids) {
                        db.collection("users").document(uid)
                                .get()
                                .addOnSuccessListener(userDoc -> {

                                    Boolean enabled = userDoc.getBoolean("notificationsEnabled");
                                    if (enabled == null) enabled = true; // default opt-in

                                    if (!enabled) return;

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

    /**
     * Generates a QR code bitmap for the given content and shows it in a dialog.
     *
     * @param content String to encode in the QR code.
     */
    private void generateAndShowQR(String content) {
        try {
            com.google.zxing.Writer writer = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix matrix = writer.encode(
                    content,
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    600,
                    600
            );

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

    /**
     * Displays the given QR bitmap in a simple dialog.
     *
     * @param qrBitmap Generated QR code bitmap.
     */
    private void showQRPopup(Bitmap qrBitmap) {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        ImageView img = new ImageView(requireContext());
        img.setImageBitmap(qrBitmap);

        builder.setView(img)
                .setPositiveButton("Close", null)
                .show();
    }

    /**
     * Updates the "Send Lottery" button text, enabled state, and color based on:
     * - Whether the lottery has already been run (lotteryDone)
     * - Whether the registration window has closed (registrationClosed)
     */
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
            // Lottery not yet sent
            buttonSendLottery.setText("SEND LOTTERY");

            if (!registrationClosed) {
                // Registration still open visually greyed out, but click shows toast
                buttonSendLottery.setEnabled(true);
                buttonSendLottery.setBackgroundTintList(
                        ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray)
                );
                buttonSendLottery.setTextColor(
                        ContextCompat.getColor(requireContext(), android.R.color.white)
                );
            } else {
                // Registration closed active black button
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
     * Logs any notification (winner, loser, waitlist, bulk, etc.) to the global
     * "notification_logs" collection for audit / analytics.
     *
     * @param eventId    Event ID.
     * @param organizerId Organizer's user ID.
     * @param recipientId Recipient user ID.
     * @param type       Logical type (e.g. "lottery_win").
     * @param title      Notification title.
     * @param message    Notification message body.
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
