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

/**
 * EventRepo.java
 *
 * Purpose:
 * Handles Firestore operations related to event participation.
 * Provides methods for entrants and organizers to join or leave waitlists,
 * accept or decline invitations, and cancel registrations.
 *
 * Design Pattern:
 * Acts as a Repository class that manages data transactions between
 * the app and Firestore, separating business logic from UI code.
 *
 * Notes:
 * - All operations use Firestore WriteBatch for atomic updates.
 * - Requires a logged-in user (FirebaseAuth) for most operations.
 */
public class EventRepo {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String uid;

    /**
     * Constructor for EventRepo.
     * Initializes the current user's ID using Firebase Authentication.
     * Throws an exception if no user is signed in.
     */
    public EventRepo() {
        this.uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid == null) throw new IllegalStateException("User must be signed in");
    }

    /**
     * Adds the current user to an event's waitlist and updates the event's waitlist count.
     *
     * @param eventId The ID of the event to join.
     * @return A Task representing the completion of the Firestore batch operation.
     */
    public Task<Void> joinWaitlist(@NonNull String eventId) {
        WriteBatch b = db.batch();
        DocumentReference wl = db.collection("events").document(eventId)
                .collection("waitlist").document(uid);

        Map<String, Object> data = new HashMap<>();
        data.put("joinedAt", FieldValue.serverTimestamp());
        b.set(wl, data);

        // Increment waitlist count
        DocumentReference ev = db.collection("events").document(eventId);
        Map<String, Object> inc = new HashMap<>();
        inc.put("waitlistCount", FieldValue.increment(1));
        b.set(ev, inc, com.google.firebase.firestore.SetOptions.merge());

        return b.commit();
    }

    /**
     * Removes the current user from an event's waitlist and decreases the waitlist count.
     *
     * @param eventId The ID of the event to leave.
     * @return A Task representing the completion of the Firestore batch operation.
     */
    public Task<Void> leaveWaitlist(@NonNull String eventId) {
        WriteBatch b = db.batch();
        DocumentReference wl = db.collection("events").document(eventId)
                .collection("waitlist").document(uid);
        b.delete(wl);

        // Decrement waitlist count
        DocumentReference ev = db.collection("events").document(eventId);
        Map<String, Object> dec = new HashMap<>();
        dec.put("waitlistCount", FieldValue.increment(-1));
        b.set(ev, dec, com.google.firebase.firestore.SetOptions.merge());

        return b.commit();
    }

    /**
     * Accepts an invitation to an event, moves the user to the registrations collection,
     * and updates event counters.
     *
     * @param eventId The ID of the event whose invite is being accepted.
     * @return A Task representing the completion of the Firestore batch operation.
     */
    public Task<Void> acceptInvite(@NonNull String eventId) {
        WriteBatch b = db.batch();
        DocumentReference invite = db.collection("events").document(eventId)
                .collection("invites").document(uid);
        DocumentReference reg = db.collection("events").document(eventId)
                .collection("registrations").document(uid);

        Map<String, Object> r = new HashMap<>();
        r.put("registeredAt", FieldValue.serverTimestamp());
        b.set(reg, r);
        b.delete(invite);

        // Update event counts
        DocumentReference ev = db.collection("events").document(eventId);
        Map<String, Object> delta = new HashMap<>();
        delta.put("invitedCount", FieldValue.increment(-1));
        delta.put("registeredCount", FieldValue.increment(1));
        b.set(ev, delta, com.google.firebase.firestore.SetOptions.merge());

        return b.commit();
    }

    /**
     * Declines an invitation to an event, adds a record in cancellations,
     * and updates event counters.
     *
     * @param eventId The ID of the event to decline.
     * @param reason Optional reason for declining (defaults to "declined").
     * @return A Task representing the completion of the Firestore batch operation.
     */
    public Task<Void> declineInvite(@NonNull String eventId, String reason) {
        WriteBatch b = db.batch();
        DocumentReference invite = db.collection("events").document(eventId)
                .collection("invites").document(uid);
        DocumentReference cancel = db.collection("events").document(eventId)
                .collection("cancellations").document(uid);

        Map<String, Object> c = new HashMap<>();
        c.put("reason", reason == null ? "declined" : reason);
        c.put("cancelledAt", FieldValue.serverTimestamp());
        b.set(cancel, c);
        b.delete(invite);

        // Update event counts
        DocumentReference ev = db.collection("events").document(eventId);
        Map<String, Object> delta = new HashMap<>();
        delta.put("invitedCount", FieldValue.increment(-1));
        delta.put("cancelledCount", FieldValue.increment(1));
        b.set(ev, delta, com.google.firebase.firestore.SetOptions.merge());

        return b.commit();
    }

    /**
     * Cancels a registered entrant's participation (used by organizers).
     * Moves the entrant’s record to the cancellations collection and updates event counts.
     *
     * @param eventId The ID of the event.
     * @param targetUid The UID of the user being cancelled.
     * @param reason Optional reason for cancellation (defaults to "organizer_cancelled").
     * @return A Task representing the completion of the Firestore batch operation.
     */
    public Task<Void> organizerCancel(@NonNull String eventId, @NonNull String targetUid, String reason) {
        WriteBatch b = db.batch();
        DocumentReference reg = db.collection("events").document(eventId)
                .collection("registrations").document(targetUid);
        DocumentReference cancel = db.collection("events").document(eventId)
                .collection("cancellations").document(targetUid);

        Map<String, Object> c = new HashMap<>();
        c.put("reason", reason == null ? "organizer_cancelled" : reason);
        c.put("cancelledAt", FieldValue.serverTimestamp());
        b.set(cancel, c);
        b.delete(reg);

        // Update event counts
        DocumentReference ev = db.collection("events").document(eventId);
        Map<String, Object> delta = new HashMap<>();
        delta.put("registeredCount", FieldValue.increment(-1));
        delta.put("cancelledCount", FieldValue.increment(1));
        b.set(ev, delta, com.google.firebase.firestore.SetOptions.merge());

        return b.commit();
    }
}
