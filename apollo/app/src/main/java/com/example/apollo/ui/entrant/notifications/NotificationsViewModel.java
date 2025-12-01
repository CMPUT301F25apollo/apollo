package com.example.apollo.ui.entrant.notifications;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * NotificationsViewModel.java
 *
 * A simple data model representing one notification stored in Firestore.
 * This class holds all fields needed for displaying notifications inside
 * the RecyclerView, and provides a helper method to construct itself
 * from a Firestore DocumentSnapshot.
 *
 * Key fields:
 * - type: identifies the notification category (lottery win, system, etc.)
 * - title/message: content shown to the user
 * - eventId: event this notification belongs to (if any)
 * - status: response state ("accepted", "declined", or null)
 * - createdAt: timestamp for sorting
 */
public class NotificationsViewModel {

    public String id;
    public String type;
    public String title;
    public String message;
    public String eventId;
    public boolean read;
    public Timestamp createdAt;
    public String status;

    /**
     * Creates a NotificationsViewModel object from a Firestore document.
     *
     * @param d Firestore snapshot containing notification data.
     * @return A fully populated {@link NotificationsViewModel}.
     */
    public static NotificationsViewModel from(DocumentSnapshot d) {
        NotificationsViewModel n = new NotificationsViewModel();

        n.id       = d.getId();
        n.type     = d.getString("type");
        n.title    = d.getString("title");
        n.message  = d.getString("message");
        n.eventId  = d.getString("eventId");
        n.status   = d.getString("status");

        Boolean r = d.getBoolean("read");
        n.read = (r != null && r);

        n.createdAt = d.getTimestamp("createdAt");

        return n;
    }
}
