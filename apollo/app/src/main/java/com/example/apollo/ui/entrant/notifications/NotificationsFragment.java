package com.example.apollo.ui.entrant.notifications;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.apollo.databinding.FragmentNotificationsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * NotificationsFragment.java
 *
 * Entrant-facing notifications screen. Displays a list of notifications
 * (e.g., lottery win invites) for the current user, and lets them accept
 * or decline directly from the list.
 *
 * Responsibilities:
 * - Subscribe to Firestore user notifications collection
 * - Feed updates into {@link NotificationsAdapter}
 * - Handle accept/decline actions and update Firestore state
 */
public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration reg;
    private NotificationsAdapter adapter;

    /**
     * Inflates the notifications layout, initializes Firestore/auth,
     * sets up the RecyclerView + adapter, and wires the action callbacks
     * for Accept / Decline on each notification.
     *
     * @param inflater  LayoutInflater used to inflate the UI.
     * @param container Parent container for the fragment (may be null).
     * @param savedInstanceState Previously saved state (not used here).
     * @return The root view bound via ViewBinding.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Listener hooks for Accept / Decline actions
        adapter = new NotificationsAdapter(new NotificationsAdapter.OnNotificationAction() {

            @Override
            public void onAccept(NotificationsViewModel n, int position) {
                acceptInvite(n.eventId, n.id);
            }

            @Override
            public void onDecline(NotificationsViewModel n, int position) {
                declineInvite(n.eventId, n.id);
            }
        });

        binding.recycler.setAdapter(adapter);
        binding.empty.setVisibility(View.VISIBLE);

        return binding.getRoot();
    }

    /**
     * Handles the "Accept" action for a notification. Marks the user
     * as accepted/registered for the event and updates the notification
     * document's status to "accepted".
     *
     * @param eventId        ID of the event related to the notification.
     * @param notificationId ID of the notification document.
     */
    private void acceptInvite(String eventId, String notificationId) {
        String uid = auth.getCurrentUser().getUid();

        DocumentReference notifRef = db.collection("users")
                .document(uid)
                .collection("notifications")
                .document(notificationId);

        DocumentReference acceptedRef = db.collection("events")
                .document(eventId)
                .collection("registration")
                .document(uid);

        HashMap<String, Object> acceptedData = new HashMap<>();
        acceptedData.put("acceptedAt", FieldValue.serverTimestamp());
        acceptedData.put("userId", uid);

        acceptedRef.set(acceptedData).addOnSuccessListener(ok -> {

            // update notification status
            notifRef.update("status", "accepted");

            Toast.makeText(getContext(), "You are now registered!", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Handles the "Decline" action for a notification. Writes a declined
     * entry under the event and updates the notification status to "declined".
     *
     * @param eventId        ID of the event related to the notification.
     * @param notificationId ID of the notification document.
     */
    private void declineInvite(String eventId, String notificationId) {
        String uid = auth.getCurrentUser().getUid();

        DocumentReference notifRef = db.collection("users")
                .document(uid)
                .collection("notifications")
                .document(notificationId);

        DocumentReference declinedRef = db.collection("events")
                .document(eventId)
                .collection("declined")
                .document(uid);

        HashMap<String, Object> declinedData = new HashMap<>();
        declinedData.put("declinedAt", FieldValue.serverTimestamp());
        declinedData.put("userId", uid);

        declinedRef.set(declinedData).addOnSuccessListener(ok -> {

            notifRef.update("status", "declined");

            Toast.makeText(getContext(), "Invitation declined.", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Starts listening to the current user's notifications collection.
     * Whenever the Firestore snapshot changes, it rebuilds the list of
     * {@link NotificationsViewModel} and updates the adapter + empty state.
     */
    @Override
    public void onStart() {
        super.onStart();

        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        reg = db.collection("users")
                .document(uid)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    List<NotificationsViewModel> fresh = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        fresh.add(NotificationsViewModel.from(d));
                    }

                    adapter.setData(fresh);
                    binding.empty.setVisibility(fresh.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    /**
     * Removes the Firestore listener when the fragment is no longer visible
     * to avoid leaking the snapshot subscription.
     */
    @Override
    public void onStop() {
        if (reg != null) reg.remove();
        super.onStop();
    }

    /**
     * Clears the ViewBinding reference when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}