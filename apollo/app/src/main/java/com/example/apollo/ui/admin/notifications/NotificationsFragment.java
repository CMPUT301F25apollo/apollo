/**
 * NotificationsFragment.java
 *
 * This fragment lets admin users view a log of sent notifications.
 * It loads recent notification log entries from Firestore and displays them
 * as cards with title, message, event, sender, recipient, and timestamp.
 *
 * Extra details like event name and user names are resolved by additional
 * Firestore lookups after the initial log data is loaded.
 */
package com.example.apollo.ui.admin.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.apollo.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * Fragment that displays a list of notification logs for admins.
 * Each log card shows the notification content along with event,
 * sender, recipient, and time information.
 */
public class NotificationsFragment extends Fragment {

    private LinearLayout eventsContainer;
    private FirebaseFirestore db;

    /**
     * Inflates the notification logs layout, initializes the container,
     * and starts loading log entries from Firestore.
     *
     * @param inflater  LayoutInflater used to inflate the UI.
     * @param container Parent view group (may be null).
     * @param savedInstanceState Previously saved state (not used here).
     * @return The root view for this fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_notifications_admin, container, false);

        eventsContainer = view.findViewById(R.id.eventsContainer);
        db = FirebaseFirestore.getInstance();

        loadLogs();

        return view;
    }

    /**
     * Loads recent notification logs from Firestore and creates a card
     * for each log entry. If no logs are found, an empty message is shown.
     */
    private void loadLogs() {
        eventsContainer.removeAllViews();

        db.collection("notification_logs")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1000)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        addLogCard(doc);
                    }

                    if (snap.isEmpty()) {
                        addEmptyMessage();
                    }
                });
    }

    /**
     * Creates and populates a single log card view based on a Firestore document.
     * The card shows the notification title, message, and metadata such as event,
     * sender, recipient, and timestamp. Some fields are filled in asynchronously
     * with extra Firestore requests.
     *
     * @param doc The Firestore document representing a notification log entry.
     */
    private void addLogCard(QueryDocumentSnapshot doc) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View card = inflater.inflate(R.layout.item_log_card, eventsContainer, false);

        TextView title = card.findViewById(R.id.logTitle);
        TextView message = card.findViewById(R.id.logMessage);
        TextView meta = card.findViewById(R.id.logMeta);

        title.setText(doc.getString("notificationTitle"));
        message.setText(doc.getString("notificationMessage"));

        String eventId = doc.getString("eventId");
        String organizerId = doc.getString("organizerId");
        String recipientId = doc.getString("recipientId");
        String type = doc.getString("notificationType");
        String ts = doc.getTimestamp("timestamp") != null
                ? doc.getTimestamp("timestamp").toDate().toString()
                : "Unknown time";

        meta.setText(
                "Event: loading..." +
                        "\nType: " + type +
                        "\nFrom: loading..." +
                        "\nTo: loading..." +
                        "\nSent: " + ts
        );

        // Fetch event title
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    if (eventDoc.exists()) {
                        String eventName = eventDoc.getString("title");
                        updateMeta(meta, "Event", eventName);
                    }
                });

        // Fetch organizer name
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(organizerId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String name = userDoc.getString("fullName");
                        if (name == null) name = userDoc.getString("username");
                        updateMeta(meta, "From", name);
                    }
                });

        // Fetch recipient name
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(recipientId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String name = userDoc.getString("fullName");
                        if (name == null) name = userDoc.getString("username");
                        updateMeta(meta, "To", name);
                    }
                });

        eventsContainer.addView(card);
    }

    /**
     * Replaces one of the placeholder lines in the meta text
     * (e.g., "Event: loading...") with the resolved value.
     *
     * @param meta  The TextView containing the meta information.
     * @param key   The label whose value should be updated ("Event", "From", or "To").
     * @param value The text to insert for the given label.
     */
    private void updateMeta(TextView meta, String key, String value) {
        String current = meta.getText().toString();
        String updated = current.replace(key + ": loading...", key + ": " + value);
        meta.setText(updated);
    }

    /**
     * Adds a simple message to the container when no log entries are found.
     */
    private void addEmptyMessage() {
        TextView msg = new TextView(getContext());
        msg.setText("No logs found.");
        msg.setTextSize(16);
        msg.setPadding(20, 40, 20, 40);
        eventsContainer.addView(msg);
    }
}
