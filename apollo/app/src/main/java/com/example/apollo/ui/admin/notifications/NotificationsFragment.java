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

public class NotificationsFragment extends Fragment {

    private LinearLayout eventsContainer;
    private FirebaseFirestore db;

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

    private void loadLogs() {
        eventsContainer.removeAllViews();

        db.collection("notification_logs")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1000)   // optional
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

        // ------- INITIAL META TEXT WITH PLACEHOLDERS -------
        meta.setText(
                "Event: loading..." +
                        "\nType: " + type +
                        "\nFrom: loading..." +
                        "\nTo: loading..." +
                        "\nSent: " + ts
        );

        // ------- FETCH EVENT TITLE -------
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

        // ------- FETCH ORGANIZER NAME -------
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

        // ------- FETCH RECIPIENT NAME -------
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
    private void updateMeta(TextView meta, String key, String value) {
        String current = meta.getText().toString();
        String updated = current.replace(key + ": loading...", key + ": " + value);
        meta.setText(updated);
    }


    private void addEmptyMessage() {
        TextView msg = new TextView(getContext());
        msg.setText("No logs found.");
        msg.setTextSize(16);
        msg.setPadding(20, 40, 20, 40);
        eventsContainer.addView(msg);
    }
}
