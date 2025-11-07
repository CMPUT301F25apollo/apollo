package com.example.apollo.ui.organizer.events;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class EventRepo {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String uid;

    public EventRepo() {
        this.uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) throw new IllegalStateException("User must be signed in");
    }

    // ENTRANT: join waitlist
    public Task<Void> joinWaitlist(@NonNull String eventId) {
        WriteBatch b = db.batch();
        DocumentReference wl = db.collection("events").document(eventId)
                .collection("waitlist").document(uid);

        Map<String,Object> data = new HashMap<>();
        data.put("joinedAt", FieldValue.serverTimestamp());
        b.set(wl, data);

        // optional counters
        DocumentReference ev = db.collection("events").document(eventId);
        Map<String,Object> inc = new HashMap<>();
        inc.put("waitlistCount", FieldValue.increment(1));
        b.set(ev, inc, com.google.firebase.firestore.SetOptions.merge());

        return b.commit();
    }

    // ENTRANT: leave waitlist
    public Task<Void> leaveWaitlist(@NonNull String eventId) {
        WriteBatch b = db.batch();
        DocumentReference wl = db.collection("events").document(eventId)
                .collection("waitlist").document(uid);
        b.delete(wl);

        DocumentReference ev = db.collection("events").document(eventId);
        Map<String,Object> dec = new HashMap<>();
        dec.put("waitlistCount", FieldValue.increment(-1));
        b.set(ev, dec, com.google.firebase.firestore.SetOptions.merge());

        return b.commit();
    }

    // ENTRANT: accept invite ⇒ registrations, remove invite
    public Task<Void> acceptInvite(@NonNull String eventId) {
        WriteBatch b = db.batch();
        DocumentReference invite = db.collection("events").document(eventId)
                .collection("invites").document(uid);
        DocumentReference reg = db.collection("events").document(eventId)
                .collection("registrations").document(uid);

        Map<String,Object> r = new HashMap<>();
        r.put("registeredAt", FieldValue.serverTimestamp());
        b.set(reg, r);
        b.delete(invite);

        DocumentReference ev = db.collection("events").document(eventId);
        Map<String,Object> delta = new HashMap<>();
        delta.put("invitedCount", FieldValue.increment(-1));
        delta.put("registeredCount", FieldValue.increment(1));
        b.set(ev, delta, com.google.firebase.firestore.SetOptions.merge());

        return b.commit();
    }

    // ENTRANT: decline invite ⇒ cancellations, remove invite
    public Task<Void> declineInvite(@NonNull String eventId, String reason) {
        WriteBatch b = db.batch();
        DocumentReference invite = db.collection("events").document(eventId)
                .collection("invites").document(uid);
        DocumentReference cancel = db.collection("events").document(eventId)
                .collection("cancellations").document(uid);

        Map<String,Object> c = new HashMap<>();
        c.put("reason", reason == null ? "declined" : reason);
        c.put("cancelledAt", FieldValue.serverTimestamp());
        b.set(cancel, c);
        b.delete(invite);

        DocumentReference ev = db.collection("events").document(eventId);
        Map<String,Object> delta = new HashMap<>();
        delta.put("invitedCount", FieldValue.increment(-1));
        delta.put("cancelledCount", FieldValue.increment(1));
        b.set(ev, delta, com.google.firebase.firestore.SetOptions.merge());

        return b.commit();
    }

    // ORGANIZER: cancel a registered entrant (optional admin flow)
    public Task<Void> organizerCancel(@NonNull String eventId, @NonNull String targetUid, String reason) {
        WriteBatch b = db.batch();
        DocumentReference reg = db.collection("events").document(eventId)
                .collection("registrations").document(targetUid);
        DocumentReference cancel = db.collection("events").document(eventId)
                .collection("cancellations").document(targetUid);

        Map<String,Object> c = new HashMap<>();
        c.put("reason", reason == null ? "organizer_cancelled" : reason);
        c.put("cancelledAt", FieldValue.serverTimestamp());
        b.set(cancel, c);
        b.delete(reg);

        DocumentReference ev = db.collection("events").document(eventId);
        Map<String,Object> delta = new HashMap<>();
        delta.put("registeredCount", FieldValue.increment(-1));
        delta.put("cancelledCount", FieldValue.increment(1));
        b.set(ev, delta, com.google.firebase.firestore.SetOptions.merge());

        return b.commit();
    }
}
