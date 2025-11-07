package com.example.apollo.ui.notifications;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

public class NotificationsViewModel {
    public String id;
    public String type;
    public String title;
    public String message;
    public String eventId;
    public boolean read;
    public Timestamp createdAt;

    public static NotificationsViewModel from(DocumentSnapshot d) {
        NotificationsViewModel n = new NotificationsViewModel();
        n.id       = d.getId();
        n.type     = d.getString("type");
        n.title    = d.getString("title");
        n.message  = d.getString("message");
        n.eventId  = d.getString("eventId");

        Boolean r  = d.getBoolean("read");
        n.read     = (r != null && r);

        n.createdAt = d.getTimestamp("createdAt");
        return n;
    }
}
