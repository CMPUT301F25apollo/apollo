package com.example.apollo.ui.entrant.notifications;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * NotificationsViewModel.java
 *
 * Purpose:
 * Represents a single notification item that is stored in Firestore.
 * Contains all the data needed to display a notification in the UI.
 *
 * Design Pattern:
 * Acts as a simple data model (part of MVC) that holds information
 * retrieved from Firestore and used by the NotificationsAdapter.
 *
 * Notes:
 * - Each notification includes a title, message, timestamp, and read status.
 * - Can be extended later to include other fields such as sender or action links.
 */
public class NotificationsViewModel {
    public String id;
    public String type;
    public String title;
    public String message;
    public String eventId;
    public boolean read;
    public Timestamp createdAt;
    public String status; // "accepted", "declined", or null

    /**n
     * Converts a Firestore document into a NotificationsViewModel object.
     *
     * @param d The Firestore DocumentSnapshot containing notification data.
     * @return A NotificationsViewModel object with data from the document.
     */
    public static NotificationsViewModel from(DocumentSnapshot d) {
        NotificationsViewModel n = new NotificationsViewModel();
        n.id       = d.getId();
        n.type     = d.getString("type");
        n.title    = d.getString("title");
        n.message  = d.getString("message");
        n.eventId  = d.getString("eventId");
        n.status = d.getString("status");

        Boolean r  = d.getBoolean("read");
        n.read     = (r != null && r);

        n.createdAt = d.getTimestamp("createdAt");
        return n;
    }
}